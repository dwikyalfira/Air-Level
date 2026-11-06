package com.airwear.aqi.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.airwear.aqi.BuildConfig
import com.airwear.aqi.R
import com.airwear.aqi.ui.components.InfoSurface
import com.airwear.aqi.ui.components.ListItemSpacing
import com.airwear.aqi.ui.components.morph

/** Shared list shell for every settings screen. */
@Composable
private fun SettingsList(
    title: String,
    content: TransformingLazyColumnScope.(TransformationSpec) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val spec = rememberTransformationSpec()
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
                ) { Text(title) }
            }
            content(spec)
        }
    }
}

@Composable
private fun locationSummary(mode: String, manualCity: String): String = when (mode) {
    "GPS" -> stringResource(R.string.loc_gps)
    "IP" -> stringResource(R.string.loc_ip)
    else -> manualCity
}

@Composable
fun SettingsScreen(
    locationMode: String,
    manualCity: String,
    refreshMinutes: Int,
    onOpenLocation: () -> Unit,
    onOpenInterval: () -> Unit,
) {
    val locationLabel = locationSummary(locationMode, manualCity)
    val intervalLabel = stringResource(R.string.refresh_every, refreshMinutes)
    val titles = listOf(
        stringResource(R.string.setting_location),
        stringResource(R.string.setting_refresh),
        stringResource(R.string.about),
        stringResource(R.string.version, BuildConfig.VERSION_NAME),
        stringResource(R.string.data_by_iqair),
        stringResource(R.string.complication_hint),
    )

    SettingsList(stringResource(R.string.settings)) { spec ->
        item {
            FilledTonalButton(
                onClick = onOpenLocation,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = {
                    Icon(
                        if (locationMode == "MANUAL") Icons.Rounded.Place else Icons.Rounded.LocationOn,
                        contentDescription = null,
                    )
                },
                label = { Text(titles[0]) },
                secondaryLabel = { Text(locationLabel) },
            )
        }
        item {
            FilledTonalButton(
                onClick = onOpenInterval,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                label = { Text(titles[1]) },
                secondaryLabel = { Text(intervalLabel) },
            )
        }
        item {
            ListSubHeader(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            ) { Text(titles[2]) }
        }
        item {
            InfoSurface(modifier = Modifier.morph(this, spec)) {
                Text(titles[3], style = MaterialTheme.typography.labelMedium)
                Text(
                    titles[4],
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    titles[5],
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun LocationScreen(
    locationMode: String,
    hasLocationPermission: () -> Boolean,
    onSelect: (String) -> Unit,
    onChooseCity: () -> Unit,
) {
    var permissionDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            onSelect("GPS")
        } else {
            permissionDenied = true
        }
    }

    val options = listOf(
        Triple("GPS", stringResource(R.string.loc_gps), stringResource(R.string.loc_gps_desc)),
        Triple("IP", stringResource(R.string.loc_ip), stringResource(R.string.loc_ip_desc)),
        Triple("MANUAL", stringResource(R.string.loc_manual), stringResource(R.string.loc_manual_desc)),
    )
    val deniedText = stringResource(R.string.loc_permission_denied)

    SettingsList(stringResource(R.string.setting_location)) { spec ->
        items(options, key = { it.first }) { (mode, label, desc) ->
            RadioButton(
                selected = locationMode == mode,
                onSelect = {
                    when {
                        mode == "MANUAL" -> onChooseCity()
                        mode == "GPS" && !hasLocationPermission() -> launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                        else -> onSelect(mode)
                    }
                },
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                label = { Text(label) },
                secondaryLabel = {
                    Text(if (mode == "GPS" && permissionDenied) deniedText else desc)
                },
            )
        }
    }
}

@Composable
fun CityPickerScreen(
    selectedCity: String?,
    onSelect: (ManualCities.Spec) -> Unit,
) {
    SettingsList(stringResource(R.string.choose_city)) { spec ->
        items(ManualCities.list, key = { it.city }) { city ->
            RadioButton(
                selected = city.city == selectedCity,
                onSelect = { onSelect(city) },
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                label = { Text(city.city) },
                secondaryLabel = { Text(city.country) },
            )
        }
    }
}

@Composable
fun IntervalScreen(
    refreshMinutes: Int,
    onSelect: (Int) -> Unit,
) {
    val labels = REFRESH_OPTIONS.associateWith { stringResource(R.string.refresh_every, it) }
    SettingsList(stringResource(R.string.setting_refresh)) { spec ->
        items(REFRESH_OPTIONS, key = { it }) { minutes ->
            RadioButton(
                selected = refreshMinutes == minutes,
                onSelect = { onSelect(minutes) },
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                label = { Text(labels.getValue(minutes)) },
            )
        }
    }
}

private val REFRESH_OPTIONS = listOf(15, 30, 60)
