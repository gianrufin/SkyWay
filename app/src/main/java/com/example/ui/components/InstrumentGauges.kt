package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.Formatters
import com.example.model.GlareRisk
import com.example.model.SolarState
import com.example.model.UnitSystem
import com.example.model.WeatherCondition
import com.example.ui.theme.AmberGlare
import com.example.ui.theme.CobaltRain
import com.example.ui.theme.CyanClear
import com.example.ui.theme.FrostMist
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SnowIce
import com.example.ui.theme.SolarCoral
import com.example.ui.theme.StormViolet
import com.example.ui.theme.TelemetryNumeralStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AnimatedWeatherEmblem(
    condition: WeatherCondition,
    isDaylight: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val transition = rememberInfiniteTransition(label = "weather_emblem")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "weather_phase",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)

        when (condition) {
            WeatherCondition.CLEAR -> {
                if (isDaylight) {
                    drawGlowingSun(center, w * 0.24f, phase)
                } else {
                    drawCrescentMoon(center, w * 0.26f, phase)
                }
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDaylight) {
                    drawGlowingSun(Offset(w * 0.62f, h * 0.36f), w * 0.18f, phase)
                } else {
                    drawCrescentMoon(Offset(w * 0.62f, h * 0.36f), w * 0.19f, phase)
                }
                val drift = sin(phase * 2f * PI.toFloat()) * w * 0.03f
                drawFrostedCloud(Offset(w * 0.45f + drift, h * 0.58f), w * 0.52f, Color(0xFFE2E8F0))
            }
            WeatherCondition.CLOUDY -> {
                val drift = sin(phase * 2f * PI.toFloat()) * w * 0.035f
                drawFrostedCloud(Offset(w * 0.56f - drift, h * 0.44f), w * 0.48f, Color(0xFF94A3B8))
                drawFrostedCloud(Offset(w * 0.44f + drift, h * 0.58f), w * 0.56f, Color(0xFFCBD5E1))
            }
            WeatherCondition.FOG -> {
                drawFrostedCloud(Offset(w * 0.5f, h * 0.38f), w * 0.52f, FrostMist)
                val shift = sin(phase * 2f * PI.toFloat()) * w * 0.06f
                for (i in 0..2) {
                    val y = h * (0.64f + i * 0.11f)
                    val offset = if (i % 2 == 0) shift else -shift
                    drawLine(
                        color = FrostMist.copy(alpha = 0.78f - i * 0.15f),
                        start = Offset(w * 0.18f + offset, y),
                        end = Offset(w * 0.82f + offset, y),
                        strokeWidth = w * 0.065f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.HEAVY_RAIN -> {
                val cloudColor = if (condition == WeatherCondition.HEAVY_RAIN) Color(0xFF64748B) else Color(0xFF94A3B8)
                drawFrostedCloud(Offset(w * 0.5f, h * 0.42f), w * 0.58f, cloudColor)
                val drops = if (condition == WeatherCondition.HEAVY_RAIN) 4 else 3
                for (i in 0 until drops) {
                    val frac = ((phase * 2f + i * 0.27f) % 1f)
                    val x = w * (0.28f + i * (0.44f / (drops - 1).coerceAtLeast(1)))
                    val yStart = h * 0.62f + frac * h * 0.26f
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanClear.copy(alpha = 0.2f), CobaltRain),
                            startY = yStart,
                            endY = yStart + h * 0.14f,
                        ),
                        start = Offset(x, yStart),
                        end = Offset(x - w * 0.04f, yStart + h * 0.13f),
                        strokeWidth = w * 0.06f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherCondition.SNOW -> {
                drawFrostedCloud(Offset(w * 0.5f, h * 0.40f), w * 0.56f, Color(0xFFCBD5E1))
                for (i in 0..2) {
                    val frac = ((phase * 1.5f + i * 0.33f) % 1f)
                    val x = w * (0.30f + i * 0.20f) + sin((frac + i) * 6.28f) * w * 0.04f
                    val y = h * 0.62f + frac * h * 0.28f
                    drawCircle(
                        color = SnowIce.copy(alpha = 1f - frac * 0.5f),
                        radius = w * 0.055f,
                        center = Offset(x, y),
                    )
                }
            }
            WeatherCondition.THUNDERSTORM -> {
                drawFrostedCloud(Offset(w * 0.5f, h * 0.38f), w * 0.60f, Color(0xFF475569))
                val flashAlpha = if ((phase * 3f) % 1f < 0.25f) 1f else 0.55f
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.52f)
                    lineTo(w * 0.40f, h * 0.70f)
                    lineTo(w * 0.51f, h * 0.70f)
                    lineTo(w * 0.44f, h * 0.90f)
                    lineTo(w * 0.62f, h * 0.65f)
                    lineTo(w * 0.51f, h * 0.65f)
                    close()
                }
                drawPath(bolt, color = AmberGlare.copy(alpha = flashAlpha))
                drawLine(
                    color = StormViolet.copy(alpha = 0.7f),
                    start = Offset(w * 0.28f, h * 0.68f),
                    end = Offset(w * 0.24f, h * 0.82f),
                    strokeWidth = w * 0.05f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawGlowingSun(center: Offset, radius: Float, phase: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(AmberGlare.copy(alpha = 0.48f), Color.Transparent),
            center = center,
            radius = radius * 2.1f,
        ),
        center = center,
        radius = radius * 2.1f,
    )
    rotate(degrees = phase * 360f, pivot = center) {
        for (i in 0 until 8) {
            val angle = i * (PI / 4.0)
            val rStart = radius * 1.25f
            val rEnd = radius * 1.62f
            drawLine(
                color = AmberGlare.copy(alpha = 0.88f),
                start = Offset(
                    center.x + (cos(angle) * rStart).toFloat(),
                    center.y + (sin(angle) * rStart).toFloat(),
                ),
                end = Offset(
                    center.x + (cos(angle) * rEnd).toFloat(),
                    center.y + (sin(angle) * rEnd).toFloat(),
                ),
                strokeWidth = radius * 0.20f,
                cap = StrokeCap.Round,
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF3C4), AmberGlare),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.3f,
        ),
        center = center,
        radius = radius,
    )
}

