package com.airwear.aqi.ui.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.pollutantLabel
import com.airwear.aqi.ui.components.AccentChip
import com.airwear.aqi.ui.components.InfoCard
import com.airwear.aqi.ui.components.SectionLabel
import com.airwear.aqi.ui.components.formatClock
import com.airwear.aqi.ui.components.formatUpdatedAgo

@Composable
fun DetailsScreen(
    snapshot: AqiSnapshot,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val pollutantList = snapshot.pollutants.entries.toList()
    val accent = snapshot.category.color

    Box(modifier = modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 32.dp, bottom = 20.dp),
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "AQI ${snapshot.aqiUs}",
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    AccentChip(label = snapshot.category.label, accent = accent)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = snapshot.stationLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = listOfNotNull(
                            snapshot.state?.takeIf { it != snapshot.city },
                            snapshot.country,
                        ).joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                InfoCard(
                    title = "Station update",
                    value = formatUpdatedAgo(snapshot.updatedAtEpochMs),
                    supporting = "Data time · ${formatClock(snapshot.updatedAtEpochMs)}",
                    accent = accent,
                )
            }

            snapshot.mainPollutantUs?.let { code ->
                item {
                    InfoCard(
                        title = "Dominant pollutant",
                        value = pollutantLabel(code),
                        accent = accent,
                        supporting = "US EPA AQI",
                    )
                }
            }

            if (pollutantList.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SectionLabel("Pollutants", Modifier.padding(top = 6.dp))
                    }
                }
                items(pollutantList) { (code, value) ->
                    InfoCard(
                        title = pollutantLabel(code),
                        value = formatConcentration(code, value),
                        supporting = concentrationUnitNote(code),
                    )
                }
            }

            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Weather", Modifier.padding(top = 6.dp))
                }
                InfoCard(
                    title = "Conditions",
                    value = buildString {
                        snapshot.tempC?.let { append("${it}°C") }
                        if (isBlank()) append("—")
                    },
                    supporting = buildString {
                        snapshot.humidity?.let { append("Humidity $it%") }
                        snapshot.windMs?.let {
                            if (isNotEmpty()) append("  ·  ")
                            append("Wind ${it} m/s")
                        }
                    }.ifBlank { null },
                )
            }

            if (snapshot.lat != null && snapshot.lng != null) {
                item {
                    InfoCard(
                        title = "Coordinates",
                        value = String.format("%.3f, %.3f", snapshot.lat, snapshot.lng),
                        supporting = "Nearest city feed",
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Data by IQAir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        TimeText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
        )
    }
}

private fun formatConcentration(code: String, value: Double): String {
    return when (code.lowercase()) {
        "co" -> String.format("%.1f", value)
        else -> String.format("%.0f", value)
    }
}

private fun concentrationUnitNote(code: String): String? = when (code.lowercase()) {
    "co" -> "ppm"
    "o3", "n2", "s2" -> "ppb"
    else -> "µg/m³"
}
