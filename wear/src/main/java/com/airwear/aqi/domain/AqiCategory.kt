package com.airwear.aqi.domain

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.airwear.aqi.R

/**
 * US EPA AQI bands. [color] is the tone-80 accent used for numbers, pills and
 * the scale bar; the full tonal palette lives in ui/theme/AqiPalette.
 */
enum class AqiCategory(
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int,
    @StringRes val adviceRes: Int,
    val color: Color,
    val rangeMax: Int,
) {
    GOOD(R.string.cat_good, R.string.cat_short_good, R.string.advice_good, Color(0xFF7DDC8A), 50),
    MODERATE(R.string.cat_moderate, R.string.cat_short_moderate, R.string.advice_moderate, Color(0xFFF2D54B), 100),
    UNHEALTHY_SENSITIVE(R.string.cat_usg, R.string.cat_short_usg, R.string.advice_usg, Color(0xFFFFB46B), 150),
    UNHEALTHY(R.string.cat_unhealthy, R.string.cat_short_unhealthy, R.string.advice_unhealthy, Color(0xFFFF8A80), 200),
    VERY_UNHEALTHY(R.string.cat_very_unhealthy, R.string.cat_short_very_unhealthy, R.string.advice_very_unhealthy, Color(0xFFD7A6FF), 300),
    HAZARDOUS(R.string.cat_hazardous, R.string.cat_short_hazardous, R.string.advice_hazardous, Color(0xFFFF7FA0), Int.MAX_VALUE),
    UNKNOWN(R.string.cat_unknown, R.string.cat_short_unknown, R.string.advice_unknown, Color(0xFFA8C7FA), Int.MAX_VALUE);

    /** Lower bound of the band (for the scale bar). */
    val rangeMin: Int
        get() = when (this) {
            GOOD, UNKNOWN -> 0
            else -> entries[ordinal - 1].rangeMax + 1
        }

    companion object {
        fun fromAqi(aqi: Int): AqiCategory = when {
            aqi < 0 -> UNKNOWN
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> UNHEALTHY_SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }

        /** Bands drawn on the scale bar, in order. */
        val scale = listOf(GOOD, MODERATE, UNHEALTHY_SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS)

        /** Scale bar / ranged complication upper bound. */
        const val SCALE_MAX = 300
    }
}

/** Per-category health recommendations shown on the Details page. */
object HealthTips {
    @StringRes
    fun exercise(c: AqiCategory): Int = when (c) {
        AqiCategory.GOOD, AqiCategory.UNKNOWN -> R.string.tip_exercise_ok
        AqiCategory.MODERATE -> R.string.tip_exercise_caution
        AqiCategory.UNHEALTHY_SENSITIVE -> R.string.tip_exercise_reduce
        AqiCategory.UNHEALTHY, AqiCategory.VERY_UNHEALTHY -> R.string.tip_exercise_avoid
        AqiCategory.HAZARDOUS -> R.string.tip_exercise_indoors
    }

    @StringRes
    fun mask(c: AqiCategory): Int = when (c) {
        AqiCategory.GOOD, AqiCategory.MODERATE, AqiCategory.UNKNOWN -> R.string.tip_mask_none
        AqiCategory.UNHEALTHY_SENSITIVE -> R.string.tip_mask_sensitive
        else -> R.string.tip_mask_n95
    }

    @StringRes
    fun windows(c: AqiCategory): Int = when (c) {
        AqiCategory.GOOD, AqiCategory.UNKNOWN -> R.string.tip_window_open
        AqiCategory.MODERATE -> R.string.tip_window_ok
        else -> R.string.tip_window_close
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

/** IQAir weather icon code (e.g. "04n") → condition string. */
@StringRes
fun weatherLabel(icon: String?): Int? = when (icon?.take(2)) {
    "01" -> R.string.weather_clear
    "02" -> R.string.weather_partly_cloudy
    "03", "04" -> R.string.weather_cloudy
    "09", "10" -> R.string.weather_rain
    "11" -> R.string.weather_thunder
    "13" -> R.string.weather_snow
    "50" -> R.string.weather_mist
    else -> null
}
