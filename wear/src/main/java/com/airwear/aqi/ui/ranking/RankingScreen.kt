package com.airwear.aqi.ui.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.ui.components.formatUpdatedAgo

sealed class RankingUiState {
    data object Loading : RankingUiState()
    data class Ready(
        val entries: List<CityRankEntry>,
        val refreshing: Boolean,
        val updatedAtMs: Long,
    ) : RankingUiState()

    data class Error(val message: String) : RankingUiState()
}

@Composable
fun RankingScreen(
    state: RankingUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 32.dp, bottom = 20.dp),
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                RankingUiState.Loading -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Loading cities…",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }
                }

                is RankingUiState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                is RankingUiState.Ready -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOP AQI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp,
                                color = Color.White,
                            )
                            if (state.refreshing) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Updating…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Updated ${formatUpdatedAgo(state.updatedAtMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }

                    if (state.entries.isEmpty()) {
                        item {
                            Text(
                                text = "No city data yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    } else {
                        itemsIndexed(state.entries.take(5)) { index, entry ->
                            RankRow(rank = index + 1, entry = entry)
                        }
                    }

                    item {
                        Text(
                            text = "Data by IQAir",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
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
private fun RankRow(rank: Int, entry: CityRankEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString().padStart(2, '0'),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = entry.city,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.aqiUs.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = entry.category.color,
        )
    }
}
