package com.airwear.aqi.data

import android.content.Context
import com.airwear.aqi.BuildConfig
import com.airwear.aqi.data.iqair.AirVisualApi
import com.airwear.aqi.data.iqair.toSnapshot
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.domain.AqiSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AqiRepository(private val context: Context) {

    private val dataStore = AqiDataStore(context)
    private val locationProvider = LocationProvider(context)

    private val api: AirVisualApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.IQAIR_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AirVisualApi::class.java)
    }

    val cachedSnapshot: Flow<AqiSnapshot?> = dataStore.snapshotFlow
    val locationMode: Flow<String> = dataStore.locationModeFlow
    val refreshMinutes: Flow<Int> = dataStore.refreshMinutesFlow
    val manualCity: Flow<AqiDataStore.ManualCity?> = dataStore.manualCityFlow

    /**
     * Fetch AQI for the active location mode:
     * MANUAL → fixed city · GPS → nearest_city(lat,lon) · IP → nearest_city
     */
    suspend fun refresh(forceGps: Boolean = false): Result<AqiSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val mode = dataStore.locationModeFlow.first()
            // Don't serve stale cache when switching to a manual city (user expects new place)
            if (mode != "MANUAL") {
                val cached = dataStore.snapshotFlow.first()
                val freshMs = 5 * 60_000L
                if (cached != null && System.currentTimeMillis() - cached.updatedAtEpochMs < freshMs && !forceGps) {
                    return@runCatching cached
                }
            }

            val key = BuildConfig.IQAIR_API_KEY
            val response = when {
                mode == "MANUAL" -> {
                    val manual = dataStore.manualCityFlow.first()
                        ?: error("No manual city selected")
                    api.city(
                        city = manual.city,
                        state = manual.state,
                        country = manual.country,
                        key = key,
                    )
                }
                else -> {
                    val useGps = forceGps || mode == "GPS"
                    val coords = if (useGps && locationProvider.hasLocationPermission()) {
                        locationProvider.currentLocation()
                    } else {
                        null
                    }
                    if (coords != null) {
                        api.nearestCity(lat = coords.lat, lon = coords.lon, key = key)
                    } else {
                        api.nearestCity(key = key)
                    }
                }
            }

            if (response.status != "success") {
                error(friendlyApiError(response.status))
            }

            val snapshot = response.toSnapshot()
            if (snapshot.aqiUs < 0) error("Empty AQI payload")
            dataStore.saveSnapshot(snapshot)
            snapshot
        }.recoverCatching { e ->
            // Prefer cached snapshot over failing the whole Home UI (e.g. HTTP 429)
            val cached = dataStore.snapshotFlow.first()
            if (cached != null) {
                cached
            } else {
                throw Exception(friendlyApiError(e.message), e)
            }
        }
    }

    private fun friendlyApiError(raw: String?): String {
        val msg = raw ?: return "Couldn't load AQI"
        return when {
            msg.contains("429", ignoreCase = true) -> "Rate limited · try again soon"
            msg.contains("401", ignoreCase = true) -> "Invalid API key"
            msg.contains("timeout", ignoreCase = true) -> "Network timeout"
            else -> msg
        }
    }

    suspend fun setLocationMode(mode: String) = dataStore.setLocationMode(mode)

    suspend fun setManualCity(city: String, state: String, country: String) =
        dataStore.setManualCity(city, state, country)

    suspend fun setRefreshMinutes(minutes: Int) = dataStore.setRefreshMinutes(minutes)

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    /**
     * Multi-city ranking. Free IQAir has no global leaderboard — we poll a fixed
     * major-city list (5 req/min → ~12s spacing) and sort by US AQI.
     * [onProgress] is invoked after each city so the UI can fill in gradually.
     */
    suspend fun loadCityRank(
        force: Boolean = false,
        onProgress: (List<CityRankEntry>) -> Unit = {},
    ): Result<List<CityRankEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (cachedJson, _) = dataStore.loadCityRankJson()
                val results = linkedMapOf<String, CityRankEntry>()

                // Seed from cache immediately (even a partial list)
                if (!force && !cachedJson.isNullOrBlank()) {
                    parseCityRank(cachedJson).forEach { results[it.city] = it }
                    if (results.isNotEmpty()) {
                        onProgress(results.values.sortedByDescending { it.aqiUs })
                    }
                }

                // Only skip network when we already have the full watch list and cache is recent
                val cachedAt = dataStore.loadCityRankJson().second
                val freshMs = 15 * 60_000L
                if (!force &&
                    results.size >= WatchCities.list.size &&
                    System.currentTimeMillis() - cachedAt < freshMs
                ) {
                    return@runCatching results.values.sortedByDescending { it.aqiUs }
                }

                val key = BuildConfig.IQAIR_API_KEY
                // Stay under Community plan 5/min with margin
                val delayMs = 14_000L
                WatchCities.list.forEachIndexed { index, spec ->
                    if (index > 0) {
                        kotlinx.coroutines.delay(delayMs)
                    }
                    // Skip cities we already have from a complete fresh cache
                    if (!force && results.containsKey(spec.city) && results.size >= WatchCities.list.size) {
                        return@forEachIndexed
                    }
                    runCatching {
                        val resp = api.city(
                            city = spec.city,
                            state = spec.state,
                            country = spec.country,
                            key = key,
                        )
                        val snap = resp.toSnapshot()
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
                    }
                    val snapshotList = results.values.sortedByDescending { it.aqiUs }
                    if (snapshotList.isNotEmpty()) {
                        dataStore.saveCityRank(serializeCityRank(snapshotList))
                        onProgress(snapshotList)
                    }
                }

                val sorted = results.values.sortedByDescending { it.aqiUs }
                if (sorted.isNotEmpty()) {
                    dataStore.saveCityRank(serializeCityRank(sorted))
                }
                sorted
            }
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
    }
}