private fun DrawScope.drawCrescentMoon(center: Offset, radius: Float, phase: Float) {
    val glowAlpha = 0.25f + 0.12f * sin(phase * 2f * PI.toFloat())
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanClear.copy(alpha = glowAlpha), Color.Transparent),
            center = center,
            radius = radius * 2.0f,
        ),
        center = center,
        radius = radius * 2.0f,
    )
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius),
        ),
        center = center,
        radius = radius,
    )
    drawCircle(
        color = Color(0xFF152236),
        center = Offset(center.x + radius * 0.42f, center.y - radius * 0.25f),
        radius = radius * 0.78f,
    )
}

private fun DrawScope.drawFrostedCloud(center: Offset, width: Float, baseColor: Color) {
    val h = width * 0.52f
    val left = center.x - width / 2f
    val bottom = center.y + h * 0.35f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.92f), baseColor),
            startY = center.y - h * 0.4f,
            endY = bottom,
        ),
        topLeft = Offset(left, center.y - h * 0.1f),
        size = Size(width, h * 0.55f),
        cornerRadius = CornerRadius(h * 0.3f, h * 0.3f),
    )
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.96f), baseColor),
        ),
        radius = width * 0.23f,
        center = Offset(center.x - width * 0.11f, center.y - h * 0.16f),
    )
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.90f), baseColor),
        ),
        radius = width * 0.17f,
        center = Offset(center.x + width * 0.16f, center.y - h * 0.04f),
    )
}

