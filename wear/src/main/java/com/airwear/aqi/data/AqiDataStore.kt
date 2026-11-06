package com.airwear.aqi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aqi_cache")

class AqiDataStore(private val context: Context) : DailyCounterStore {

    private object Keys {
        val AQI_US = intPreferencesKey("aqi_us")
        val AQI_CN = intPreferencesKey("aqi_cn")
        val MAIN_POL = stringPreferencesKey("main_pol")
        val CITY = stringPreferencesKey("city")
        val STATE = stringPreferencesKey("state")
        val COUNTRY = stringPreferencesKey("country")
        val LAT = floatPreferencesKey("lat")
        val LNG = floatPreferencesKey("lng")
        val TEMP = intPreferencesKey("temp")
        val HUMIDITY = intPreferencesKey("humidity")
        val WIND = intPreferencesKey("wind")
        val ICON = stringPreferencesKey("icon")
        val POLLUTANTS_JSON = stringPreferencesKey("pollutants_json")
        val UPDATED_AT = longPreferencesKey("updated_at")
        val FETCHED_AT = longPreferencesKey("fetched_at")
        val LOCATION_MODE = stringPreferencesKey("location_mode") // GPS | IP | MANUAL
        val MANUAL_CITY = stringPreferencesKey("manual_city")
        val MANUAL_STATE = stringPreferencesKey("manual_state")
        val MANUAL_COUNTRY = stringPreferencesKey("manual_country")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes")
        val CITY_RANK_JSON = stringPreferencesKey("city_rank_json")
        val CITY_RANK_AT = longPreferencesKey("city_rank_at")
        val QUOTA_DAY = stringPreferencesKey("quota_day")
        val QUOTA_COUNT = intPreferencesKey("quota_count")
        val DEVICE_LAT = stringPreferencesKey("device_lat")
        val DEVICE_LON = stringPreferencesKey("device_lon")
        val DEVICE_AT = longPreferencesKey("device_at")
    }

    val snapshotFlow: Flow<AqiSnapshot?> = context.dataStore.data.map { prefs ->
        val aqi = prefs[Keys.AQI_US] ?: return@map null
        if (aqi < 0) return@map null
        AqiSnapshot(
            aqiUs = aqi,
            aqiCn = prefs[Keys.AQI_CN],
            category = AqiCategory.fromAqi(aqi),
            mainPollutantUs = prefs[Keys.MAIN_POL],
            city = prefs[Keys.CITY],
            state = prefs[Keys.STATE],
            country = prefs[Keys.COUNTRY],
            lat = prefs[Keys.LAT]?.toDouble(),
            lng = prefs[Keys.LNG]?.toDouble(),
            tempC = prefs[Keys.TEMP],
            humidity = prefs[Keys.HUMIDITY],
            windMs = prefs[Keys.WIND],
            weatherIcon = prefs[Keys.ICON],
            pollutants = parsePollutants(prefs[Keys.POLLUTANTS_JSON]),
            updatedAtEpochMs = prefs[Keys.UPDATED_AT] ?: 0L,
            sourceFetchedAtMs = prefs[Keys.FETCHED_AT] ?: 0L,
        )
    }.distinctUntilChanged()

    val locationModeFlow: Flow<String> = context.dataStore.data
        .map { it[Keys.LOCATION_MODE] ?: "GPS" }
        .distinctUntilChanged()

    val refreshMinutesFlow: Flow<Int> = context.dataStore.data
        .map { it[Keys.REFRESH_MINUTES] ?: DEFAULT_REFRESH_MINUTES }
        .distinctUntilChanged()

