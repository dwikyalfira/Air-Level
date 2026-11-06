package com.airwear.aqi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.ui.AqiViewModel
import com.airwear.aqi.ui.cities.CitiesPage
import com.airwear.aqi.ui.details.DetailsPage
import com.airwear.aqi.ui.home.HomePage
import com.airwear.aqi.ui.settings.CityPickerScreen
import com.airwear.aqi.ui.settings.IntervalScreen
import com.airwear.aqi.ui.settings.LocationScreen
import com.airwear.aqi.ui.settings.SettingsScreen
import com.airwear.aqi.ui.theme.AirWearTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as AqiApp).repository
        setContent {
            val vm: AqiViewModel = viewModel(factory = AqiViewModel.factory(repository))
            // Theme depends on the AQI band only — not on every state change.
            val category by vm.category.collectAsStateWithLifecycle()
            AirWearTheme(category = category) {
                AppScaffold {
                    AirWearNavHost(vm)
                }
            }
        }
    }
}

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val LOCATION = "settings/location"
    const val CITY = "settings/city"
    const val INTERVAL = "settings/interval"
}

@Composable
private fun AirWearNavHost(vm: AqiViewModel) {
    val nav = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainPager(vm = vm, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            val mode by vm.locationMode.collectAsStateWithLifecycle()
            val city by vm.manualCityName.collectAsStateWithLifecycle()
            val minutes by vm.refreshMinutes.collectAsStateWithLifecycle()
            SettingsScreen(
                locationMode = mode,
                manualCity = city,
                refreshMinutes = minutes,
                onOpenLocation = { nav.navigate(Routes.LOCATION) },
                onOpenInterval = { nav.navigate(Routes.INTERVAL) },
            )
        }
        composable(Routes.LOCATION) {
            val mode by vm.locationMode.collectAsStateWithLifecycle()
            LocationScreen(
                locationMode = mode,
                hasLocationPermission = vm::hasLocationPermission,
                onSelect = { selected ->
                    vm.setLocationMode(selected)
                    nav.popBackStack()
                },
                onChooseCity = { nav.navigate(Routes.CITY) },
            )
        }
        composable(Routes.CITY) {
            val mode by vm.locationMode.collectAsStateWithLifecycle()
            val city by vm.manualCityName.collectAsStateWithLifecycle()
            CityPickerScreen(
                selectedCity = city.takeIf { mode == "MANUAL" },
                onSelect = { spec ->
                    vm.setManualCity(spec.city, spec.state, spec.country)
                    nav.popBackStack(Routes.SETTINGS, inclusive = false)
                },
            )
        }
        composable(Routes.INTERVAL) {
            val minutes by vm.refreshMinutes.collectAsStateWithLifecycle()
            IntervalScreen(
                refreshMinutes = minutes,
                onSelect = {
                    vm.setRefreshMinutes(it)
                    nav.popBackStack()
                },
            )
        }
    }
}

// Home sits in the middle: World cities on the left, Details on the right.
private const val PAGE_CITIES = 0
private const val PAGE_HOME = 1
private const val PAGE_DETAILS = 2

/** Cities · Home · Details (starts on Home). Each page collects only the state it renders. */
@Composable
private fun MainPager(vm: AqiViewModel, onOpenSettings: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = PAGE_HOME, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page == PAGE_CITIES) vm.onRankingVisible()
        }
    }

    HorizontalPagerScaffold(pagerState = pagerState) {
        // Pre-compose neighbours so swipes don't pay first-composition cost mid-gesture.
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                when (page) {
                    PAGE_HOME -> {
                        val state by vm.uiState.collectAsStateWithLifecycle()
                        val mode by vm.locationMode.collectAsStateWithLifecycle()
                        HomePage(
                            state = state,
                            isManualLocation = mode == "MANUAL",
                            onRefresh = { vm.refresh(force = true) },
                            onOpenDetails = { scope.launch { pagerState.animateScrollToPage(PAGE_DETAILS) } },
                        )
                    }

                    PAGE_DETAILS -> {
                        val state by vm.uiState.collectAsStateWithLifecycle()
                        val minutes by vm.refreshMinutes.collectAsStateWithLifecycle()
                        DetailsPage(
                            snapshot = (state as? AqiUiState.Ready)?.snapshot,
                            refreshMinutes = minutes,
                            onOpenSettings = onOpenSettings,
                        )
                    }

                    PAGE_CITIES -> {
                        val ranking by vm.rankingState.collectAsStateWithLifecycle()
                        val state by vm.uiState.collectAsStateWithLifecycle()
                        CitiesPage(
                            state = ranking,
                            mine = (state as? AqiUiState.Ready)?.snapshot,
                        )
                    }
                }
            }
        }
    }
}
