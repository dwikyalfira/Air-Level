package com.airwear.aqi.data.iqair

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirVisualResponse(
    @SerialName("status") val status: String? = null,
    @SerialName("data") val data: AirVisualData? = null,
)

@Serializable
data class AirVisualData(
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("location") val location: AirVisualLocation? = null,
    @SerialName("current") val current: AirVisualCurrent? = null,
)

@Serializable
data class AirVisualLocation(
    @SerialName("type") val type: String? = null,
    @SerialName("coordinates") val coordinates: List<Double>? = null,
)

@Serializable
data class AirVisualCurrent(
    @SerialName("weather") val weather: AirVisualWeather? = null,
    @SerialName("pollution") val pollution: AirVisualPollution? = null,
)

@Serializable
data class AirVisualWeather(
    @SerialName("ts") val ts: String? = null,
    @SerialName("tp") val tp: Int? = null,
    @SerialName("pr") val pr: Int? = null,
    @SerialName("hu") val hu: Int? = null,
    @SerialName("ws") val ws: Double? = null,
    @SerialName("wd") val wd: Int? = null,
    @SerialName("ic") val ic: String? = null,
)

@Serializable
data class AirVisualPollution(
    @SerialName("ts") val ts: String? = null,
    @SerialName("aqius") val aqius: Int? = null,
    @SerialName("mainus") val mainus: String? = null,
    @SerialName("aqicn") val aqicn: Int? = null,
    @SerialName("maincn") val maincn: String? = null,
    @SerialName("p2") val p2: AirVisualPollutant? = null,
    @SerialName("p1") val p1: AirVisualPollutant? = null,
    @SerialName("o3") val o3: AirVisualPollutant? = null,
    @SerialName("n2") val n2: AirVisualPollutant? = null,
    @SerialName("s2") val s2: AirVisualPollutant? = null,
    @SerialName("co") val co: AirVisualPollutant? = null,
)

@Serializable
data class AirVisualPollutant(
    @SerialName("conc") val concentration: Double? = null,
    @SerialName("aqius") val aqius: Int? = null,
    @SerialName("aqicn") val aqicn: Int? = null,
)
