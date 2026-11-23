package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Formatters
import com.example.model.DepartureWindowSlot
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
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_SCRUB_MINUTES = 72 * 60 // 3 days of interactive fine-scrubbing

@Composable
fun DepartureTimeScrubber(
    departureOffsetMinutes: Int,
    baseNowEpochSeconds: Long,
    originUtcOffsetSeconds: Int?,
    bestWindowOffsetMinutes: Int?,
    slots: List<DepartureWindowSlot>,
    hapticsEnabled: Boolean,
    onOffsetChange: (Int) -> Unit,
    onJumpToBestWindow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticBucket by remember { mutableIntStateOf(departureOffsetMinutes / 15) }

    val activeDepartureEpoch = baseNowEpochSeconds + departureOffsetMinutes * 60L
    val fractionTarget = (departureOffsetMinutes.toFloat() / MAX_SCRUB_MINUTES.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fractionTarget,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "scrubber_fraction",
    )

    val isAtBest = bestWindowOffsetMinutes != null && abs(departureOffsetMinutes - bestWindowOffsetMinutes) <= 30

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = CyanClear,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (departureOffsetMinutes == 0) "DEPART NOW" else "DEPARTURE +${formatOffset(departureOffsetMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanClear,
                    )
                    Text(
                        text = Formatters.formatDayAndTime(activeDepartureEpoch, originUtcOffsetSeconds),
                        style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                        color = InkPrimary,
                    )
                }
            }

            if (bestWindowOffsetMinutes != null) {
                val bestTint by animateColorAsState(
                    targetValue = if (isAtBest) EmeraldOptimal else AmberGlare,
                    label = "best_window_tint",
                )
                TactileGlassChip(
                    onClick = onJumpToBestWindow,
                    selected = isAtBest,
                    accentColor = bestTint,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("best_window_button"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = bestTint,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAtBest) "Optimal" else "Best Window",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recessed tactile ruler track
        RecessedInstrumentWell(
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("departure_scrubber_track"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(MAX_SCRUB_MINUTES, hapticsEnabled) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val snapped = (((frac * MAX_SCRUB_MINUTES) / 15f).roundToInt() * 15)
                                .coerceIn(0, MAX_SCRUB_MINUTES)
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onOffsetChange(snapped)
                        }
                    }
                    .pointerInput(MAX_SCRUB_MINUTES, hapticsEnabled) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val snapped = (((frac * MAX_SCRUB_MINUTES) / 15f).roundToInt() * 15)
                                .coerceIn(0, MAX_SCRUB_MINUTES)
                            val bucket = snapped / 15
                            if (bucket != lastHapticBucket) {
                                lastHapticBucket = bucket
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            onOffsetChange(snapped)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barHeight = 6.dp.toPx()
                    val barTop = h * 0.22f

                    // Forecast quality heatmap ribbon across the 72h track
                    if (slots.isNotEmpty()) {
                        val segW = w / slots.size.toFloat()
                        slots.forEachIndexed { idx, slot ->
                            val c = when (slot.severity) {
                                WeatherSeverity.CLEAR -> EmeraldOptimal.copy(alpha = 0.72f)
                                WeatherSeverity.CAUTION -> AmberGlare.copy(alpha = 0.78f)
                                WeatherSeverity.HAZARD -> CobaltRain.copy(alpha = 0.85f)
                            }
                            drawRoundRect(
                                color = c,
                                topLeft = Offset(idx * segW + 1f, barTop),
                                size = Size((segW - 2f).coerceAtLeast(2f), barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                            )
                        }
                    } else {
                        drawRoundRect(
                            color = CyanClear.copy(alpha = 0.3f),
                            topLeft = Offset(0f, barTop),
                            size = Size(w, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        )
                    }

                    // Calibrated Vernier tick marks (every 2 hours = 36 ticks across 72h)
                    val tickCount = 36
                    for (i in 0..tickCount) {
                        val x = (i.toFloat() / tickCount.toFloat()) * w
                        val isMajor = i % 6 == 0 // Every 12h
                        val tickTop = barTop + barHeight + 5.dp.toPx()
                        val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                        drawLine(
                            color = if (isMajor) InkSecondary.copy(alpha = 0.75f) else InkMuted.copy(alpha = 0.35f),
                            start = Offset(x, tickTop),
                            end = Offset(x, tickTop + tickLen),
                            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                        )
                    }

                    // Best-window diamond marker on track
                    if (bestWindowOffsetMinutes != null) {
                        val bestX = ((bestWindowOffsetMinutes.toFloat() / MAX_SCRUB_MINUTES.toFloat()).coerceIn(0f, 1f)) * w
                        drawCircle(
                            color = EmeraldOptimal,
                            radius = 3.5.dp.toPx(),
                            center = Offset(bestX, barTop + barHeight / 2f),
                        )
                    }

                    // Physical convex glass thumb lens
                    val thumbX = (animatedFraction * w).coerceIn(14.dp.toPx(), w - 14.dp.toPx())
                    val thumbY = h / 2f
                    val thumbW = 24.dp.toPx()
                    val thumbH = h - 2.dp.toPx()

                    // Soft glow behind thumb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyanClear.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(thumbX, thumbY),
                            radius = 22.dp.toPx(),
                        ),
                        center = Offset(thumbX, thumbY),
                        radius = 22.dp.toPx(),
                    )

                    // Beveled glass lens body
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xE638BDF8), Color(0xCC0F2744))
                        ),
                        topLeft = Offset(thumbX - thumbW / 2f, thumbY - thumbH / 2f),
                        size = Size(thumbW, thumbH),
                        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.75f),
                        topLeft = Offset(thumbX - thumbW / 2f, thumbY - thumbH / 2f),
                        size = Size(thumbW, thumbH),
                        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                    // Center vertical hairline reticle
                    drawLine(
                        color = Color.White,
                        start = Offset(thumbX, thumbY - thumbH * 0.28f),
                        end = Offset(thumbX, thumbY + thumbH * 0.28f),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick departure presets
        val tomorrow8amOffset = remember(baseNowEpochSeconds) {
            computeTomorrowMorningOffset(baseNowEpochSeconds)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val presets = listOf(
                "Now" to 0,
                "+2h" to 120,
                "+6h" to 360,
                "+12h" to 720,
                "Tomorrow 8A" to tomorrow8amOffset,
                "+48h" to 2880,
            )
            presets.forEach { (label, mins) ->
                val isSelected = abs(departureOffsetMinutes - mins) < 10
                TactileGlassChip(
                    onClick = { onOffsetChange(mins) },
                    selected = isSelected,
                    accentColor = CyanClear,
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("preset_${label.lowercase().replace(" ", "_")}"),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) InkPrimary else InkSecondary,
                    )
                }
            }
        }
    }
}

private fun formatOffset(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun computeTomorrowMorningOffset(baseNowEpochSeconds: Long): Int {
    val cal = Calendar.getInstance().apply {
        timeInMillis = baseNowEpochSeconds * 1000L
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val diffMins = ((cal.timeInMillis / 1000L - baseNowEpochSeconds) / 60L).toInt()
    return diffMins.coerceIn(60, MAX_SCRUB_MINUTES)
}
