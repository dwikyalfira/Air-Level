package com.airwear.aqi.data

import com.airwear.aqi.data.iqair.AirVisualCurrent
import com.airwear.aqi.data.iqair.AirVisualData
import com.airwear.aqi.data.iqair.AirVisualPollution
import com.airwear.aqi.data.iqair.AirVisualResponse
import com.airwear.aqi.data.iqair.AirVisualWeather
import com.airwear.aqi.data.iqair.toSnapshot
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.CityRankEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MappingTest {

    @Test
    fun categoryBoundaries() {
        assertEquals(AqiCategory.GOOD, AqiCategory.fromAqi(50))
        assertEquals(AqiCategory.MODERATE, AqiCategory.fromAqi(51))
        assertEquals(AqiCategory.MODERATE, AqiCategory.fromAqi(100))
        assertEquals(AqiCategory.UNHEALTHY_SENSITIVE, AqiCategory.fromAqi(101))
        assertEquals(AqiCategory.UNHEALTHY, AqiCategory.fromAqi(200))
        assertEquals(AqiCategory.VERY_UNHEALTHY, AqiCategory.fromAqi(300))
        assertEquals(AqiCategory.HAZARDOUS, AqiCategory.fromAqi(301))
    }

    @Test
    fun parsesRealPayloadShape() {
        val raw = """
            {"status":"success","data":{"city":"Tabing","state":"West Sumatra","country":"Indonesia",
             "location":{"type":"Point","coordinates":[100.35,-0.876]},
             "current":{"pollution":{"ts":"2026-11-06T12:00:00.000Z","aqius":56,"mainus":"p2","aqicn":20,"maincn":"p2"},
                        "weather":{"ts":"2026-11-06T13:00:00.000Z","ic":"04n","hu":89,"pr":1010,"tp":25,"wd":120,"ws":1.2,"heatIndex":27}}}}
        """.trimIndent()
        val resp = Json { ignoreUnknownKeys = true }.decodeFromString(AirVisualResponse.serializer(), raw)
        val snap = resp.toSnapshot(fetchedAtMs = 42L)
        assertEquals(56, snap.aqiUs)
        assertEquals(AqiCategory.MODERATE, snap.category)
        assertEquals(-0.876, snap.lat!!, 1e-9)
        assertEquals(100.35, snap.lng!!, 1e-9)
        assertEquals(1, snap.windMs)
        assertEquals(42L, snap.sourceFetchedAtMs)
        assertEquals(java.time.Instant.parse("2026-11-06T12:00:00Z").toEpochMilli(), snap.updatedAtEpochMs)
    }

    @Test
    fun missingPollutionGivesEmptySnapshot() {
        val resp = AirVisualResponse(status = "success", data = AirVisualData(city = "X"))
        assertEquals(-1, resp.toSnapshot().aqiUs)
    }

    @Test
    fun missingTimestampFallsBackToFetchTime() {
        val resp = AirVisualResponse(
            status = "success",
            data = AirVisualData(
                current = AirVisualCurrent(
                    pollution = AirVisualPollution(aqius = 10),
                    weather = AirVisualWeather(),
                ),
            ),
        )
        assertEquals(99L, resp.toSnapshot(fetchedAtMs = 99L).updatedAtEpochMs)
    }

    @Test
    fun staleAfterTwoHours() {
        val now = 1_800_000_000_000L
        val fresh = AqiSnapshot.EMPTY.copy(updatedAtEpochMs = now - 60 * 60_000L)
        val old = AqiSnapshot.EMPTY.copy(updatedAtEpochMs = now - 3 * 60 * 60_000L)
        assertFalse(fresh.isStale(now))
        assertTrue(old.isStale(now))
    }

    @Test
    fun rankListDropsCitiesNoLongerWatchedAndSorts() {
        fun e(city: String, aqi: Int) =
            CityRankEntry(city, null, "X", aqi, AqiCategory.fromAqi(aqi), 0L)
        val cached = listOf(e("Singapore", 104), e("Jakarta", 110), e("Cairo", 80), e("Tokyo", 22), e("Delhi", 150))
        val kept = RankList.sorted(RankList.keepWatched(cached, setOf("Jakarta", "Tokyo", "Delhi")))
        assertEquals(listOf("Delhi", "Jakarta", "Tokyo"), kept.map { it.city })
    }

    @Test
    fun friendlyErrors() {
        assertEquals("Network timeout", friendlyError(SocketTimeoutException()).message)
        assertEquals("No internet connection", friendlyError(UnknownHostException()).message)
        assertEquals("Network error", friendlyError(IOException()).message)
        val quota = DailyQuotaExceededException()
        assertTrue(friendlyError(quota) === quota)
        assertEquals("Choose a city in Settings", friendlyError(IllegalStateException("Choose a city in Settings")).message)
    }
}
