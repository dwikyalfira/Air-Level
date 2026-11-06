package com.airwear.aqi.domain

/** One row in the multi-city ranking list. */
data class CityRankEntry(
    val city: String,
    val state: String?,
    val country: String,
    val aqiUs: Int,
    val category: AqiCategory,
    val updatedAtEpochMs: Long,
) {
    val displayLabel: String
        get() = if (state.isNullOrBlank() || state == city) city else city
}
