package com.airwear.aqi.data.iqair

import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import java.time.Instant
import java.time.format.DateTimeFormatter

fun AirVisualResponse.toSnapshot(): AqiSnapshot {
    val data = data ?: return AqiSnapshot.EMPTY
    val pollution = data.current?.pollution
    val weather = data.current?.weather
    val aqiUs = pollution?.aqius ?: return AqiSnapshot.EMPTY
    val coords = data.location?.coordinates
    // AirVisual returns [lng, lat]
    val lng = coords?.getOrNull(0)
    val lat = coords?.getOrNull(1)

    val pollutants = buildMap {
        pollution.p2?.concentration?.let { put("p2", it) }
        pollution.p1?.concentration?.let { put("p1", it) }
        pollution.o3?.concentration?.let { put("o3", it) }
        pollution.n2?.concentration?.let { put("n2", it) }
        pollution.s2?.concentration?.let { put("s2", it) }
        pollution.co?.concentration?.let { put("co", it) }
    }

    val stationTs = parseIsoMillis(pollution.ts)
        ?: parseIsoMillis(weather?.ts)
        ?: System.currentTimeMillis()

    return AqiSnapshot(
        aqiUs = aqiUs,
        aqiCn = pollution.aqicn,
        category = AqiCategory.fromAqi(aqiUs),
        mainPollutantUs = pollution.mainus,
        city = data.city,
        state = data.state,
        country = data.country,
        lat = lat,
        lng = lng,
        tempC = weather?.tp,
        humidity = weather?.hu,
        windMs = weather?.ws?.toInt(),
        weatherIcon = weather?.ic,
        pollutants = pollutants,
        // Prefer station measurement time from IQAir over fetch time
        updatedAtEpochMs = stationTs,
        sourceFetchedAtMs = System.currentTimeMillis(),
    )
}

private fun parseIsoMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Instant.parse(raw).toEpochMilli() }
        .recoverCatching {
            // Some payloads use offset without Z
            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(raw)).toEpochMilli()
        }
        .getOrNull()
}
