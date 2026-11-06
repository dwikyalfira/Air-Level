package com.airwear.aqi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TimeText
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.ui.components.AqiFaceNumber
import com.airwear.aqi.ui.components.SensorCard
import com.airwear.aqi.ui.components.formatUpdatedAgo

/**
 * Scrollable Home. Vertical scroll only (so it does not fight HorizontalPager).
 * Top padding clears TimeText; bottom padding keeps the sensor card on the round face.
 */
@Composable
fun HomeScreen(
    state: AqiUiState,
    onOpenDetails: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wash = when (state) {
        is AqiUiState.Ready -> state.snapshot.category.wash
        is AqiUiState.Error -> state.cached?.category?.wash ?: Color(0xFF121212)
        AqiUiState.Loading -> Color(0xFF121212)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(wash),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                AqiUiState.Loading -> {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading…",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                is AqiUiState.Ready -> Hero(snapshot = state.snapshot)

                is AqiUiState.Error -> {
                    // Only shown when there is no cached AQI at all
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
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
private fun Hero(snapshot: AqiSnapshot) {
    val category = snapshot.category
    val city = (snapshot.city ?: snapshot.stationLabel).uppercase()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = city.take(14),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(Modifier.height(8.dp))

        AqiFaceNumber(
            aqi = snapshot.aqiUs,
            category = category,
            numberSize = 50.sp,
            faceSize = 32.sp,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = category.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = category.color,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = category.advice,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Spacer(Modifier.height(10.dp))

        SensorCard(
            station = snapshot.stationLabel,
            sourceLine = "IQAir · ${formatUpdatedAgo(snapshot.updatedAtEpochMs)}",
            accent = category.color,
            compact = true,
        )
    }
}
