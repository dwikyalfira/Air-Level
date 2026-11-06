package com.airwear.aqi.data.iqair

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * IQAir AirVisual API.
 * Base URL: https://api.airvisual.com/
 * Free Community plan: 5 req/min, 500/day.
 */
interface AirVisualApi {

    /**
     * Nearest city to coordinates (or IP if lat/lon omitted).
     * Coordinates are preferred when GPS is available.
     */
    @GET("v2/nearest_city")
    suspend fun nearestCity(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("key") key: String,
    ): AirVisualResponse

    @GET("v2/city")
    suspend fun city(
        @Query("city") city: String,
        @Query("state") state: String,
        @Query("country") country: String,
        @Query("key") key: String,
    ): AirVisualResponse
}
