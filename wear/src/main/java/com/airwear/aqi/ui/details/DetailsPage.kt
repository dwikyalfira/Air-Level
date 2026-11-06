package com.airwear.aqi.ui.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.airwear.aqi.R
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.domain.HealthTips
import com.airwear.aqi.domain.pollutantLabel
import com.airwear.aqi.domain.weatherLabel
import com.airwear.aqi.ui.components.InfoSurface
import com.airwear.aqi.ui.components.ListItemSpacing
import com.airwear.aqi.ui.components.SurfaceTitle
import com.airwear.aqi.ui.components.TipRow
import com.airwear.aqi.ui.components.clockTime
import com.airwear.aqi.ui.components.morph
import com.airwear.aqi.ui.components.relativeTime

@Composable
fun DetailsPage(
    snapshot: AqiSnapshot?,
    refreshMinutes: Int,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val spec = rememberTransformationSpec()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // The page is pre-composed (pager neighbour) before data arrives; when the
    // cards get inserted the list keeps its old anchor and opens mid-scroll.
    // Snap back to the header once data is present.
    val hasData = snapshot != null
    LaunchedEffect(hasData) {
        if (hasData) listState.scrollToItem(0)
    }

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
                ) { Text(stringResource(R.string.details_title)) }
            }

            if (snapshot == null) {
                item {
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().morph(this, spec),
                    )
                }
            } else {
                val category = snapshot.category

                item {
                    InfoSurface(modifier = Modifier.morph(this, spec), container = scheme.primaryContainer) {
                        Text(
                            text = "${snapshot.aqiUs}",
                            style = MaterialTheme.typography.numeralSmall,
                            color = scheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(category.adviceRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }

                item {
                    InfoSurface(modifier = Modifier.morph(this, spec)) {
                        SurfaceTitle(stringResource(R.string.health_title))
                        TipRow(Icons.Rounded.Favorite, stringResource(HealthTips.exercise(category)), scheme.primary)
                        TipRow(Icons.Rounded.Face, stringResource(HealthTips.mask(category)), scheme.primary)
                        TipRow(Icons.Rounded.Home, stringResource(HealthTips.windows(category)), scheme.primary)
                    }
                }

                snapshot.mainPollutantUs?.let { code ->
                    item {
                        InfoSurface(modifier = Modifier.morph(this, spec)) {
                            SurfaceTitle(stringResource(R.string.pollutant_title))
                            Text(pollutantLabel(code), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = snapshot.aqiCn?.let {
                                    stringResource(R.string.aqi_us_cn, snapshot.aqiUs, it)
                                } ?: stringResource(R.string.aqi_us_only, snapshot.aqiUs),
                                style = MaterialTheme.typography.bodyExtraSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (snapshot.tempC != null || snapshot.humidity != null) {
                    item {
                        InfoSurface(modifier = Modifier.morph(this, spec)) {
                            SurfaceTitle(stringResource(R.string.weather_title))
                            Text(
                                text = listOfNotNull(
                                    snapshot.tempC?.let { "$it°C" },
                                    weatherLabel(snapshot.weatherIcon)?.let { stringResource(it) },
                                ).joinToString("  ·  "),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (snapshot.humidity != null && snapshot.windMs != null) {
                                Text(
                                    text = stringResource(R.string.humidity_wind, snapshot.humidity, snapshot.windMs),
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    InfoSurface(modifier = Modifier.morph(this, spec)) {
                        SurfaceTitle(stringResource(R.string.source_title))
                        Text(stringResource(R.string.source_provider), style = MaterialTheme.typography.titleMedium)
                        val station = listOfNotNull(
                            snapshot.stationLabel,
                            snapshot.state?.takeIf { it != snapshot.city },
                            snapshot.country,
                        ).joinToString(", ")
                        Text(
                            text = stringResource(R.string.source_station, station),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.measured_at,
                                clockTime(context, snapshot.updatedAtEpochMs),
                                relativeTime(snapshot.updatedAtEpochMs),
                            ),
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.fetched_at,
                                clockTime(context, snapshot.sourceFetchedAtMs),
                                relativeTime(snapshot.sourceFetchedAtMs),
                            ),
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.auto_refresh_every, refreshMinutes),
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.data_by_iqair),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .morph(this, spec),
                )
            }
        }
    }
}
