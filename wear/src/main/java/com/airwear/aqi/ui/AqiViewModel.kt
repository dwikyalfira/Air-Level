package com.airwear.aqi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airwear.aqi.data.AqiRepository
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiUiState
import com.airwear.aqi.domain.CityRankEntry
import com.airwear.aqi.ui.ranking.RankingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AqiViewModel(
    private val repository: AqiRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private var lastRefreshMs = 0L

    private val rankRefreshing = MutableStateFlow(false)
    private val rankData = MutableStateFlow<List<CityRankEntry>>(emptyList())
    private val rankError = MutableStateFlow<String?>(null)
    private var rankLoadedAt = 0L
    private var rankStarted = false

    val rankingState: StateFlow<RankingUiState> = combine(
        rankData,
        rankRefreshing,
        rankError,
    ) { list, loading, err ->
        when {
            err != null && list.isEmpty() -> RankingUiState.Error(err)
            list.isEmpty() -> RankingUiState.Loading
            else -> RankingUiState.Ready(list, loading, rankLoadedAt)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingUiState.Loading)

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
            // If we have data, keep showing it — errors (429) are soft
            cached != null -> AqiUiState.Ready(cached, fromCache = true)
            err != null -> AqiUiState.Error(err)
            isRefreshing -> AqiUiState.Loading
            else -> AqiUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AqiUiState.Loading)

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshMs < 60_000L) return@launch
            lastRefreshMs = now
            refreshing.value = true
            error.value = null
            repository.refresh()
                .onSuccess { error.value = null }
                .onFailure { e ->
                    // Don't surface 429 when cache already exists
                    error.value = e.message
                }
            refreshing.value = false
        }
    }

    fun setLocationMode(mode: String) {
        viewModelScope.launch {
            repository.setLocationMode(mode)
            refresh(force = true)
        }
    }

    fun setManualCity(city: String, state: String, country: String) {
        viewModelScope.launch {
            repository.setManualCity(city, state, country)
            repository.setLocationMode("MANUAL")
            refresh(force = true)
        }
    }

    fun cycleLocationMode() {
        val current = locationMode.value
        val next = when (current) {
            "GPS" -> "IP"
            "IP" -> "MANUAL"
            else -> "GPS"
        }
        setLocationMode(next)
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

            // Cache first (instant), then keep fetching remaining cities
            repository.loadCityRank(force = false) { partial ->
                if (partial.isNotEmpty()) {
                    rankData.value = partial
                    rankLoadedAt = System.currentTimeMillis()
                }
            }.onSuccess { list ->
                rankData.value = list
                rankLoadedAt = System.currentTimeMillis()
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

    fun currentAccent(): AqiCategory {
        val state = uiState.value
        return when (state) {
            is AqiUiState.Ready -> state.snapshot.category
            is AqiUiState.Error -> state.cached?.category ?: AqiCategory.UNKNOWN
            AqiUiState.Loading -> AqiCategory.UNKNOWN
        }
    }

    companion object {
        fun factory(repository: AqiRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AqiViewModel(repository) as T
            }
        }
    }
}
