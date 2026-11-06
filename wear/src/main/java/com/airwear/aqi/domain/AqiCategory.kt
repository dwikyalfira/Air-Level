package com.airwear.aqi.domain

import androidx.compose.ui.graphics.Color

enum class AqiCategory(
    val label: String,
    val shortLabel: String,
    val color: Color,
    /** Dark wash for full-screen background (IQAir-style). */
    val wash: Color,
    val face: String,
    val advice: String,
    val rangeMax: Int,
) {
    GOOD(
        label = "Good",
        shortLabel = "Good",
        color = Color(0xFF7CFC00),
        wash = Color(0xFF0D1F0A),
        face = "😊",
        advice = "Air is clean — enjoy outside",
        rangeMax = 50,
    ),
    MODERATE(
        label = "Moderate",
        shortLabel = "Moderate",
        color = Color(0xFFE8E800),
        wash = Color(0xFF1F1E0A),
        face = "😐",
        advice = "Open windows are fine",
        rangeMax = 100,
    ),
    UNHEALTHY_SENSITIVE(
        label = "Unhealthy for Sensitive",
        shortLabel = "Sensitive",
        color = Color(0xFFFF9F1A),
        wash = Color(0xFF241408),
        face = "😕",
        advice = "Limit outdoor exertion",
        rangeMax = 150,
    ),
    UNHEALTHY(
        label = "Unhealthy",
        shortLabel = "Unhealthy",
        color = Color(0xFFFF4D4D),
        wash = Color(0xFF260A0A),
        face = "😷",
        advice = "Wear a mask outdoors",
        rangeMax = 200,
    ),
    VERY_UNHEALTHY(
        label = "Very Unhealthy",
        shortLabel = "Very bad",
        color = Color(0xFFC77DFF),
        wash = Color(0xFF1A0A24),
        face = "🤢",
        advice = "Avoid outdoor activity",
        rangeMax = 300,
    ),
    HAZARDOUS(
        label = "Hazardous",
        shortLabel = "Hazardous",
        color = Color(0xFFB71C1C),
        wash = Color(0xFF200606),
        face = "☠️",
        advice = "Stay indoors if possible",
        rangeMax = Int.MAX_VALUE,
    ),
    UNKNOWN(
        label = "Unknown",
        shortLabel = "—",
        color = Color(0xFF9E9E9E),
        wash = Color(0xFF121212),
        face = "–",
        advice = "Waiting for data",
        rangeMax = Int.MAX_VALUE,
    );

    companion object {
        fun fromAqi(aqi: Int): AqiCategory = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> UNHEALTHY_SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }

        val maxScale = 300f

        /** EPA scale stops for gradient rings. */
        val scaleColors = listOf(
            0 to Color(0xFF00E400),
            50 to Color(0xFFFFFF00),
            100 to Color(0xFFFF7E00),
            150 to Color(0xFFFF0000),
            200 to Color(0xFF8F3F97),
            300 to Color(0xFF7E0023),
        )
    }
}

fun pollutantLabel(code: String?): String = when (code?.lowercase()) {
    "p2", "pm25" -> "PM2.5"
    "p1", "pm10" -> "PM10"
    "o3" -> "O₃"
    "n2", "no2" -> "NO₂"
    "s2", "so2" -> "SO₂"
    "co" -> "CO"
    else -> code?.uppercase() ?: "—"
}
