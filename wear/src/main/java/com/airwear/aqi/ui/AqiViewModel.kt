package com.airwear.aqi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airwear.aqi.data.AqiRepository
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.ui.cities.RankingUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AqiViewModel(
    private val repository: AqiRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null
    private var settingsRefreshJob: Job? = null

    private val rankRefreshing = MutableStateFlow(false)
    private val rankData = MutableStateFlow<List<CityRankEntry>>(emptyList())
    private val rankUpdatedAt = MutableStateFlow(0L)
    private val rankError = MutableStateFlow<String?>(null)
    private var rankStarted = false

    val rankingState: StateFlow<RankingUiState> = combine(
        rankData,
        rankRefreshing,
        rankError,
        rankUpdatedAt,
    ) { list, loading, err, updatedAt ->
        when {
            err != null && list.isEmpty() -> RankingUiState.Error(err)
            list.isEmpty() -> RankingUiState.Loading
            else -> RankingUiState.Ready(list, loading, updatedAt, repository.watchCityCount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState.Loading)

    /** Drives the theme; changes only when the AQI band changes. */
    val category: StateFlow<AqiCategory> = repository.cachedSnapshot
        .map { it?.category ?: AqiCategory.UNKNOWN }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AqiCategory.UNKNOWN)

    val locationMode: StateFlow<String> = repository.locationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "GPS")

    val manualCityName: StateFlow<String> = repository.manualCity
        .map { it?.city ?: "Jakarta" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Jakarta")

    val refreshMinutes: StateFlow<Int> = repository.refreshMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    val uiState: StateFlow<AqiUiState> = combine(
        repository.cachedSnapshot,
        refreshing,
        error,
    ) { cached, isRefreshing, err ->
        when {
            cached != null -> AqiUiState.Ready(
                snapshot = cached,
                isRefreshing = isRefreshing,
                softError = err,
                isStale = cached.isStale(),
            )
            err != null -> AqiUiState.Error(err)
            else -> AqiUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AqiUiState.Loading)

    init {
        refresh(force = false)
    }

    /**
     * [force] = user explicitly asked (bypass cache TTL). A non-forced refresh
     * is a no-op while another one is running; the repository serves the cache
     * when the last download is still fresh.
     */
    fun refresh(force: Boolean = true) {
        if (!force && refreshJob?.isActive == true) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            repository.refresh(force = force)
                .onSuccess { error.value = null }
                .onFailure { e -> error.value = e.message ?: "Couldn't load AQI" }
            refreshing.value = false
        }
    }

    /**
     * Settings changes are debounced so cycling GPS → IP → Manual only
     * fetches for the option the user stops on.
     */
    private fun refreshAfterSettingsChange() {
        settingsRefreshJob?.cancel()
        settingsRefreshJob = viewModelScope.launch {
            delay(SETTINGS_DEBOUNCE_MS)
            refresh(force = false) // cache freshness was invalidated by the change
        }
    }

    fun setLocationMode(mode: String) {
        viewModelScope.launch {
            repository.setLocationMode(mode)
            refreshAfterSettingsChange()
        }
    }

    fun setManualCity(city: String, state: String, country: String) {
        viewModelScope.launch {
            repository.setManualCity(city, state, country)
            repository.setLocationMode("MANUAL")
            refreshAfterSettingsChange()
        }
    }

    fun setRefreshMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setRefreshMinutes(minutes)
        }
    }

    /** Called when Ranking page is shown. Loads cache then background-refreshes. */
    fun onRankingVisible() {
        if (rankStarted) return
        rankStarted = true
        viewModelScope.launch {
            rankRefreshing.value = true
            rankError.value = null

            repository.loadCityRank(force = false) { partial ->
                if (partial.entries.isNotEmpty()) {
                    rankData.value = partial.entries
                    rankUpdatedAt.value = partial.updatedAtMs
                }
            }.onSuccess { result ->
                rankData.value = result.entries
                rankUpdatedAt.value = result.updatedAtMs
                rankError.value = null
            }.onFailure { e ->
                if (rankData.value.isEmpty()) {
                    rankError.value = e.message ?: "Couldn't load cities"
                }
            }
            rankRefreshing.value = false
        }
    }

    fun hasLocationPermission(): Boolean = repository.hasLocationPermission()

    companion object {
        private const val SETTINGS_DEBOUNCE_MS = 1_500L

        fun factory(repository: AqiRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AqiViewModel(repository) as T
            }
        }
    }
}
