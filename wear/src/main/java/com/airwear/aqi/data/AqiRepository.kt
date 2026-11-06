package com.airwear.aqi.data

import android.content.Context
import android.util.Log
import com.airwear.aqi.BuildConfig
import com.airwear.aqi.complication.ComplicationUpdater
import com.airwear.aqi.data.iqair.AirVisualApi
import com.airwear.aqi.data.iqair.toSnapshot
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.tile.AqiTileService
import com.airwear.aqi.work.RefreshScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class CityRankResult(val entries: List<CityRankEntry>, val updatedAtMs: Long)

class AqiRepository(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val dataStore = AqiDataStore(context)
    private val locationProvider = LocationProvider(context)
    private val limiter = ApiRateLimiter(store = dataStore, clock = clock)

    private val api: AirVisualApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply { if (BuildConfig.DEBUG) addInterceptor(requestLogger) }
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        Retrofit.Builder()
            .baseUrl(BuildConfig.IQAIR_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AirVisualApi::class.java)
    }

    val cachedSnapshot: Flow<AqiSnapshot?> = dataStore.snapshotFlow
    val locationMode: Flow<String> = dataStore.locationModeFlow
    val refreshMinutes: Flow<Int> = dataStore.refreshMinutesFlow
    val manualCity: Flow<AqiDataStore.ManualCity?> = dataStore.manualCityFlow

    /**
     * Fetch AQI for the active location mode:
     * MANUAL → fixed city · GPS → nearest_city(lat,lon) · IP → nearest_city.
     *
     * Returns the cached reading without a request when it was downloaded
     * less than [CachePolicy.FRESH_TTL_MS] ago (unless [force]).
     * On failure returns the error — the cache is untouched and still
     * observable through [cachedSnapshot], so callers decide how to show it.
     */
    suspend fun refresh(force: Boolean = false): Result<AqiSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val cached = dataStore.snapshotFlow.first()
            if (!force && cached != null && CachePolicy.isFresh(cached.sourceFetchedAtMs, clock())) {
                return@runCatching cached
            }

            val key = BuildConfig.IQAIR_API_KEY
            val response = when (dataStore.locationModeFlow.first()) {
                "MANUAL" -> {
                    val manual = dataStore.manualCityFlow.first()
                        ?: error("Choose a city in Settings")
                    networkCall {
                        api.city(
                            city = manual.city,
                            state = manual.state,
                            country = manual.country,
                            key = key,
                        )
                    }
                }

                "GPS" -> {
                    // Background callers (tile, worker) get no fix without
                    // background-location permission — reuse the last
                    // foreground fix instead of silently falling back to IP.
                    val fresh = if (locationProvider.hasLocationPermission()) {
                        locationProvider.currentLocation()
                    } else {
                        null
                    }
                    if (fresh != null) dataStore.saveDeviceLocation(fresh, clock())
                    val coords = fresh ?: dataStore.lastDeviceLocation()
                        ?.takeIf { (_, at) -> clock() - at < LAST_FIX_MAX_AGE_MS }
                        ?.first
                    networkCall {
                        if (coords != null) {
                            api.nearestCity(lat = coords.lat, lon = coords.lon, key = key)
                        } else {
                            api.nearestCity(key = key)
                        }
                    }
                }

                else -> networkCall { api.nearestCity(key = key) }
            }

            if (response.status != "success") error("IQAir: ${response.status ?: "no status"}")

            val snapshot = response.toSnapshot(fetchedAtMs = clock())
            if (snapshot.aqiUs < 0) error("Empty AQI payload")
            dataStore.saveSnapshot(snapshot)
            ComplicationUpdater.requestUpdateAll(context)
            AqiTileService.requestUpdate(context)
            snapshot
        }.recoverCatching { e -> throw friendlyError(e) }
    }

    /** Every IQAir request goes through the shared limiter. */
    private suspend fun <T> networkCall(maxWaitMs: Long = 0L, block: suspend () -> T): T {
        limiter.acquire(maxWaitMs)
        try {
            return block()
        } catch (e: HttpException) {
            if (e.code() == 429) limiter.onServerRateLimited()
            throw e
        }
    }

    suspend fun setLocationMode(mode: String) {
        dataStore.setLocationMode(mode)
        dataStore.invalidateFreshness()
    }

    suspend fun setManualCity(city: String, state: String, country: String) {
        dataStore.setManualCity(city, state, country)
        dataStore.invalidateFreshness()
    }

    suspend fun setRefreshMinutes(minutes: Int) {
        dataStore.setRefreshMinutes(minutes)
        RefreshScheduler.schedule(context, minutes)
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    val watchCityCount: Int get() = WatchCities.list.size

    /**
     * Multi-city ranking. Free IQAir has no global leaderboard — we poll a fixed
     * major-city list (rate limited) and sort by US AQI.
     * [onProgress] is invoked after each city so the UI can fill in gradually.
     */
    suspend fun loadCityRank(
        force: Boolean = false,
        onProgress: (CityRankResult) -> Unit = {},
    ): Result<CityRankResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (cachedJson, cachedAt) = dataStore.loadCityRankJson()
            val results = linkedMapOf<String, CityRankEntry>()

            // Seed from cache — only cities still on the watch list.
            if (!cachedJson.isNullOrBlank()) {
                RankList.keepWatched(parseCityRank(cachedJson), WatchCities.names)
                    .forEach { results[it.city] = it }
                if (results.isNotEmpty()) {
                    onProgress(CityRankResult(RankList.sorted(results.values), cachedAt))
                }
            }

            val cacheComplete = results.keys.containsAll(WatchCities.names)
            if (!force && cacheComplete && clock() - cachedAt in 0 until RANK_TTL_MS) {
                return@runCatching CityRankResult(RankList.sorted(results.values), cachedAt)
            }

            val key = BuildConfig.IQAIR_API_KEY
            var lastSavedAt = cachedAt
            for ((index, spec) in WatchCities.list.withIndex()) {
                // Leave headroom in the 5/min window for the user's own refresh.
                if (index > 0) delay(RANK_SPACING_MS)
                try {
                    val resp = networkCall(maxWaitMs = 65_000L) {
                        api.city(city = spec.city, state = spec.state, country = spec.country, key = key)
                    }
                    val snap = resp.toSnapshot(fetchedAtMs = clock())
                    if (snap.aqiUs >= 0) {
                        results[spec.city] = CityRankEntry(
                            city = spec.city,
                            state = spec.state,
                            country = spec.country,
                            aqiUs = snap.aqiUs,
                            category = snap.category,
                            updatedAtEpochMs = snap.updatedAtEpochMs,
                        )
                    }
                } catch (e: DailyQuotaExceededException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Rank fetch ${spec.city} failed: ${e.message}")
                }
                val sorted = RankList.sorted(results.values)
                if (sorted.isNotEmpty()) {
                    lastSavedAt = clock()
                    dataStore.saveCityRank(serializeCityRank(sorted), lastSavedAt)
                    onProgress(CityRankResult(sorted, lastSavedAt))
                }
            }

            CityRankResult(RankList.sorted(results.values), lastSavedAt)
        }.recoverCatching { e -> throw friendlyError(e) }
    }

    private fun serializeCityRank(list: List<CityRankEntry>): String {
        val arr = org.json.JSONArray()
        list.forEach { e ->
            arr.put(
                org.json.JSONObject().apply {
                    put("c", e.city)
                    put("s", e.state ?: "")
                    put("k", e.country)
                    put("a", e.aqiUs)
                    put("t", e.updatedAtEpochMs)
                }
            )
        }
        return arr.toString()
    }

    private fun parseCityRank(raw: String): List<CityRankEntry> {
        return runCatching {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val aqi = o.getInt("a")
                    add(
                        CityRankEntry(
                            city = o.getString("c"),
                            state = o.optString("s").ifBlank { null },
                            country = o.getString("k"),
                            aqiUs = aqi,
                            category = AqiCategory.fromAqi(aqi),
                            updatedAtEpochMs = o.optLong("t"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Fixed watch list — only 5 cities so Top AQI loads in ~1 min (5 req/min). */
    private object WatchCities {
        data class Spec(val city: String, val state: String, val country: String)

        val list = listOf(
            Spec("Jakarta", "Jakarta", "Indonesia"),
            Spec("Delhi", "Delhi", "India"),
            Spec("Beijing", "Beijing", "China"),
            Spec("London", "England", "United Kingdom"),
            Spec("Tokyo", "Tokyo", "Japan"),
        )
        val names: Set<String> = list.map { it.city }.toSet()
    }

    private companion object {
        const val TAG = "AirWear"
        const val RANK_TTL_MS = 15 * 60_000L
        /** How long a stored foreground GPS fix is trusted for background refreshes. */
        const val LAST_FIX_MAX_AGE_MS = 24 * 60 * 60_000L
        const val RANK_SPACING_MS = 14_000L

        /** Debug-only: logs path + status, never the query string (it holds the API key). */
        val requestLogger = Interceptor { chain ->
            val req = chain.request()
            val start = System.nanoTime()
            val res = chain.proceed(req)
            val ms = (System.nanoTime() - start) / 1_000_000
            Log.d(TAG, "IQAir ${req.method} ${req.url.encodedPath} -> ${res.code} (${ms}ms)")
            res
        }
    }
}

/** Pure helpers for the ranking list (unit tested). */
object RankList {
    fun keepWatched(entries: List<CityRankEntry>, watched: Set<String>): List<CityRankEntry> =
        entries.filter { it.city in watched }

    fun sorted(entries: Collection<CityRankEntry>): List<CityRankEntry> =
        entries.sortedByDescending { it.aqiUs }
}

/** Maps transport / API failures to short, user-facing messages. */
internal fun friendlyError(e: Throwable): Throwable = when (e) {
    is RateLimitedException, is DailyQuotaExceededException -> e
    is HttpException -> Exception(
        when (e.code()) {
            429 -> "Rate limited · try again soon"
            401, 403 -> "Invalid API key"
            400, 404 -> "Location not found"
            in 500..599 -> "IQAir is unavailable"
            else -> "Couldn't load AQI (${e.code()})"
        },
        e,
    )
    is SocketTimeoutException -> Exception("Network timeout", e)
    is UnknownHostException -> Exception("No internet connection", e)
    is IOException -> Exception("Network error", e)
    is IllegalStateException -> Exception(e.message ?: "Couldn't load AQI", e)
    else -> Exception("Couldn't load AQI", e)
}
