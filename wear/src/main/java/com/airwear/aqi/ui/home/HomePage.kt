package com.airwear.aqi.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.airwear.aqi.R
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.ui.components.AqiGauge
import com.airwear.aqi.ui.components.clockTime
import com.airwear.aqi.ui.components.relativeTime
import kotlinx.coroutines.delay

/** MD3 "emphasized decelerate" — fast start, long gentle settle. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Expressive hero: bezel gauge + count-up number, with where the data
 * comes from (source + station measurement time) and when this watch last
 * fetched it. Tapping the status chip refreshes; tapping the number opens
 * Details. No EdgeButton — it would cover the pager's page indicator.
 */
@Composable
fun HomePage(
    state: AqiUiState,
    isManualLocation: Boolean,
    onRefresh: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val glow = remember(scheme.primaryContainer) {
        Brush.radialGradient(
            0f to scheme.primaryContainer.copy(alpha = 0.9f),
            0.72f to Color.Black,
        )
    }

    // One animated value drives the ring sweep and the count-up number.
    val target = (state as? AqiUiState.Ready)?.snapshot?.aqiUs?.toFloat() ?: 0f
    val shown = remember { Animatable(0f) }
    LaunchedEffect(target) {
        shown.animateTo(target, tween(durationMillis = 1100, easing = EmphasizedDecelerate))
    }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glow),
            contentAlignment = Alignment.Center,
        ) {
            AqiGauge(value = { shown.value }, modifier = Modifier.fillMaxSize())

            when (state) {
                AqiUiState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }

                is AqiUiState.Error -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 30.dp),
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = scheme.error, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.error_title),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusChip(
                        text = stringResource(R.string.retry),
                        busy = false,
                        error = false,
                        onClick = onRefresh,
                    )
                }

                is AqiUiState.Ready -> Hero(
                    state = state,
                    shownAqi = { shown.value },
                    isManualLocation = isManualLocation,
                    onRefresh = onRefresh,
                    onOpenDetails = onOpenDetails,
                )
            }
        }
    }
}

@Composable
private fun Hero(
    state: AqiUiState.Ready,
    shownAqi: () -> Float,
    isManualLocation: Boolean,
    onRefresh: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val snapshot = state.snapshot
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val categoryLabel = stringResource(snapshot.category.labelRes)
    val shortLabel = stringResource(snapshot.category.shortLabelRes)
    val a11y = stringResource(R.string.aqi_content_description, snapshot.aqiUs, categoryLabel)

    // Keeps "Updated 2 min. ago" honest while the screen stays on.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Narrow outer padding so the status chip can be wide; text rows add their own.
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Icon(
                imageVector = if (isManualLocation) Icons.Rounded.Place else Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = snapshot.stationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .semantics(mergeDescendants = true) { contentDescription = a11y }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenDetails,
                ),
        ) {
            CountUpNumber(shownAqi)
            Text(
                // Long band names ("Unhealthy for sensitive groups") use the short form.
                text = if (categoryLabel.length > 16) shortLabel else categoryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Source + station measurement time (warns when the station lags).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            if (state.isStale) {
                Icon(Icons.Rounded.Warning, null, tint = scheme.primary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = stringResource(
                    R.string.source_measured,
                    stringResource(R.string.source_short),
                    clockTime(context, snapshot.updatedAtEpochMs),
                ),
                style = MaterialTheme.typography.bodyExtraSmall,
                color = if (state.isStale) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // When this watch last downloaded the reading; tap to refresh.
        StatusChip(
            text = when {
                state.isRefreshing -> stringResource(R.string.refreshing)
                state.softError != null -> state.softError
                else -> stringResource(R.string.updated_ago, relativeTime(snapshot.sourceFetchedAtMs, now))
            },
            busy = state.isRefreshing,
            error = state.softError != null && !state.isRefreshing,
            onClick = onRefresh,
        )
    }
}

/** Reads the animation in its own scope so only this Text recomposes per frame. */
@Composable
private fun CountUpNumber(shownAqi: () -> Float) {
    Text(
        text = shownAqi().toInt().toString(),
        style = MaterialTheme.typography.numeralLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

/**
 * Small status pill: fetch status, tap to refresh. Visually ~24dp so it sits
 * inside the gauge; the invisible outer padding keeps a comfortable touch area.
 */
@Composable
private fun StatusChip(
    text: String,
    busy: Boolean,
    error: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val refreshLabel = stringResource(R.string.refresh)
    val container = if (error) scheme.errorContainer.copy(alpha = 0.7f) else scheme.surfaceContainerHigh
    val content = if (error) scheme.onErrorContainer else scheme.onSurface
    val iconTint = if (error) scheme.onErrorContainer else scheme.primary

    Box(
        modifier = Modifier
            .semantics(mergeDescendants = true) { contentDescription = "$refreshLabel. $text" }
            .clickable(
                enabled = !busy,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(container)
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        ) {
            when {
                busy -> CircularProgressIndicator(modifier = Modifier.size(12.dp))
                error -> Icon(Icons.Rounded.Warning, null, tint = iconTint, modifier = Modifier.size(12.dp))
                else -> Icon(Icons.Rounded.Refresh, null, tint = iconTint, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyExtraSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
