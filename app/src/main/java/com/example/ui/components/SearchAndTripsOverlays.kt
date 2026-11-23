package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.Formatters
import com.example.model.PlacePoint
import com.example.model.SavedRouteItem
import com.example.model.SearchTarget
import com.example.model.UnitSystem
import com.example.model.MapStyle
import com.example.ui.theme.AmberGlare
import com.example.ui.theme.CyanClear
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SkySlateDeep
import com.example.ui.theme.SolarCoral

@Composable
fun FloatingRouteSearchBar(
    origin: PlacePoint?,
    destination: PlacePoint?,
    isLocating: Boolean,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onSwapClick: () -> Unit,
    onUseLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassInstrumentPanel(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecessedInstrumentWell(
                    cornerRadius = 12.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOriginClick)
                        .testTag("origin_search_pill"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldOptimal)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = origin?.name ?: stringResource(R.string.search_origin_hint),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (origin != null) InkPrimary else InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                RecessedInstrumentWell(
                    cornerRadius = 12.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDestinationClick)
                        .testTag("destination_search_pill"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CyanClear,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = destination?.name ?: stringResource(R.string.search_destination_hint),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (destination != null) InkPrimary else InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TactileGlassOrb(
                    onClick = onUseLocationClick,
                    size = 48.dp,
                    active = isLocating,
                    modifier = Modifier.testTag("gps_origin_button"),
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyanClear,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = stringResource(R.string.cd_use_current_location),
                            tint = CyanClear,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                TactileGlassOrb(
                    onClick = onSwapClick,
                    size = 48.dp,
                    modifier = Modifier.testTag("swap_endpoints_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.cd_swap_endpoints),
                        tint = InkSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun GlassSearchOverlay(
    target: SearchTarget,
    query: String,
    results: List<PlacePoint>,
    recentPlaces: List<PlacePoint>,
    isSearching: Boolean,
    isLocating: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlacePoint) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onQuickCorridor: (PlacePoint, PlacePoint) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkySlateDeep.copy(alpha = 0.88f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        GlassInstrumentPanel(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 26.dp,
            elevated = true,
            accentGlow = CyanClear,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (target == SearchTarget.ORIGIN) "Select Starting Point" else "Search Destination",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary,
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_search_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = InkSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                RecessedInstrumentWell(
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CyanClear,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "City, town, or landmark…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = InkMuted,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkPrimary),
                                cursorBrush = SolidColor(CyanClear),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("place_search_input"),
                            )
                        }
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanClear,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (target == SearchTarget.ORIGIN) {
                    TactileGlassChip(
                        onClick = onUseCurrentLocation,
                        selected = true,
                        accentColor = EmeraldOptimal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_sheet_use_gps"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldOptimal,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    tint = EmeraldOptimal,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use Current GPS Location",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (results.isNotEmpty()) {
                        item {
                            Text(
                                text = "MATCHING PLACES",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanClear,
                            )
                        }
                        items(results) { place ->
                            PlaceResultRow(
                                place = place,
                                isRecent = false,
                                onClick = { onPlaceSelected(place) },
                            )
                        }
                    } else if (query.trim().length >= 2 && !isSearching) {
                        item {
                            Text(
                                text = "No matching locations found. Try a nearby city.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }

                    if (recentPlaces.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "RECENT WAYPOINTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted,
                            )
                        }
                        items(recentPlaces) { place ->
                            PlaceResultRow(
                                place = place,
                                isRecent = true,
                                onClick = { onPlaceSelected(place) },
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "POPULAR WEATHER CORRIDORS",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGlare,
                        )
                    }
                    items(featuredCorridors) { (orig, dest, tagline) ->
                        GlassInstrumentPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onQuickCorridor(orig, dest) },
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Explore,
                                        contentDescription = null,
                                        tint = AmberGlare,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${orig.name} → ${dest.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InkPrimary,
                                        )
                                        Text(
                                            text = tagline,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InkSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(
    place: PlacePoint,
    isRecent: Boolean,
    onClick: () -> Unit,
) {
    RecessedInstrumentWell(
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isRecent) Icons.Rounded.History else Icons.Rounded.Place,
                contentDescription = null,
                tint = if (isRecent) InkSecondary else CyanClear,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                )
                if (place.subtitle.isNotBlank()) {
                    Text(
                        text = place.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                    )
                }
            }
        }
    }
}

@Composable
fun SavedTripsOverlay(
    savedRoutes: List<SavedRouteItem>,
    unitSystem: UnitSystem,
    onSelectRoute: (SavedRouteItem) -> Unit,
    onDeleteRoute: (Long) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkySlateDeep.copy(alpha = 0.88f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        GlassInstrumentPanel(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 26.dp,
            elevated = true,
            accentGlow = AmberGlare,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Saved Routes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary,
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = InkSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (savedRoutes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(44.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Saved Routes Yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chart a route and tap the bookmark icon to keep it ready on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(savedRoutes, key = { it.id }) { item ->
                            RecessedInstrumentWell(
                                cornerRadius = 16.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectRoute(item) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.origin.name} → ${item.destination.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InkPrimary,
                                        )
                                        Text(
                                            text = "${Formatters.formatDuration(item.durationSeconds)} · " +
                                                Formatters.formatDistance(item.distanceMeters, unitSystem),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InkSecondary,
                                        )
                                    }
                                    IconButton(onClick = { onDeleteRoute(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete saved route",
                                            tint = SolarCoral,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstrumentSettingsOverlay(
    unitSystem: UnitSystem,
    mapStyle: MapStyle,
    hapticsEnabled: Boolean,
    onSelectUnitSystem: (UnitSystem) -> Unit,
    onSelectMapStyle: (MapStyle) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkySlateDeep.copy(alpha = 0.85f))
            .clickable(onClick = onClose)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassInstrumentPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {}),
            cornerRadius = 24.dp,
            elevated = true,
            accentGlow = CyanClear,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Instrument Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary,
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = InkSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MEASUREMENT UNITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanClear,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TactileGlassChip(
                        onClick = { onSelectUnitSystem(UnitSystem.IMPERIAL) },
                        selected = unitSystem == UnitSystem.IMPERIAL,
                        modifier = Modifier.weight(1f).testTag("unit_imperial_chip"),
                    ) {
                        Text(
                            text = "Imperial (°F · mph)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                        )
                    }
                    TactileGlassChip(
                        onClick = { onSelectUnitSystem(UnitSystem.METRIC) },
                        selected = unitSystem == UnitSystem.METRIC,
                        modifier = Modifier.weight(1f).testTag("unit_metric_chip"),
                    ) {
                        Text(
                            text = "Metric (°C · km/h)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "BASE MAP INSTRUMENTATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanClear,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MapStyle.values().forEach { style ->
                        TactileGlassChip(
                            onClick = { onSelectMapStyle(style) },
                            selected = mapStyle == style,
                            accentColor = CyanClear,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).testTag("map_style_${style.name.lowercase()}"),
                        ) {
                            Text(
                                text = style.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = InkPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                RecessedInstrumentWell(
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scrubber Haptic Detents",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                            )
                            Text(
                                text = "Tactile click every 15-min departure step",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                            )
                        }
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanClear,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private val featuredCorridors = listOf(
    Triple(
        PlacePoint("San Francisco", "California, US", 37.7749, -122.4194),
        PlacePoint("South Lake Tahoe", "California, US", 38.9399, -119.9772),
        "Coastal fog to Sierra alpine pass",
    ),
    Triple(
        PlacePoint("Seattle", "Washington, US", 47.6062, -122.3321),
        PlacePoint("Portland", "Oregon, US", 45.5152, -122.6784),
        "Pacific Northwest I-5 corridor",
    ),
    Triple(
        PlacePoint("Denver", "Colorado, US", 39.7392, -104.9903),
        PlacePoint("Aspen", "Colorado, US", 39.1911, -106.8175),
        "High-elevation Rocky Mountain transit",
    ),
)
