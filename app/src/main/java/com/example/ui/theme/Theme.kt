package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SkyWayGlassPalette(
    val glassTop: Color = SkyGlassTop,
    val glassBottom: Color = SkyGlassBottom,
    val glassElevatedTop: Color = SkyGlassElevatedTop,
    val glassElevatedBottom: Color = SkyGlassElevatedBottom,
    val recessedTop: Color = SkyRecessedTop,
    val recessedBottom: Color = SkyRecessedBottom,
    val rimBright: Color = RimSpecularBright,
    val rimSoft: Color = RimSpecularSoft,
    val rimAlloy: Color = RimAlloySubtle,
    val innerShadow: Color = InnerShadowDark,
    val cyanClear: Color = CyanClear,
    val emeraldOptimal: Color = EmeraldOptimal,
    val amberGlare: Color = AmberGlare,
    val solarCoral: Color = SolarCoral,
    val cobaltRain: Color = CobaltRain,
    val stormViolet: Color = StormViolet,
    val frostMist: Color = FrostMist,
)

val LocalSkyWayGlass = staticCompositionLocalOf { SkyWayGlassPalette() }

private val SkyWayDarkColorScheme = darkColorScheme(
    primary = CyanClear,
    onPrimary = SkySlateDeep,
    primaryContainer = Color(0xFF143554),
    onPrimaryContainer = CyanGlow,
    secondary = AmberGlare,
    onSecondary = SkySlateDeep,
    secondaryContainer = Color(0xFF3E2A12),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = CobaltRain,
    onTertiary = InkPrimary,
    background = SkySlateDeep,
    onBackground = InkPrimary,
    surface = SkySlateBase,
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFF1A273A),
    onSurfaceVariant = InkSecondary,
    outline = RimAlloySubtle,
    error = SolarCoral,
    onError = SkySlateDeep,
)

@Composable
fun SkyWayTheme(
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSkyWayGlass provides SkyWayGlassPalette(),
    ) {
        MaterialTheme(
            colorScheme = SkyWayDarkColorScheme,
            typography = SkyWayTypography,
            content = content,
        )
    }
}
