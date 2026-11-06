package com.airwear.aqi.ui.cities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.airwear.aqi.R
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.ui.components.AqiPill
import com.airwear.aqi.ui.components.ListItemSpacing
import com.airwear.aqi.ui.components.morph
import com.airwear.aqi.ui.components.relativeTime

sealed class RankingUiState {
    data object Loading : RankingUiState()

    @Immutable
    data class Ready(
        val entries: List<CityRankEntry>,
        val refreshing: Boolean,
        val updatedAtMs: Long,
        val total: Int,
    ) : RankingUiState()

    data class Error(val message: String) : RankingUiState()
}

@Composable
fun CitiesPage(
    state: RankingUiState,
    mine: AqiSnapshot?,
) {
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val spec = rememberTransformationSpec()
    val scheme = MaterialTheme.colorScheme

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = ListItemSpacing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text(stringResource(R.string.cities_title)) }
            }

            item {
                val status = when (state) {
                    RankingUiState.Loading -> stringResource(R.string.loading)
                    is RankingUiState.Error -> state.message
                    is RankingUiState.Ready ->
                        if (state.refreshing && state.entries.size < state.total) {
                            stringResource(R.string.cities_loading, state.entries.size, state.total)
                        } else if (state.refreshing) {
                            stringResource(R.string.refreshing) + "…"
                        } else {
                            stringResource(R.string.cities_updated, relativeTime(state.updatedAtMs))
                        }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().morph(this, spec),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state is RankingUiState.Loading ||
                        (state is RankingUiState.Ready && state.refreshing)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 6.dp).size(14.dp))
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = if (state is RankingUiState.Error) scheme.error else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (mine != null) {
                item {
                    CityRow(
                        rank = null,
                        title = mine.stationLabel,
                        subtitle = stringResource(R.string.your_location),
                        aqi = mine.aqiUs,
                        category = mine.category,
                        highlighted = true,
                        modifier = Modifier.morph(this, spec),
                    )
                }
            }

            if (state is RankingUiState.Ready) {
                items(state.entries, key = { it.city }) { entry ->
                    CityRow(
                        rank = state.entries.indexOf(entry) + 1,
                        title = entry.city,
                        subtitle = entry.country,
                        aqi = entry.aqiUs,
                        category = entry.category,
                        highlighted = false,
                        modifier = Modifier.morph(this, spec),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.data_by_iqair),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).morph(this, spec),
                )
            }
        }
    }
}

@Composable
private fun CityRow(
    rank: Int?,
    title: String,
    subtitle: String,
    aqi: Int,
    category: AqiCategory,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container: Color = if (highlighted) scheme.primaryContainer else scheme.surfaceContainer
    val onContainer: Color = if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AqiPill(aqi = aqi, category = category)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (rank != null) "#$rank · $subtitle" else subtitle,
                style = MaterialTheme.typography.bodyExtraSmall,
                color = onContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
