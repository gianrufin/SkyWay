package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocationHelper
import com.example.data.local.SkyWayDatabase
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.RouteWeatherRepository
import com.example.domain.RouteEvaluator
import com.example.model.DepartureWindowSlot
import com.example.model.EvaluatedRoute
import com.example.model.PlacePoint
import com.example.model.RadarFrame
import com.example.model.RouteBundle
import com.example.model.RouteLoadState
import com.example.model.SavedRouteItem
import com.example.model.SearchTarget
import com.example.model.UnitSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class RouteWeatherUiState(
    val origin: PlacePoint? = null,
    val destination: PlacePoint? = null,
    val loadState: RouteLoadState = RouteLoadState.Idle,
    val evaluatedRoute: EvaluatedRoute? = null,
    val departureSlots: List<DepartureWindowSlot> = emptyList(),
    val departureOffsetMinutes: Int = 0,
    val baseNowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    val selectedCheckpointIndex: Int? = null,
    val isTimelineExpanded: Boolean = false,
    val isSearchSheetOpen: Boolean = false,
    val searchTarget: SearchTarget = SearchTarget.DESTINATION,
    val searchQuery: String = "",
    val searchResults: List<PlacePoint> = emptyList(),
    val isSearching: Boolean = false,
    val isSavedTripsOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isLocatingUser: Boolean = false,
    val locationStatusMessage: String? = null,
    val radarFrames: List<RadarFrame> = emptyList(),
    val activeRadarIndex: Int = 0,
    val isRadarPlaying: Boolean = true,
    val recenterNonce: Int = 0,
    val savedRoutes: List<SavedRouteItem> = emptyList(),
    val recentPlaces: List<PlacePoint> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
) {
    val activeRadarFrame: RadarFrame?
        get() = if (preferences.radarEnabled && radarFrames.isNotEmpty()) {
            radarFrames[activeRadarIndex.coerceIn(0, radarFrames.lastIndex)]
        } else {
            null
        }

    val isCurrentRouteSaved: Boolean
        get() {
            val o = origin ?: return false
            val d = destination ?: return false
            return savedRoutes.any {
                it.origin.name == o.name && it.destination.name == d.name
            }
        }
}

class RouteWeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SkyWayDatabase.getInstance(application)
    private val repository = RouteWeatherRepository(database.skyWayDao())
    private val prefsRepo = UserPreferencesRepository(application)

    private val rawBundleFlow = MutableStateFlow<RouteBundle?>(null)
    private val internalState = MutableStateFlow(RouteWeatherUiState())

    private var searchJob: Job? = null
    private var routeJob: Job? = null
    private var radarTickerJob: Job? = null

    val uiState: StateFlow<RouteWeatherUiState> = combine(
        internalState,
        repository.savedRoutesFlow,
        repository.recentPlacesFlow,
        prefsRepo.preferencesFlow,
    ) { state, saved, recent, prefs ->
        state.copy(
            savedRoutes = saved,
            recentPlaces = recent,
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RouteWeatherUiState(),
    )

    init {
        // Default origin ready so searching a destination immediately charts a route
        val defaultOrigin = PlacePoint(
            name = "San Francisco",
            subtitle = "California, United States",
            latitude = 37.7749,
            longitude = -122.4194,
        )
        internalState.update { it.copy(origin = defaultOrigin) }
    }

    fun openSearch(target: SearchTarget) {
        internalState.update {
            it.copy(
                isSearchSheetOpen = true,
                searchTarget = target,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
            )
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        internalState.update { it.copy(isSearchSheetOpen = false, isSearching = false) }
    }

    fun onSearchQueryChanged(query: String) {
        internalState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            internalState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            internalState.update { it.copy(isSearching = true) }
            delay(280L)
            val results = repository.searchPlaces(query)
            internalState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun selectPlace(place: PlacePoint) {
        val target = internalState.value.searchTarget
        viewModelScope.launch { repository.saveRecentPlace(place) }

        internalState.update { state ->
            when (target) {
                SearchTarget.ORIGIN -> state.copy(
                    origin = place,
                    isSearchSheetOpen = false,
                )
                SearchTarget.DESTINATION -> state.copy(
                    destination = place,
                    isSearchSheetOpen = false,
                )
            }
        }
        triggerRouteCalculationIfReady()
    }

    fun launchQuickCorridor(origin: PlacePoint, destination: PlacePoint) {
        internalState.update {
            it.copy(
                origin = origin,
                destination = destination,
                isSearchSheetOpen = false,
                isSavedTripsOpen = false,
            )
        }
        triggerRouteCalculationIfReady()
    }

    fun swapEndpoints() {
        val o = internalState.value.origin
        val d = internalState.value.destination
        if (o == null && d == null) return
        internalState.update {
            it.copy(origin = d, destination = o)
        }
        triggerRouteCalculationIfReady()
    }

    fun resolveDeviceLocationAsOrigin() {
        viewModelScope.launch {
            internalState.update { it.copy(isLocatingUser = true, locationStatusMessage = null) }
            val coords = LocationHelper.getCurrentCoordinates(getApplication())
            if (coords != null) {
                val place = repository.reverseGeocode(coords.latitude, coords.longitude)
                repository.saveRecentPlace(place)
                internalState.update {
                    it.copy(
                        origin = place,
                        isLocatingUser = false,
                        isSearchSheetOpen = false,
                        locationStatusMessage = "Origin set to ${place.name}",
                    )
                }
                triggerRouteCalculationIfReady()
            } else {
                internalState.update {
                    it.copy(
                        isLocatingUser = false,
                        locationStatusMessage = "Unable to acquire GPS fix. Search manually or retry.",
                    )
                }
            }
        }
    }

    fun clearLocationStatusMessage() {
        internalState.update { it.copy(locationStatusMessage = null) }
    }

    fun retryRouteCalculation() {
        triggerRouteCalculationIfReady()
    }

    private fun triggerRouteCalculationIfReady() {
        val origin = internalState.value.origin ?: return
        val destination = internalState.value.destination ?: return

        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            val nowEpoch = System.currentTimeMillis() / 1000L
            internalState.update {
                it.copy(
                    loadState = RouteLoadState.Loading("Charting driving corridor & hourly telemetry…"),
                    baseNowEpochSeconds = nowEpoch,
                    departureOffsetMinutes = 0,
                    selectedCheckpointIndex = null,
                )
            }
            try {
                val bundle = repository.computeRouteAndWeather(origin, destination)
                rawBundleFlow.value = bundle
                val evaluated = RouteEvaluator.evaluateRoute(
                    bundle = bundle,
                    departureEpochSeconds = nowEpoch,
                    baseNowEpochSeconds = nowEpoch,
                )
                val slots = RouteEvaluator.computeDepartureSlots(
                    bundle = bundle,
                    baseNowEpochSeconds = nowEpoch,
                )
                internalState.update {
                    it.copy(
                        loadState = RouteLoadState.Ready,
                        evaluatedRoute = evaluated,
                        departureSlots = slots,
                        selectedCheckpointIndex = 0,
                        recenterNonce = it.recenterNonce + 1,
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(
                        loadState = RouteLoadState.Error(
                            e.message ?: "Failed to fetch route or live forecast."
                        )
                    )
                }
            }
        }
    }

    fun setDepartureOffsetMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 6 * 24 * 60)
        val bundle = rawBundleFlow.value ?: return
        val baseNow = internalState.value.baseNowEpochSeconds
        val depEpoch = baseNow + clamped * 60L
        val evaluated = RouteEvaluator.evaluateRoute(
            bundle = bundle,
            departureEpochSeconds = depEpoch,
            baseNowEpochSeconds = baseNow,
        )
        internalState.update {
            it.copy(
                departureOffsetMinutes = clamped,
                evaluatedRoute = evaluated,
            )
        }
    }

    fun jumpToBestWindow() {
        val best = internalState.value.evaluatedRoute?.bestWindowOffsetMinutes ?: return
        setDepartureOffsetMinutes(best)
    }

    fun selectCheckpoint(index: Int?) {
        internalState.update { it.copy(selectedCheckpointIndex = index) }
    }

    fun setTimelineExpanded(expanded: Boolean) {
        internalState.update { it.copy(isTimelineExpanded = expanded) }
    }

    fun toggleTimelineExpanded() {
        internalState.update { it.copy(isTimelineExpanded = !it.isTimelineExpanded) }
    }

    fun recenterMap() {
        internalState.update { it.copy(recenterNonce = it.recenterNonce + 1) }
    }

    fun toggleRadar() {
        val nextEnabled = !uiState.value.preferences.radarEnabled
        viewModelScope.launch {
            prefsRepo.setRadarEnabled(nextEnabled)
            if (nextEnabled) {
                if (internalState.value.radarFrames.isEmpty()) {
                    runCatching { repository.fetchRadarFrames() }
                        .onSuccess { frames ->
                            internalState.update {
                                it.copy(
                                    radarFrames = frames,
                                    activeRadarIndex = (frames.size - 1).coerceAtLeast(0),
                                )
                            }
                        }
                }
                startRadarPlaybackLoop()
            } else {
                radarTickerJob?.cancel()
            }
        }
    }

    fun toggleRadarPlayback() {
        val nextPlaying = !internalState.value.isRadarPlaying
        internalState.update { it.copy(isRadarPlaying = nextPlaying) }
        if (nextPlaying && uiState.value.preferences.radarEnabled) {
            startRadarPlaybackLoop()
        } else {
            radarTickerJob?.cancel()
        }
    }

    private fun startRadarPlaybackLoop() {
        radarTickerJob?.cancel()
        radarTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1100L)
                val frames = internalState.value.radarFrames
                if (frames.size > 1 && internalState.value.isRadarPlaying) {
                    internalState.update {
                        it.copy(activeRadarIndex = (it.activeRadarIndex + 1) % frames.size)
                    }
                }
            }
        }
    }

    fun toggleBookmarkCurrentRoute() {
        val bundle = rawBundleFlow.value ?: return
        val currentlySaved = uiState.value.isCurrentRouteSaved
        viewModelScope.launch {
            if (currentlySaved) {
                repository.deleteSavedRouteByEndpoints(bundle.origin.name, bundle.destination.name)
            } else {
                repository.saveRoute(bundle)
            }
        }
    }

    fun loadSavedRoute(item: SavedRouteItem) {
        launchQuickCorridor(item.origin, item.destination)
    }

    fun deleteSavedRoute(id: Long) {
        viewModelScope.launch { repository.deleteSavedRoute(id) }
    }

    fun setSavedTripsOpen(open: Boolean) {
        internalState.update { it.copy(isSavedTripsOpen = open) }
    }

    fun setSettingsOpen(open: Boolean) {
        internalState.update { it.copy(isSettingsOpen = open) }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { prefsRepo.setUnitSystem(unitSystem) }
    }

    fun setMapStyle(mapStyle: com.example.model.MapStyle) {
        viewModelScope.launch { prefsRepo.setMapStyle(mapStyle) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    }
}
