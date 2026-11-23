package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Formatters
import com.example.model.DepartureWindowSlot
import com.example.model.EvaluatedCheckpoint
import com.example.model.EvaluatedRoute
import com.example.model.GlareRisk
import com.example.model.SolarBoundaryMarker
import com.example.model.SolarEventType
import com.example.model.UnitSystem
import com.example.model.WeatherSeverity
import com.example.ui.theme.AmberGlare
import com.example.ui.theme.CobaltRain
import com.example.ui.theme.CyanClear
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SolarCoral
import com.example.ui.theme.TelemetryNumeralStyle

@Composable
fun RouteTimelineConsole(
    evaluatedRoute: EvaluatedRoute,
    departureSlots: List<DepartureWindowSlot>,
    departureOffsetMinutes: Int,
    baseNowEpochSeconds: Long,
    selectedCheckpointIndex: Int?,
    isTimelineExpanded: Boolean,
    isRouteSaved: Boolean,
    unitSystem: UnitSystem,
    hapticsEnabled: Boolean,
    isSplitScreen: Boolean,
    onOffsetChange: (Int) -> Unit,
    onJumpToBestWindow: () -> Unit,
    onSelectCheckpoint: (Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentGlow = when (evaluatedRoute.overallSeverity) {
        WeatherSeverity.CLEAR -> CyanClear
        WeatherSeverity.CAUTION -> AmberGlare
        WeatherSeverity.HAZARD -> SolarCoral
    }
    val originOffset = evaluatedRoute.checkpoints.firstOrNull()?.utcOffsetSeconds
    val destOffset = evaluatedRoute.checkpoints.lastOrNull()?.utcOffsetSeconds
    val listState = rememberLazyListState()

    LaunchedEffect(selectedCheckpointIndex, isTimelineExpanded) {
        val idx = selectedCheckpointIndex ?: return@LaunchedEffect
        if (isTimelineExpanded || isSplitScreen) {
            listState.animateScrollToItem(idx.coerceIn(0, (evaluatedRoute.checkpoints.size - 1).coerceAtLeast(0)))
        }
    }

    GlassInstrumentPanel(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.82f, stiffness = 340f)),
        cornerRadius = 26.dp,
        elevated = true,
        accentGlow = accentGlow,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header & Expand/Collapse Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(accentGlow)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${evaluatedRoute.bundle.origin.name} → ${evaluatedRoute.bundle.destination.name}",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${Formatters.formatDuration(evaluatedRoute.bundle.totalDurationSeconds)} · " +
                            "${Formatters.formatDistance(evaluatedRoute.bundle.totalDistanceMeters, unitSystem)} · " +
                            "Arrive ${Formatters.formatLocalTime(evaluatedRoute.arrivalEpochSeconds, destOffset)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("bookmark_route_button"),
                    ) {
                        Icon(
                            imageVector = if (isRouteSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Save route",
                            tint = if (isRouteSaved) AmberGlare else InkSecondary,
                        )
                    }

                    if (!isSplitScreen) {
                        TactileGlassOrb(
                            onClick = onToggleExpanded,
                            size = 44.dp,
                            active = isTimelineExpanded,
                            modifier = Modifier.testTag("toggle_timeline_button"),
                        ) {
                            Icon(
                                imageVector = if (isTimelineExpanded) {
                                    Icons.Rounded.KeyboardArrowDown
                                } else {
                                    Icons.Rounded.KeyboardArrowUp
                                },
                                contentDescription = if (isTimelineExpanded) "Collapse timeline" else "Expand timeline",
                                tint = InkPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Departure Scrubber
            DepartureTimeScrubber(
                departureOffsetMinutes = departureOffsetMinutes,
                baseNowEpochSeconds = baseNowEpochSeconds,
                originUtcOffsetSeconds = originOffset,
                bestWindowOffsetMinutes = evaluatedRoute.bestWindowOffsetMinutes,
                slots = departureSlots,
                hapticsEnabled = hapticsEnabled,
                onOffsetChange = onOffsetChange,
                onJumpToBestWindow = onJumpToBestWindow,
            )

            // Quick Leg Glance when collapsed on mobile
            if (!isTimelineExpanded && !isSplitScreen) {
                val activeIdx = (selectedCheckpointIndex ?: 0).coerceIn(0, evaluatedRoute.checkpoints.lastIndex)
                val activeCp = evaluatedRoute.checkpoints[activeIdx]
                Spacer(modifier = Modifier.height(10.dp))
                CompactCheckpointPreviewStrip(
                    checkpoint = activeCp,
                    unitSystem = unitSystem,
                    onClick = onToggleExpanded,
                )
            }

            // Full Chronological Timeline
            AnimatedVisibility(
                visible = isTimelineExpanded || isSplitScreen,
                enter = fadeIn() + expandVertically(spring(dampingRatio = 0.8f, stiffness = 360f)),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val timelineModifier = if (isSplitScreen) {
                        Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 240.dp, max = 560.dp)
                    } else {
                        Modifier.fillMaxWidth().heightIn(max = 360.dp)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = timelineModifier.testTag("route_timeline_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 6.dp),
                    ) {
                        itemsIndexed(
                            items = evaluatedRoute.checkpoints,
                            key = { idx, cp -> "${idx}_${cp.checkpoint.label}" },
                        ) { idx, evalCp ->
                            val isSelected = selectedCheckpointIndex == idx
                            TimelineCheckpointCard(
                                evalCp = evalCp,
                                isFirst = idx == 0,
                                isLast = idx == evaluatedRoute.checkpoints.lastIndex,
                                isSelected = isSelected,
                                unitSystem = unitSystem,
                                onClick = { onSelectCheckpoint(idx) },
                            )

                            // Inject Sunrise/Sunset Horizon Marker if crossed on this leg
                            val solarCrossing = evaluatedRoute.solarMarkers.firstOrNull {
                                it.afterCheckpointIndex == idx
                            }
                            if (solarCrossing != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SolarBoundaryTimelineBanner(marker = solarCrossing)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCheckpointPreviewStrip(
    checkpoint: EvaluatedCheckpoint,
    unitSystem: UnitSystem,
    onClick: () -> Unit,
) {
    RecessedInstrumentWell(
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("compact_timeline_strip"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                AnimatedWeatherEmblem(
                    condition = checkpoint.condition,
                    isDaylight = checkpoint.solarState.isDaylight,
                    size = 38.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${checkpoint.checkpoint.label} · ${Formatters.formatLocalTime(checkpoint.passByEpochSeconds, checkpoint.utcOffsetSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = checkpoint.hazardSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (checkpoint.solarState.glareRisk != GlareRisk.NONE) AmberGlare else InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.formatTemp(checkpoint.temperatureC, unitSystem),
                    style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                    color = InkPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanClear,
                )
            }
        }
    }
}

@Composable
private fun TimelineCheckpointCard(
    evalCp: EvaluatedCheckpoint,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    unitSystem: UnitSystem,
    onClick: () -> Unit,
) {
    val nodeColor = when {
        evalCp.solarState.glareRisk == GlareRisk.SEVERE -> SolarCoral
        evalCp.severity == WeatherSeverity.HAZARD -> CobaltRain
        evalCp.severity == WeatherSeverity.CAUTION -> AmberGlare
        else -> CyanClear
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_checkpoint_${evalCp.checkpoint.index}"),
        verticalAlignment = Alignment.Top,
    ) {
        // Luminous vertical route spine
        Canvas(
            modifier = Modifier
                .width(22.dp)
                .height(if (isSelected) 220.dp else 86.dp)
        ) {
            val cx = size.width / 2f
            val cy = 34.dp.toPx()
            if (!isFirst) {
                drawLine(
                    color = CyanClear.copy(alpha = 0.45f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, cy),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            if (!isLast) {
                drawLine(
                    color = nodeColor.copy(alpha = 0.45f),
                    start = Offset(cx, cy),
                    end = Offset(cx, size.height),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            if (isSelected) {
                drawCircle(
                    color = nodeColor.copy(alpha = 0.3f),
                    radius = 10.dp.toPx(),
                    center = Offset(cx, cy),
                )
            }
            drawCircle(
                color = nodeColor,
                radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                center = Offset(cx, cy),
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        GlassInstrumentPanel(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            cornerRadius = 18.dp,
            elevated = isSelected,
            accentGlow = if (isSelected) nodeColor else null,
            contentPadding = PaddingValues(12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedWeatherEmblem(
                            condition = evalCp.condition,
                            isDaylight = evalCp.solarState.isDaylight,
                            size = 40.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Formatters.formatLocalTime(evalCp.passByEpochSeconds, evalCp.utcOffsetSeconds),
                                    style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                                    color = CyanClear,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "· ${Formatters.formatDistance(evalCp.checkpoint.distanceFromStartMeters, unitSystem)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkMuted,
                                )
                            }
                            Text(
                                text = evalCp.checkpoint.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = evalCp.hazardSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (evalCp.solarState.glareRisk != GlareRisk.NONE) AmberGlare else InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatTemp(evalCp.temperatureC, unitSystem),
                            style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                            color = InkPrimary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = CobaltRain,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${evalCp.precipProbPct}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary,
                            )
                        }
                    }
                }

                // Sun Glare alert pill when applicable
                if (evalCp.solarState.glareRisk != GlareRisk.NONE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val glareTint = if (evalCp.solarState.glareRisk == GlareRisk.SEVERE) SolarCoral else AmberGlare
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(glareTint.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = glareTint,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${evalCp.solarState.glareRisk.label} · ${evalCp.solarState.elevationDeg.toInt()}° sun elevation ahead",
                            style = MaterialTheme.typography.labelMedium,
                            color = glareTint,
                        )
                    }
                }

                // Expanded Instrument Detail Lens
                AnimatedVisibility(visible = isSelected) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AnalogWindCompassDial(
                                windSpeedKmh = evalCp.windSpeedKmh,
                                windDirDeg = evalCp.windDirDeg,
                                unitSystem = unitSystem,
                                modifier = Modifier.weight(1f),
                            )
                            SolarGlareHorizonGauge(
                                solarState = evalCp.solarState,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        RecessedInstrumentWell(
                            cornerRadius = 12.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = CyanClear,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Vis ${Formatters.formatVisibility(evalCp.visibilityMeters, unitSystem)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkSecondary,
                                    )
                                }
                                Text(
                                    text = "Feels ${Formatters.formatTempWithUnit(evalCp.apparentTempC, unitSystem)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkSecondary,
                                )
                                Text(
                                    text = Formatters.formatPrecip(evalCp.precipMm, unitSystem),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CobaltRain,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolarBoundaryTimelineBanner(marker: SolarBoundaryMarker) {
    val isSunrise = marker.type == SolarEventType.SUNRISE
    val accent = if (isSunrise) AmberGlare else SolarCoral

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        GlassInstrumentPanel(
            modifier = Modifier.weight(1f),
            cornerRadius = 14.dp,
            accentGlow = accent,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WbTwilight,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isSunrise) "SUNRISE ON ROUTE" else "SUNSET ON ROUTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                        )
                        Text(
                            text = "Near ${marker.locationHint} · Golden-hour glare risk",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkPrimary,
                        )
                    }
                }
                Text(
                    text = Formatters.formatLocalTime(marker.eventEpochSeconds, marker.utcOffsetSeconds),
                    style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
