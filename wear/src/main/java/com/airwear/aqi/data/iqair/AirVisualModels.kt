package com.airwear.aqi.data.iqair

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class AirVisualResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: AirVisualData? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualData(
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "location") val location: AirVisualLocation? = null,
    @Json(name = "current") val current: AirVisualCurrent? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualLocation(
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: List<Double>? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualCurrent(
    @Json(name = "weather") val weather: AirVisualWeather? = null,
    @Json(name = "pollution") val pollution: AirVisualPollution? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualWeather(
    @Json(name = "ts") val ts: String? = null,
    @Json(name = "tp") val tp: Int? = null,
    @Json(name = "pr") val pr: Int? = null,
    @Json(name = "hu") val hu: Int? = null,
    @Json(name = "ws") val ws: Double? = null,
    @Json(name = "wd") val wd: Int? = null,
    @Json(name = "ic") val ic: String? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualPollution(
    @Json(name = "ts") val ts: String? = null,
    @Json(name = "aqius") val aqius: Int? = null,
    @Json(name = "mainus") val mainus: String? = null,
    @Json(name = "aqicn") val aqicn: Int? = null,
    @Json(name = "maincn") val maincn: String? = null,
    @Json(name = "p2") val p2: AirVisualPollutant? = null,
    @Json(name = "p1") val p1: AirVisualPollutant? = null,
    @Json(name = "o3") val o3: AirVisualPollutant? = null,
    @Json(name = "n2") val n2: AirVisualPollutant? = null,
    @Json(name = "s2") val s2: AirVisualPollutant? = null,
    @Json(name = "co") val co: AirVisualPollutant? = null,
)

@JsonClass(generateAdapter = false)
data class AirVisualPollutant(
    @Json(name = "concentration") val concentration: Double? = null,
    @Json(name = "aqius") val aqius: Int? = null,
    @Json(name = "aqicn") val aqicn: Int? = null,
)
