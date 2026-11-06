package com.airwear.aqi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Fallback primary when AQI is unknown. */
val DefaultAccent = Color(0xFF00E400)

/**
 * Wear Material 3 theme. ColorScheme is remembered so we don't rebuild it
 * every recomposition. Typography uses the library default (its constructor
 * is internal in 1.6.x).
 */
@Composable
fun AirWearTheme(
    accent: Color = DefaultAccent,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(accent) {
        ColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            primaryContainer = accent.copy(alpha = 0.28f),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFD0D5DD),
            onSecondary = Color(0xFF1A1B1E),
            secondaryContainer = Color(0xFF2C2E33),
            onSecondaryContainer = Color(0xFFF1F3F5),
            tertiary = Color(0xFF8ECFCC),
            onTertiary = Color(0xFF003330),
            tertiaryContainer = Color(0xFF0F3D3A),
            onTertiaryContainer = Color(0xFFB2EBE6),
            surfaceContainerLow = Color(0xFF0E0F11),
            surfaceContainer = Color(0xFF181A1D),
            surfaceContainerHigh = Color(0xFF24262B),
            onSurface = Color(0xFFF2F4F7),
            onSurfaceVariant = Color(0xFF9EA4AD),
            outline = Color(0xFF5C636E),
            outlineVariant = Color(0xFF3A3F47),
            background = Color(0xFF000000),
            onBackground = Color(0xFFF2F4F7),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
