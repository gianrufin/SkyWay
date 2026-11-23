package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.Formatters
import com.example.model.MapStyle
import com.example.model.PlacePoint
import com.example.model.RouteLoadState
import com.example.model.SearchTarget
import com.example.model.WeatherCondition
import com.example.ui.components.AnimatedWeatherEmblem
import com.example.ui.components.FloatingRouteSearchBar
import com.example.ui.components.GlassInstrumentPanel
import com.example.ui.components.GlassSearchOverlay
import com.example.ui.components.InstrumentSettingsOverlay
import com.example.ui.components.OsmMapCanvas
import com.example.ui.components.RouteTimelineConsole
import com.example.ui.components.SavedTripsOverlay
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.TactileGlassChip
import com.example.ui.components.TactileGlassOrb
import com.example.ui.theme.AmberGlare
import com.example.ui.theme.CobaltRain
import com.example.ui.theme.CyanClear
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SkySlateDeep
import com.example.ui.theme.SolarCoral
import com.example.ui.theme.TelemetryNumeralStyle
import kotlinx.coroutines.delay

@Composable
fun RouteWeatherScreen(
    viewModel: RouteWeatherViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDeniedHint by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            permissionDeniedHint = null
            viewModel.resolveDeviceLocationAsOrigin()
        } else {
            permissionDeniedHint = "Location permission denied. Search origin city manually."
        }
    }

    val requestLocationAndResolve = {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.resolveDeviceLocationAsOrigin()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(state.locationStatusMessage, permissionDeniedHint) {
        if (state.locationStatusMessage != null || permissionDeniedHint != null) {
            delay(3500L)
            viewModel.clearLocationStatusMessage()
            permissionDeniedHint = null
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitScreen = maxWidth >= 720.dp

        // Base Map Layer (always full-bleed edge-to-edge)
        OsmMapCanvas(
            evaluatedRoute = state.evaluatedRoute,
            selectedCheckpointIndex = state.selectedCheckpointIndex,
            unitSystem = state.preferences.unitSystem,
            mapStyle = state.preferences.mapStyle,
            activeRadarFrame = state.activeRadarFrame,
            recenterNonce = state.recenterNonce,
            onCheckpointClick = { idx ->
                viewModel.selectCheckpoint(idx)
                if (!state.isTimelineExpanded && !isSplitScreen) {
                    viewModel.setTimelineExpanded(true)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isSplitScreen) {
            // Adaptive Tablet / Landscape Split Cockpit
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 340.dp, max = 420.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FloatingRouteSearchBar(
                        origin = state.origin,
                        destination = state.destination,
                        isLocating = state.isLocatingUser,
                        onOriginClick = { viewModel.openSearch(SearchTarget.ORIGIN) },
                        onDestinationClick = { viewModel.openSearch(SearchTarget.DESTINATION) },
                        onSwapClick = viewModel::swapEndpoints,
                        onUseLocationClick = requestLocationAndResolve,
                    )

                    StatusFeedbackSection(
                        loadState = state.loadState,
                        noticeMessage = permissionDeniedHint ?: state.locationStatusMessage,
                        onRetry = viewModel::retryRouteCalculation,
                    )

                    val evalRoute = state.evaluatedRoute
                    if (evalRoute != null) {
                        RouteTimelineConsole(
                            evaluatedRoute = evalRoute,
                            departureSlots = state.departureSlots,
                            departureOffsetMinutes = state.departureOffsetMinutes,
                            baseNowEpochSeconds = state.baseNowEpochSeconds,
                            selectedCheckpointIndex = state.selectedCheckpointIndex,
                            isTimelineExpanded = true,
                            isRouteSaved = state.isCurrentRouteSaved,
                            unitSystem = state.preferences.unitSystem,
                            hapticsEnabled = state.preferences.hapticsEnabled,
                            isSplitScreen = true,
                            onOffsetChange = viewModel::setDepartureOffsetMinutes,
                            onJumpToBestWindow = viewModel::jumpToBestWindow,
                            onSelectCheckpoint = viewModel::selectCheckpoint,
                            onToggleExpanded = {},
                            onToggleBookmark = viewModel::toggleBookmarkCurrentRoute,
                        )
                    } else if (state.loadState is RouteLoadState.Idle) {
                        EmptyRouteInstrumentCard(
                            onSearchDestination = { viewModel.openSearch(SearchTarget.DESTINATION) },
                            onLaunchDemoCorridor = { o, d -> viewModel.launchQuickCorridor(o, d) },
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ExpressiveFloatingToolbar(
                        mapStyle = state.preferences.mapStyle,
                        radarEnabled = state.preferences.radarEnabled,
                        hasRoute = state.evaluatedRoute != null,
                        savedCount = state.savedRoutes.size,
                        onSelectMapStyle = viewModel::setMapStyle,
                        onToggleRadar = viewModel::toggleRadar,
                        onRecenter = viewModel::recenterMap,
                        onOpenSaved = { viewModel.setSavedTripsOpen(true) },
                        onOpenSettings = { viewModel.setSettingsOpen(true) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    RadarPlaybackPill(
                        state = state,
                        onTogglePlay = viewModel::toggleRadarPlayback,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        } else {
            // Compact Portrait Phone Layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FloatingRouteSearchBar(
                        origin = state.origin,
                        destination = state.destination,
                        isLocating = state.isLocatingUser,
                        onOriginClick = { viewModel.openSearch(SearchTarget.ORIGIN) },
                        onDestinationClick = { viewModel.openSearch(SearchTarget.DESTINATION) },
                        onSwapClick = viewModel::swapEndpoints,
                        onUseLocationClick = requestLocationAndResolve,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (state.activeRadarFrame != null) {
                            RadarPlaybackPill(
                                state = state,
                                onTogglePlay = viewModel::toggleRadarPlayback,
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        ExpressiveFloatingToolbar(
                            mapStyle = state.preferences.mapStyle,
                            radarEnabled = state.preferences.radarEnabled,
                            hasRoute = state.evaluatedRoute != null,
                            savedCount = state.savedRoutes.size,
                            onSelectMapStyle = viewModel::setMapStyle,
                            onToggleRadar = viewModel::toggleRadar,
                            onRecenter = viewModel::recenterMap,
                            onOpenSaved = { viewModel.setSavedTripsOpen(true) },
                            onOpenSettings = { viewModel.setSettingsOpen(true) },
                        )
                    }

                    StatusFeedbackSection(
                        loadState = state.loadState,
                        noticeMessage = permissionDeniedHint ?: state.locationStatusMessage,
                        onRetry = viewModel::retryRouteCalculation,
                    )
                }

                // Bottom Docked Glass Console
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    val evalRoute = state.evaluatedRoute
                    if (evalRoute != null) {
                        RouteTimelineConsole(
                            evaluatedRoute = evalRoute,
                            departureSlots = state.departureSlots,
                            departureOffsetMinutes = state.departureOffsetMinutes,
                            baseNowEpochSeconds = state.baseNowEpochSeconds,
                            selectedCheckpointIndex = state.selectedCheckpointIndex,
                            isTimelineExpanded = state.isTimelineExpanded,
                            isRouteSaved = state.isCurrentRouteSaved,
                            unitSystem = state.preferences.unitSystem,
                            hapticsEnabled = state.preferences.hapticsEnabled,
                            isSplitScreen = false,
                            onOffsetChange = viewModel::setDepartureOffsetMinutes,
                            onJumpToBestWindow = viewModel::jumpToBestWindow,
                            onSelectCheckpoint = viewModel::selectCheckpoint,
                            onToggleExpanded = viewModel::toggleTimelineExpanded,
                            onToggleBookmark = viewModel::toggleBookmarkCurrentRoute,
                        )
                    } else if (state.loadState is RouteLoadState.Idle) {
                        EmptyRouteInstrumentCard(
                            onSearchDestination = { viewModel.openSearch(SearchTarget.DESTINATION) },
                            onLaunchDemoCorridor = { o, d -> viewModel.launchQuickCorridor(o, d) },
                        )
                    }
                }
            }
        }

        // Modal Overlays
        AnimatedVisibility(
            visible = state.isSearchSheetOpen,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            GlassSearchOverlay(
                target = state.searchTarget,
                query = state.searchQuery,
                results = state.searchResults,
                recentPlaces = state.recentPlaces,
                isSearching = state.isSearching,
                isLocating = state.isLocatingUser,
                onQueryChange = viewModel::onSearchQueryChanged,
                onPlaceSelected = viewModel::selectPlace,
                onUseCurrentLocation = requestLocationAndResolve,
                onQuickCorridor = viewModel::launchQuickCorridor,
                onClose = viewModel::closeSearch,
            )
        }

        AnimatedVisibility(
            visible = state.isSavedTripsOpen,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            SavedTripsOverlay(
                savedRoutes = state.savedRoutes,
                unitSystem = state.preferences.unitSystem,
                onSelectRoute = viewModel::loadSavedRoute,
                onDeleteRoute = viewModel::deleteSavedRoute,
                onClose = { viewModel.setSavedTripsOpen(false) },
            )
        }

        AnimatedVisibility(
            visible = state.isSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            InstrumentSettingsOverlay(
                unitSystem = state.preferences.unitSystem,
                mapStyle = state.preferences.mapStyle,
                hapticsEnabled = state.preferences.hapticsEnabled,
                onSelectUnitSystem = viewModel::setUnitSystem,
                onSelectMapStyle = viewModel::setMapStyle,
                onToggleHaptics = viewModel::setHapticsEnabled,
                onClose = { viewModel.setSettingsOpen(false) },
            )
        }

        if (state.isUpdateDialogVisible) {
            val update = state.updateInfo
            if (update != null) {
                AppUpdateDialog(
                    updateInfo = update,
                    onDismiss = viewModel::dismissUpdateDialog,
                )
            }
        }
    }
}

@Composable
private fun ExpressiveFloatingToolbar(
    mapStyle: MapStyle,
    radarEnabled: Boolean,
    hasRoute: Boolean,
    savedCount: Int,
    onSelectMapStyle: (MapStyle) -> Unit,
    onToggleRadar: () -> Unit,
    onRecenter: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMapStyleExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Expandable map styles chip row with M3 Expressive Spring animation
        AnimatedVisibility(
            visible = isMapStyleExpanded,
            enter = fadeIn(spring(dampingRatio = 0.72f, stiffness = 380f)) +
                slideInVertically(spring(dampingRatio = 0.72f, stiffness = 380f)) { -it / 2 },
            exit = fadeOut(spring(dampingRatio = 0.72f, stiffness = 380f)) +
                slideOutVertically(spring(dampingRatio = 0.72f, stiffness = 380f)) { -it / 2 },
        ) {
            GlassInstrumentPanel(
                cornerRadius = 20.dp,
                accentGlow = CyanClear,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapStyle.values().forEach { style ->
                        TactileGlassChip(
                            onClick = {
                                onSelectMapStyle(style)
                                isMapStyleExpanded = false
                            },
                            selected = mapStyle == style,
                            accentColor = CyanClear,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("quick_map_style_${style.name.lowercase()}"),
                        ) {
                            Text(
                                text = style.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = InkPrimary,
                            )
                        }
                    }
                }
            }
        }

        // Horizontal M3 Expressive Floating Toolbar Dock
        GlassInstrumentPanel(
            cornerRadius = 24.dp,
            elevated = true,
            accentGlow = if (radarEnabled) CobaltRain else null,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            modifier = Modifier.testTag("floating_cockpit_toolbar"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Map Style Switcher Chip
                TactileGlassChip(
                    onClick = { isMapStyleExpanded = !isMapStyleExpanded },
                    selected = isMapStyleExpanded,
                    accentColor = CyanClear,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("map_style_toolbar_button"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = stringResource(R.string.cd_select_map_style),
                            tint = CyanClear,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mapStyle.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                        )
                    }
                }

                // Doppler Radar Orb
                TactileGlassOrb(
                    onClick = onToggleRadar,
                    size = 40.dp,
                    active = radarEnabled,
                    activeColor = CobaltRain,
                    modifier = Modifier.testTag("radar_toggle_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Radar,
                        contentDescription = stringResource(R.string.cd_toggle_radar),
                        tint = if (radarEnabled) CyanClear else InkPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Recenter Orb (if route is active)
                if (hasRoute) {
                    TactileGlassOrb(
                        onClick = onRecenter,
                        size = 40.dp,
                        modifier = Modifier.testTag("recenter_route_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = stringResource(R.string.cd_recenter_route),
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Saved Trips Orb with Badge
                Box {
                    TactileGlassOrb(
                        onClick = onOpenSaved,
                        size = 40.dp,
                        active = savedCount > 0,
                        activeColor = AmberGlare,
                        modifier = Modifier.testTag("saved_trips_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = stringResource(R.string.cd_saved_trips),
                            tint = if (savedCount > 0) AmberGlare else InkPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (savedCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(AmberGlare),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = savedCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = SkySlateDeep,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Settings Orb
                TactileGlassOrb(
                    onClick = onOpenSettings,
                    size = 40.dp,
                    modifier = Modifier.testTag("settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = InkPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarPlaybackPill(
    state: RouteWeatherUiState,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeFrame = state.activeRadarFrame ?: return
    GlassInstrumentPanel(
        modifier = modifier,
        cornerRadius = 18.dp,
        accentGlow = CobaltRain,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TactileGlassOrb(
                onClick = onTogglePlay,
                size = 36.dp,
                active = state.isRadarPlaying,
                activeColor = CobaltRain,
            ) {
                Icon(
                    imageVector = if (state.isRadarPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isRadarPlaying) "Pause radar" else "Play radar",
                    tint = InkPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.radar_live_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanClear,
                )
                Text(
                    text = Formatters.formatLocalTime(activeFrame.timeEpoch),
                    style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                    color = InkPrimary,
                )
            }
        }
    }
}

@Composable
private fun StatusFeedbackSection(
    loadState: RouteLoadState,
    noticeMessage: String?,
    onRetry: () -> Unit,
) {
    if (loadState is RouteLoadState.Loading) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_glow_anim",
        )

        GlassInstrumentPanel(
            cornerRadius = 20.dp,
            accentGlow = CyanClear.copy(alpha = pulseAlpha),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.5.dp,
                    color = CyanClear,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SYNTHESIZING ROUTE WEATHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanClear,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = loadState.stepLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary,
                    )
                }
            }
        }
    } else if (loadState is RouteLoadState.Error) {
        GlassInstrumentPanel(
            cornerRadius = 18.dp,
            accentGlow = SolarCoral,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = loadState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                TactileGlassChip(
                    onClick = onRetry,
                    selected = true,
                    accentColor = SolarCoral,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.error_retry),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary,
                        )
                    }
                }
            }
        }
    }

    if (noticeMessage != null) {
        GlassInstrumentPanel(
            cornerRadius = 16.dp,
            accentGlow = AmberGlare,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = noticeMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
            )
        }
    }
}

@Composable
private fun EmptyRouteInstrumentCard(
    onSearchDestination: () -> Unit,
    onLaunchDemoCorridor: (PlacePoint, PlacePoint) -> Unit,
) {
    GlassInstrumentPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevated = true,
        accentGlow = CyanClear,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedWeatherEmblem(
                    condition = WeatherCondition.PARTLY_CLOUDY,
                    isDaylight = true,
                    size = 54.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.empty_route_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.empty_route_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TactileGlassChip(
                    onClick = onSearchDestination,
                    selected = true,
                    accentColor = CyanClear,
                    modifier = Modifier.weight(1f).testTag("empty_search_cta"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Search Route",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                        )
                    }
                }
                TactileGlassChip(
                    onClick = {
                        onLaunchDemoCorridor(
                            PlacePoint("San Francisco", "California, US", 37.7749, -122.4194),
                            PlacePoint("South Lake Tahoe", "California, US", 38.9399, -119.9772),
                        )
                    },
                    accentColor = AmberGlare,
                    modifier = Modifier.weight(1f).testTag("empty_sample_route_cta"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = null,
                            tint = AmberGlare,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SF → Tahoe",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                        )
                    }
                }
            }
        }
    }
}
