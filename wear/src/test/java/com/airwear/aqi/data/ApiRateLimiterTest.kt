package com.airwear.aqi.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApiRateLimiterTest {

    private class MemoryStore(var day: String = "", var count: Int = 0) : DailyCounterStore {
        override suspend fun loadDailyCount() = day to count
        override suspend fun saveDailyCount(day: String, count: Int) {
            this.day = day
            this.count = count
        }
    }

    private var now = 1_800_000_000_000L
    private val slept = mutableListOf<Long>()

    private fun limiter(store: DailyCounterStore = MemoryStore(), perDay: Int = 500) =
        ApiRateLimiter(
            store = store,
            perMinute = 5,
            perDay = perDay,
            clock = { now },
            sleep = { ms -> slept += ms; now += ms },
        )

    @Test
    fun allowsFiveRequestsPerMinute_thenRejectsWithoutWaiting() = runTest {
        val l = limiter()
        repeat(5) { l.acquire() }
        try {
            l.acquire(maxWaitMs = 0)
            fail("6th request in the same minute must be rejected")
        } catch (e: RateLimitedException) {
            assertEquals(60_000L, e.retryInMs)
        }
    }

    @Test
    fun waitsForWindowWhenAllowed() = runTest {
        val l = limiter()
        repeat(5) { now += 1_000; l.acquire() }
        l.acquire(maxWaitMs = 65_000)
        assertEquals(listOf(56_000L), slept)
    }

    @Test
    fun windowSlides() = runTest {
        val l = limiter()
        repeat(5) { l.acquire() }
        now += 60_000
        l.acquire() // oldest entries expired
    }

    @Test
    fun serverRateLimitBlocksWholeWindow() = runTest {
        val l = limiter()
        l.onServerRateLimited()
        try {
            l.acquire()
            fail()
        } catch (e: RateLimitedException) {
            assertEquals(60_000L, e.retryInMs)
        }
    }

    @Test
    fun dailyQuotaPersistsAndResetsNextDay() = runTest {
        val today = ApiRateLimiter.dayKey(now)
        val store = MemoryStore(day = today, count = 499)
        val l = limiter(store)
        l.acquire()
        assertEquals(500, store.count)
        now += 60_000
        try {
            l.acquire()
            fail("quota spent")
        } catch (_: DailyQuotaExceededException) {
        }
        now += 24 * 60 * 60_000L
        l.acquire()
        assertEquals(1, store.count)
        assertTrue(store.day != today)
    }
}

class CachePolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun freshWithinTtl() {
        assertTrue(CachePolicy.isFresh(now - 60_000, now))
    }

    @Test
    fun staleAfterTtl() {
        assertTrue(!CachePolicy.isFresh(now - CachePolicy.FRESH_TTL_MS, now))
    }

    @Test
    fun invalidatedOrUnknownIsNeverFresh() {
        assertTrue(!CachePolicy.isFresh(0L, now))
    }

    @Test
    fun clockSkewIntoFutureIsNotFresh() {
        assertTrue(!CachePolicy.isFresh(now + 60_000, now))
    }
}
