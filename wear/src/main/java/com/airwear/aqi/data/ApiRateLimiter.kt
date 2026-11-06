package com.airwear.aqi.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneOffset

/** Persists the per-day request counter so it survives process death. */
interface DailyCounterStore {
    suspend fun loadDailyCount(): Pair<String, Int>
    suspend fun saveDailyCount(day: String, count: Int)
}

class RateLimitedException(val retryInMs: Long) :
    Exception("Rate limited · try again in ${((retryInMs + 999) / 1000).coerceAtLeast(1)}s")

class DailyQuotaExceededException :
    Exception("Daily API limit reached · resumes tomorrow")

/**
 * Client-side guard for the IQAir Community plan (5 req/min, 500 req/day).
 * Every network call must go through [acquire] first. Shared by the app UI,
 * the refresh worker and the ranking loader (same process, same repository).
 */
class ApiRateLimiter(
    private val store: DailyCounterStore,
    private val perMinute: Int = 5,
    private val perDay: Int = 500,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private val recent = ArrayDeque<Long>()
    private var blockedUntil = 0L
    private var day: String? = null
    private var dayCount = 0

    /**
     * Reserve one request slot. Suspends up to [maxWaitMs] for the per-minute
     * window to free up; otherwise throws [RateLimitedException].
     * Throws [DailyQuotaExceededException] when the daily budget is spent.
     */
    suspend fun acquire(maxWaitMs: Long = 0L) {
        var waited = 0L
        while (true) {
            val waitMs = mutex.withLock { tryAcquireLocked() }
            if (waitMs == 0L) return
            if (waited + waitMs > maxWaitMs) throw RateLimitedException(waitMs)
            sleep(waitMs)
            waited += waitMs
        }
    }

    /** Server answered 429 — treat the whole minute window as used. */
    suspend fun onServerRateLimited() = mutex.withLock {
        blockedUntil = clock() + WINDOW_MS
    }

    /** Returns 0 when a slot was taken, else how long to wait. */
    private suspend fun tryAcquireLocked(): Long {
        val now = clock()
        if (now < blockedUntil) return blockedUntil - now

        while (recent.isNotEmpty() && now - recent.first() >= WINDOW_MS) recent.removeFirst()
        if (recent.size >= perMinute) return recent.first() + WINDOW_MS - now

        val today = dayKey(now)
        if (day == null) {
            val (storedDay, storedCount) = store.loadDailyCount()
            day = storedDay
            dayCount = storedCount
        }
        if (day != today) {
            day = today
            dayCount = 0
        }
        if (dayCount >= perDay) throw DailyQuotaExceededException()

        dayCount += 1
        recent.addLast(now)
        store.saveDailyCount(today, dayCount)
        return 0L
    }

    companion object {
        const val WINDOW_MS = 60_000L

        fun dayKey(epochMs: Long): String =
            Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.UTC).toLocalDate().toString()
    }
}

/** How long a downloaded reading is reused before hitting the network again. */
object CachePolicy {
    const val FRESH_TTL_MS = 10 * 60_000L

    fun isFresh(fetchedAtMs: Long, nowMs: Long): Boolean =
        fetchedAtMs > 0L && nowMs - fetchedAtMs in 0 until FRESH_TTL_MS
}