    /**
     * Overwrites the whole snapshot. Fields missing from the new payload are
     * removed so values from a previous location never leak into this one.
     */
    suspend fun saveSnapshot(snapshot: AqiSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AQI_US] = snapshot.aqiUs
            prefs.putOrRemove(Keys.AQI_CN, snapshot.aqiCn)
            prefs.putOrRemove(Keys.MAIN_POL, snapshot.mainPollutantUs)
            prefs.putOrRemove(Keys.CITY, snapshot.city)
            prefs.putOrRemove(Keys.STATE, snapshot.state)
            prefs.putOrRemove(Keys.COUNTRY, snapshot.country)
            prefs.putOrRemove(Keys.LAT, snapshot.lat?.toFloat())
            prefs.putOrRemove(Keys.LNG, snapshot.lng?.toFloat())
            prefs.putOrRemove(Keys.TEMP, snapshot.tempC)
            prefs.putOrRemove(Keys.HUMIDITY, snapshot.humidity)
            prefs.putOrRemove(Keys.WIND, snapshot.windMs)
            prefs.putOrRemove(Keys.ICON, snapshot.weatherIcon)
            prefs[Keys.POLLUTANTS_JSON] = serializePollutants(snapshot.pollutants)
            prefs[Keys.UPDATED_AT] = snapshot.updatedAtEpochMs
            prefs[Keys.FETCHED_AT] = snapshot.sourceFetchedAtMs
        }
    }

    /** Keep showing the cached reading, but force the next refresh to hit the network. */
    suspend fun invalidateFreshness() {
        context.dataStore.edit { it.remove(Keys.FETCHED_AT) }
    }

    suspend fun setLocationMode(mode: String) {
        context.dataStore.edit { it[Keys.LOCATION_MODE] = mode }
    }

    data class ManualCity(val city: String, val state: String, val country: String)

    val manualCityFlow: Flow<ManualCity?> = context.dataStore.data.map { prefs ->
        val city = prefs[Keys.MANUAL_CITY] ?: return@map null
        ManualCity(
            city = city,
            state = prefs[Keys.MANUAL_STATE] ?: "",
            country = prefs[Keys.MANUAL_COUNTRY] ?: "",
        )
    }.distinctUntilChanged()

    suspend fun setManualCity(city: String, state: String, country: String) {
        context.dataStore.edit {
            it[Keys.MANUAL_CITY] = city
            it[Keys.MANUAL_STATE] = state
            it[Keys.MANUAL_COUNTRY] = country
        }
    }

    suspend fun setRefreshMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REFRESH_MINUTES] = minutes }
    }

    /** Compact JSON array cache for multi-city ranking. */
    suspend fun saveCityRank(json: String, atMs: Long) {
        context.dataStore.edit {
            it[Keys.CITY_RANK_JSON] = json
            it[Keys.CITY_RANK_AT] = atMs
        }
    }

    suspend fun loadCityRankJson(): Pair<String?, Long> {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.CITY_RANK_JSON] to (prefs[Keys.CITY_RANK_AT] ?: 0L)
    }

    override suspend fun loadDailyCount(): Pair<String, Int> {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.QUOTA_DAY] ?: "") to (prefs[Keys.QUOTA_COUNT] ?: 0)
    }

    override suspend fun saveDailyCount(day: String, count: Int) {
        context.dataStore.edit {
            it[Keys.QUOTA_DAY] = day
            it[Keys.QUOTA_COUNT] = count
        }
    }

    /** Last GPS fix taken while the app was in the foreground. */
    suspend fun saveDeviceLocation(coords: LatLon, atMs: Long) {
        context.dataStore.edit {
            it[Keys.DEVICE_LAT] = coords.lat.toString()
            it[Keys.DEVICE_LON] = coords.lon.toString()
            it[Keys.DEVICE_AT] = atMs
        }
    }

    suspend fun lastDeviceLocation(): Pair<LatLon, Long>? {
        val prefs = context.dataStore.data.first()
        val lat = prefs[Keys.DEVICE_LAT]?.toDoubleOrNull() ?: return null
        val lon = prefs[Keys.DEVICE_LON]?.toDoubleOrNull() ?: return null
        return LatLon(lat, lon) to (prefs[Keys.DEVICE_AT] ?: 0L)
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun serializePollutants(map: Map<String, Double>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun parsePollutants(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.getDouble(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        const val DEFAULT_REFRESH_MINUTES = 30
    }
}
