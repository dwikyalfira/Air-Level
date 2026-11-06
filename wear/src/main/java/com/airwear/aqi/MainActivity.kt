package com.airwear.aqi

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.ui.AqiViewModel
import com.airwear.aqi.ui.details.DetailsScreen
import com.airwear.aqi.ui.home.HomeScreen
import com.airwear.aqi.ui.ranking.RankingScreen
import com.airwear.aqi.ui.ranking.RankingUiState
import com.airwear.aqi.ui.settings.SettingsScreen
import com.airwear.aqi.ui.theme.AirWearTheme
import com.airwear.aqi.ui.theme.DefaultAccent

class MainActivity : ComponentActivity() {

    private var vmRef: AqiViewModel? = null

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                vmRef?.setLocationMode("GPS")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as AqiApp
            val vm: AqiViewModel = viewModel(factory = AqiViewModel.factory(app.repository))
            vmRef = vm

            val state by vm.uiState.collectAsState()
            val locationMode by vm.locationMode.collectAsState()
            val refreshMinutes by vm.refreshMinutes.collectAsState()
            val manualCity by vm.manualCityName.collectAsState()

            val accent = when (val s = state) {
                is AqiUiState.Ready -> s.snapshot.category.color
                is AqiUiState.Error -> s.cached?.category?.color ?: DefaultAccent
                AqiUiState.Loading -> DefaultAccent
            }

            AirWearTheme(accent = accent) {
                val rankingState by vm.rankingState.collectAsState()
                WearApp(
                    state = state,
                    rankingState = rankingState,
                    locationMode = locationMode,
                    refreshMinutes = refreshMinutes,
                    manualCity = manualCity,
                    hasLocationPermission = vm.hasLocationPermission(),
                    onRetry = { vm.refresh() },
                    onRankingVisible = { vm.onRankingVisible() },
                    onCycleLocation = {
                        val next = when (locationMode) {
                            "GPS" -> "IP"
                            "IP" -> "MANUAL"
                            else -> "GPS"
                        }
                        if (next == "GPS" && !vm.hasLocationPermission()) {
                            locationPermission.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        } else {
                            vm.setLocationMode(next)
                        }
                    },
                    onManualCity = { city, stateName, country ->
                        vm.setManualCity(city, stateName, country)
                    },
                    onCycleRefresh = {
                        val next = when (refreshMinutes) {
                            15 -> 30
                            30 -> 60
                            else -> 15
                        }
                        vm.setRefreshMinutes(next)
                    },
                )
            }
        }
    }
}

@Composable
private fun WearApp(
    state: AqiUiState,
    rankingState: RankingUiState,
    locationMode: String,
    refreshMinutes: Int,
    manualCity: String,
    hasLocationPermission: Boolean,
    onRetry: () -> Unit,
    onRankingVisible: () -> Unit,
    onCycleLocation: () -> Unit,
    onManualCity: (String, String, String) -> Unit,
    onCycleRefresh: () -> Unit,
) {
    // Ranking ← Home → Details → Settings
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { it },
    ) { page ->
        when (page) {
            0 -> {
                LaunchedEffect(Unit) { onRankingVisible() }
                RankingScreen(state = rankingState, modifier = Modifier.fillMaxSize())
            }

            1 -> HomeScreen(
                state = state,
                onOpenDetails = {},
                onOpenSettings = {},
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            2 -> {
                val snapshot = when (state) {
                    is AqiUiState.Ready -> state.snapshot
                    is AqiUiState.Error -> state.cached
                    AqiUiState.Loading -> null
                }
                if (snapshot != null) {
                    DetailsScreen(snapshot = snapshot, modifier = Modifier.fillMaxSize())
                } else {
                    EmptyPage("No data yet")
                }
            }

            3 -> SettingsScreen(
                locationMode = locationMode,
                refreshMinutes = refreshMinutes,
                hasLocationPermission = hasLocationPermission,
                manualCity = manualCity,
                onLocationModeChange = { onCycleLocation() },
                onRefreshChange = { onCycleRefresh() },
                onManualCitySelected = onManualCity,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EmptyPage(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
