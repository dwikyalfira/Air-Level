package com.airwear.aqi.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText

/**
 * Settings: cycle GPS → IP → Manual.
 * When Manual, a city list is shown — tap a city to use it.
 */
@Composable
fun SettingsScreen(
    locationMode: String,
    refreshMinutes: Int,
    hasLocationPermission: Boolean,
    manualCity: String,
    onLocationModeChange: (String) -> Unit,
    onRefreshChange: (Int) -> Unit,
    onManualCitySelected: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val nextRefresh = when (refreshMinutes) {
        15 -> 30
        30 -> 60
        else -> 15
    }
    val nextMode = when (locationMode) {
        "GPS" -> "IP"
        "IP" -> "MANUAL"
        else -> "GPS"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 32.dp, bottom = 28.dp),
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = "SETTINGS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                SettingsRow(
                    title = "Location",
                    value = when (locationMode) {
                        "GPS" -> "GPS + IP"
                        "IP" -> "IP address"
                        else -> "Manual · $manualCity"
                    },
                    hint = "Tap → $nextMode",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onLocationModeChange(nextMode) },
                )
            }

            if (locationMode == "MANUAL") {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "CHOOSE CITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.1.sp,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(ManualCities.list) { spec ->
                    CityRow(
                        name = spec.city,
                        selected = spec.city == manualCity,
                        onClick = {
                            onManualCitySelected(spec.city, spec.state, spec.country)
                        },
                    )
                }
            }

            item {
                SettingsRow(
                    title = "Refresh",
                    value = "$refreshMinutes min",
                    hint = "Tap → $nextRefresh min",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onRefreshChange(nextRefresh) },
                )
            }
        }

        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    hint: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = hint,
                fontSize = 11.sp,
                color = accent,
            )
        }
    }
}

@Composable
private fun CityRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("●", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 8.dp))
        }
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
