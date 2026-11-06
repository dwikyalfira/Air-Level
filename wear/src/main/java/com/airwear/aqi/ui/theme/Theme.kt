package com.airwear.aqi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.airwear.aqi.domain.AqiCategory

/** One MD3 tonal set per AQI band (tones ≈ 80 / 70 / 30 / 20 / 90). */
@Immutable
data class TonalSet(
    val primary: Color,
    val primaryDim: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
)

object AqiPalette {
    fun of(category: AqiCategory): TonalSet = when (category) {
        AqiCategory.GOOD -> TonalSet(
            Color(0xFF7DDC8A), Color(0xFF5FBF6D), Color(0xFF1E4A28), Color(0xFF00391A), Color(0xFFB8F5C0),
        )
        AqiCategory.MODERATE -> TonalSet(
            Color(0xFFF2D54B), Color(0xFFD4B830), Color(0xFF4A3F00), Color(0xFF3A3000), Color(0xFFFFEB99),
        )
        AqiCategory.UNHEALTHY_SENSITIVE -> TonalSet(
            Color(0xFFFFB46B), Color(0xFFE8994C), Color(0xFF5A3300), Color(0xFF4A2800), Color(0xFFFFDCBC),
        )
        AqiCategory.UNHEALTHY -> TonalSet(
            Color(0xFFFF8A80), Color(0xFFE86C62), Color(0xFF6B1A16), Color(0xFF5C0F0B), Color(0xFFFFDAD5),
        )
        AqiCategory.VERY_UNHEALTHY -> TonalSet(
            Color(0xFFD7A6FF), Color(0xFFBB86F0), Color(0xFF4D2470), Color(0xFF3D1166), Color(0xFFF0DBFF),
        )
        AqiCategory.HAZARDOUS -> TonalSet(
            Color(0xFFFF7FA0), Color(0xFFE85F84), Color(0xFF6E1230), Color(0xFF5A0A24), Color(0xFFFFD9E1),
        )
        AqiCategory.UNKNOWN -> TonalSet(
            Color(0xFFA8C7FA), Color(0xFF8AAEE8), Color(0xFF284777), Color(0xFF062E6F), Color(0xFFD6E3FF),
        )
    }
}

/**
 * Wear Material 3 theme. Only depends on the AQI *category*, so data updates
 * that keep the same band never rebuild the scheme or recompose the tree.
 */
@Composable
fun AirWearTheme(
    category: AqiCategory,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(category) { schemeFor(category) }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun schemeFor(category: AqiCategory): ColorScheme {
    val t = AqiPalette.of(category)
    return ColorScheme(
        primary = t.primary,
        primaryDim = t.primaryDim,
        primaryContainer = t.primaryContainer,
        onPrimary = t.onPrimary,
        onPrimaryContainer = t.onPrimaryContainer,
        secondary = Color(0xFFC3C8D2),
        secondaryDim = Color(0xFFA6ABB5),
        secondaryContainer = Color(0xFF2F333A),
        onSecondary = Color(0xFF2C3038),
        onSecondaryContainer = Color(0xFFDFE3EC),
        tertiary = Color(0xFF8FD3CF),
        tertiaryDim = Color(0xFF72B7B3),
        tertiaryContainer = Color(0xFF124C49),
        onTertiary = Color(0xFF003734),
        onTertiaryContainer = Color(0xFFB0F0EB),
        surfaceContainerLow = Color(0xFF15171A),
        surfaceContainer = Color(0xFF1F2125),
        surfaceContainerHigh = Color(0xFF2A2C31),
        onSurface = Color(0xFFE6E7EB),
        onSurfaceVariant = Color(0xFFC2C6CF),
        outline = Color(0xFF8C9099),
        outlineVariant = Color(0xFF43474E),
        background = Color.Black,
        onBackground = Color(0xFFE6E7EB),
        error = Color(0xFFFFB4AB),
        errorDim = Color(0xFFE89990),
        errorContainer = Color(0xFF93000A),
        onError = Color(0xFF690005),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}
