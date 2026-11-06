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
    /** When this device last downloaded the payload. 0 = unknown / invalidated. */
    val sourceFetchedAtMs: Long = System.currentTimeMillis(),
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

    /** Station measurement is old enough that the user should be told. */
    fun isStale(nowMs: Long = System.currentTimeMillis()): Boolean =
        updatedAtEpochMs > 0L && nowMs - updatedAtEpochMs > STALE_AFTER_MS

    companion object {
        /** IQAir stations report hourly; > 2 h means the feed is lagging. */
        const val STALE_AFTER_MS = 2 * 60 * 60_000L

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

    /**
     * Data is available. [softError] is a failed refresh that did not wipe
     * the cached reading (offline, rate limited…).
     */
    data class Ready(
        val snapshot: AqiSnapshot,
        val isRefreshing: Boolean = false,
        val softError: String? = null,
        val isStale: Boolean = false,
    ) : AqiUiState()

    /** No cached reading at all and the fetch failed. */
    data class Error(val message: String) : AqiUiState()
}
