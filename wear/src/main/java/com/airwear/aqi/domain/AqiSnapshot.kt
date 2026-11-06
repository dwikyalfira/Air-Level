package com.airwear.aqi.domain

data class AqiSnapshot(
    val aqiUs: Int,
    val aqiCn: Int?,
    val category: AqiCategory,
    val mainPollutantUs: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val lat: Double?,
    val lng: Double?,
    val tempC: Int?,
    val humidity: Int?,
    val windMs: Int?,
    val weatherIcon: String?,
    val pollutants: Map<String, Double> = emptyMap(),
    /** Measurement time from IQAir (pollution.ts), not necessarily when we fetched. */
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    /** When this device last downloaded the payload. */
    val sourceFetchedAtMs: Long = System.currentTimeMillis(),
    val isStale: Boolean = false,
) {
    /** City/station name from the nearest_city feed. */
    val stationLabel: String
        get() = city
            ?: listOfNotNull(state?.takeIf { it != city }, country)
                .joinToString(", ")
                .ifBlank { "Unknown station" }

    val locationLabel: String
        get() = listOfNotNull(city, state?.takeIf { it != city }, country)
            .joinToString(", ")
            .ifBlank { "Unknown location" }

    val aqiDisplay: String
        get() = aqiUs.toString()

    val categoryLabel: String
        get() = category.label

    companion object {
        val EMPTY = AqiSnapshot(
            aqiUs = -1,
            aqiCn = null,
            category = AqiCategory.UNKNOWN,
            mainPollutantUs = null,
            city = null,
            state = null,
            country = null,
            lat = null,
            lng = null,
            tempC = null,
            humidity = null,
            windMs = null,
            weatherIcon = null,
        )
    }
}

sealed class AqiUiState {
    data object Loading : AqiUiState()
    data class Ready(val snapshot: AqiSnapshot, val fromCache: Boolean = false) : AqiUiState()
    data class Error(val message: String, val cached: AqiSnapshot? = null) : AqiUiState()
}
