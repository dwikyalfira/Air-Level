package com.airwear.aqi.data

import android.content.Context
import androidx.datastore.core.DataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aqi_cache")

class AqiDataStore(private val context: Context) {

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
        val LOCATION_MODE = stringPreferencesKey("location_mode") // GPS | IP | MANUAL
        val MANUAL_CITY = stringPreferencesKey("manual_city")
        val MANUAL_STATE = stringPreferencesKey("manual_state")
        val MANUAL_COUNTRY = stringPreferencesKey("manual_country")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes")
        val CITY_RANK_JSON = stringPreferencesKey("city_rank_json")
        val CITY_RANK_AT = longPreferencesKey("city_rank_at")
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
        )
    }

    val locationModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOCATION_MODE] ?: "GPS"
    }

    val refreshMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_MINUTES] ?: 30
    }

    suspend fun saveSnapshot(snapshot: AqiSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AQI_US] = snapshot.aqiUs
            snapshot.aqiCn?.let { prefs[Keys.AQI_CN] = it }
            snapshot.mainPollutantUs?.let { prefs[Keys.MAIN_POL] = it }
            snapshot.city?.let { prefs[Keys.CITY] = it }
            snapshot.state?.let { prefs[Keys.STATE] = it }
            snapshot.country?.let { prefs[Keys.COUNTRY] = it }
            snapshot.lat?.let { prefs[Keys.LAT] = it.toFloat() }
            snapshot.lng?.let { prefs[Keys.LNG] = it.toFloat() }
            snapshot.tempC?.let { prefs[Keys.TEMP] = it }
            snapshot.humidity?.let { prefs[Keys.HUMIDITY] = it }
            snapshot.windMs?.let { prefs[Keys.WIND] = it }
            snapshot.weatherIcon?.let { prefs[Keys.ICON] = it }
            prefs[Keys.POLLUTANTS_JSON] = serializePollutants(snapshot.pollutants)
            prefs[Keys.UPDATED_AT] = snapshot.updatedAtEpochMs
        }
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
    }

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
    suspend fun saveCityRank(json: String) {
        context.dataStore.edit {
            it[Keys.CITY_RANK_JSON] = json
            it[Keys.CITY_RANK_AT] = System.currentTimeMillis()
        }
    }

    suspend fun loadCityRankJson(): Pair<String?, Long> {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.CITY_RANK_JSON] to (prefs[Keys.CITY_RANK_AT] ?: 0L)
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
}
