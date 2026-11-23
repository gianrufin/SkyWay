package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSkyWayGlass

@Composable
fun GlassInstrumentPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    accentGlow: Color? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalSkyWayGlass.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val topColor = if (elevated) glass.glassElevatedTop else glass.glassTop
    val bottomColor = if (elevated) glass.glassElevatedBottom else glass.glassBottom

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 20.dp else 12.dp,
                shape = shape,
                ambientColor = accentGlow ?: Color.Black,
                spotColor = accentGlow ?: Color.Black,
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )
            .drawBehind {
                val radiusPx = cornerRadius.toPx()
                // Subtle top specular sheen inside the glass face
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.38f,
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                )
                // Optional subtle colored accent wash along top edge
                if (accentGlow != null) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accentGlow.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.2f, 0f),
                            radius = size.width * 0.7f,
                        ),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glass.rimBright,
                        glass.rimAlloy,
                        glass.rimSoft,
                    )
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun RecessedInstrumentWell(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalSkyWayGlass.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(glass.recessedTop, glass.recessedBottom)
                )
            )
            .drawBehind {
                val r = cornerRadius.toPx()
                // Top inner shadow bevel
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(glass.innerShadow, Color.Transparent),
                        startY = 0f,
                        endY = 14.dp.toPx(),
                    ),
                    cornerRadius = CornerRadius(r, r),
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        glass.rimSoft,
                    )
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun TactileGlassOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    active: Boolean = false,
    activeColor: Color = LocalSkyWayGlass.current.cyanClear,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalSkyWayGlass.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "orb_scale",
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (active) 14.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (active) activeColor else Color.Black,
                spotColor = if (active) activeColor else Color.Black,
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (active) {
                        listOf(
                            activeColor.copy(alpha = 0.36f),
                            glass.glassElevatedBottom,
                        )
                    } else {
                        listOf(glass.glassElevatedTop, glass.glassBottom)
                    }
                )
            )
            .drawBehind {
                // Convex lens highlight at top dome
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(this.size.width * 0.35f, this.size.height * 0.24f),
                        radius = this.size.width * 0.55f,
                    )
                )
                if (active) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.7f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (active) activeColor.copy(alpha = 0.9f) else glass.rimBright,
                        glass.rimSoft,
                    )
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(10.dp),
        content = content,
    )
}

@Composable
fun TactileGlassChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: Color = LocalSkyWayGlass.current.cyanClear,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalSkyWayGlass.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chip_scale",
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val shape = RoundedCornerShape(14.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (selected) {
                        listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.14f))
                    } else {
                        listOf(glass.glassElevatedTop, glass.glassBottom)
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (selected) accentColor.copy(alpha = 0.9f) else glass.rimBright,
                        glass.rimSoft,
                    )
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(contentPadding),
        content = content,
    )
}