@Composable
fun AnalogWindCompassDial(
    windSpeedKmh: Double,
    windDirDeg: Double,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val animatedAngle by animateFloatAsState(
        targetValue = windDirDeg.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "wind_needle",
    )

    RecessedInstrumentWell(
        modifier = modifier,
        cornerRadius = 18.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "WIND VECTOR",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(92.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f - 4.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)

                    // Bezel ring
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF334766), Color(0xFF0E1726))
                        ),
                        radius = r,
                        center = c,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    // 24 ticks
                    for (i in 0 until 24) {
                        val deg = i * 15.0
                        val rad = Math.toRadians(deg - 90.0)
                        val isMajor = i % 6 == 0
                        val inner = r - if (isMajor) 9.dp.toPx() else 5.dp.toPx()
                        drawLine(
                            color = if (isMajor) CyanClear.copy(alpha = 0.85f) else InkMuted.copy(alpha = 0.4f),
                            start = Offset(
                                c.x + (cos(rad) * inner).toFloat(),
                                c.y + (sin(rad) * inner).toFloat(),
                            ),
                            end = Offset(
                                c.x + (cos(rad) * (r - 2.dp.toPx())).toFloat(),
                                c.y + (sin(rad) * (r - 2.dp.toPx())).toFloat(),
                            ),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }

                    // Animated needle pointing where wind blows FROM
                    rotate(degrees = animatedAngle, pivot = c) {
                        val needlePath = Path().apply {
                            moveTo(c.x, c.y - r * 0.68f)
                            lineTo(c.x - 5.dp.toPx(), c.y + 4.dp.toPx())
                            lineTo(c.x, c.y - 2.dp.toPx())
                            lineTo(c.x + 5.dp.toPx(), c.y + 4.dp.toPx())
                            close()
                        }
                        drawPath(needlePath, color = CyanClear)
                        drawLine(
                            color = AmberGlare.copy(alpha = 0.8f),
                            start = Offset(c.x, c.y + 4.dp.toPx()),
                            end = Offset(c.x, c.y + r * 0.45f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    drawCircle(
                        color = InkPrimary,
                        radius = 4.dp.toPx(),
                        center = c,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Formatters.formatWind(windSpeedKmh, unitSystem),
                style = TelemetryNumeralStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                color = InkPrimary,
            )
            Text(
                text = "From ${Formatters.formatCardinalDirection(windDirDeg)} (${windDirDeg.roundToInt()}°)",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
    }
}

@Composable
fun SolarGlareHorizonGauge(
    solarState: SolarState,
    modifier: Modifier = Modifier,
) {
    val glareColor = when (solarState.glareRisk) {
        GlareRisk.SEVERE -> SolarCoral
        GlareRisk.MODERATE -> AmberGlare
        GlareRisk.NONE -> CyanClear
    }

    RecessedInstrumentWell(
        modifier = modifier,
        cornerRadius = 18.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "SOLAR & GLARE",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    val arcWidth = size.width * 0.84f
                    val arcHeight = size.height * 1.45f
                    val left = (size.width - arcWidth) / 2f
                    val top = size.height * 0.16f
                    val horizonY = top + arcHeight / 2f

                    // Horizon line
                    drawLine(
                        color = InkMuted.copy(alpha = 0.45f),
                        start = Offset(left - 8.dp.toPx(), horizonY),
                        end = Offset(left + arcWidth + 8.dp.toPx(), horizonY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // Sky dome arc
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(AmberGlare, CyanClear, AmberGlare)
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(arcWidth, arcHeight),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Map elevation (-15..75 deg) along dome height & relative angle along horizontal
                    val centerX = size.width / 2f
                    val radiusX = arcWidth / 2f
                    val radiusY = arcHeight / 2f
                    val elevClamped = solarState.elevationDeg.coerceIn(-12.0, 75.0)
                    val elevNorm = (elevClamped / 75.0).toFloat()

                    // Relative angle 0 = dead ahead (center), 90 = side, 180 = behind
                    val relNorm = (solarState.relativeAngleDeg / 180.0).toFloat().coerceIn(0f, 1f)
                    val sunX = centerX + (relNorm - 0.1f) * radiusX * 0.65f
                    val sunY = (horizonY - elevNorm * radiusY * 0.85f).coerceIn(top, horizonY + 12.dp.toPx())

                    if (solarState.isDaylight) {
                        // Glare ray cone toward driver viewpoint
                        if (solarState.glareRisk != GlareRisk.NONE) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(glareColor.copy(alpha = 0.7f), Color.Transparent),
                                    start = Offset(sunX, sunY),
                                    end = Offset(centerX, horizonY + 6.dp.toPx()),
                                ),
                                start = Offset(sunX, sunY),
                                end = Offset(centerX, horizonY + 6.dp.toPx()),
                                strokeWidth = 10.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glareColor.copy(alpha = 0.55f), Color.Transparent),
                                center = Offset(sunX, sunY),
                                radius = 18.dp.toPx(),
                            ),
                            center = Offset(sunX, sunY),
                            radius = 18.dp.toPx(),
                        )
                        drawCircle(
                            color = if (solarState.glareRisk == GlareRisk.SEVERE) SolarCoral else AmberGlare,
                            radius = 7.dp.toPx(),
                            center = Offset(sunX, sunY),
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFF64748B),
                            radius = 6.dp.toPx(),
                            center = Offset(centerX, horizonY + 10.dp.toPx()),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = solarState.glareRisk.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = glareColor,
                )
            }
            Text(
                text = if (solarState.isDaylight) {
                    "Sun ${solarState.elevationDeg.roundToInt()}° alt · ${solarState.relativeAngleDeg.roundToInt()}° off nose"
                } else {
                    "Nighttime transit"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
    }
}
