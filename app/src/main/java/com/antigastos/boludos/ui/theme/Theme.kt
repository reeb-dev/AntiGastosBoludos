package com.antigastos.boludos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ChampGold = Color(0xFFC9A227)
private val ChampGoldDark = Color(0xFF8B6914)
private val ChampInk = Color(0xFF1A1206)
private val ChampPaper = Color(0xFFFFFAF0)
private val ChampNight = Color(0xFF120D05)
private val ChampSurfaceDark = Color(0xFF1C1508)

private val LightColors = lightColorScheme(
    primary = ArgPalette.Celeste,
    onPrimary = ArgPalette.Ink,
    primaryContainer = ArgPalette.CelesteContainer,
    onPrimaryContainer = ArgPalette.Ink,
    secondary = ArgPalette.Sun,
    onSecondary = ArgPalette.Ink,
    secondaryContainer = ArgPalette.SunLight,
    onSecondaryContainer = ArgPalette.Ink,
    tertiary = ArgPalette.CelesteDark,
    onTertiary = ArgPalette.White,
    background = ArgPalette.CelestePaper,
    onBackground = ArgPalette.Ink,
    surface = ArgPalette.White,
    onSurface = ArgPalette.Ink,
    surfaceVariant = ArgPalette.CelesteSurface,
    onSurfaceVariant = ArgPalette.InkMuted,
    outline = ArgPalette.Celeste,
    outlineVariant = ArgPalette.CelesteContainer,
)

private val LightChampionColors = lightColorScheme(
    primary = ChampGold,
    onPrimary = ChampInk,
    primaryContainer = Color(0xFFFFF4D6),
    onPrimaryContainer = ChampInk,
    secondary = ChampGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8B8),
    onSecondaryContainer = ChampInk,
    tertiary = ChampGoldDark,
    onTertiary = Color.White,
    background = ChampPaper,
    onBackground = ChampInk,
    surface = Color.White,
    onSurface = ChampInk,
    surfaceVariant = Color(0xFFF3E9D2),
    onSurfaceVariant = Color(0xFF3D3018),
)

private val DarkColors = darkColorScheme(
    primary = ArgPalette.Celeste,
    onPrimary = ArgPalette.Ink,
    primaryContainer = ArgPalette.CelesteDark,
    onPrimaryContainer = ArgPalette.White,
    secondary = ArgPalette.Sun,
    onSecondary = ArgPalette.Ink,
    secondaryContainer = Color(0xFF5C4A00),
    onSecondaryContainer = ArgPalette.SunLight,
    tertiary = ArgPalette.CelesteLight,
    onTertiary = ArgPalette.Ink,
    background = ArgPalette.Night,
    onBackground = ArgPalette.White,
    surface = ArgPalette.SurfaceDark,
    onSurface = ArgPalette.White,
    surfaceVariant = Color(0xFF1A2840),
    onSurfaceVariant = ArgPalette.CelesteLight,
)

private val DarkChampionColors = darkColorScheme(
    primary = ChampGold,
    onPrimary = ChampInk,
    primaryContainer = ChampGoldDark,
    onPrimaryContainer = Color(0xFFFFF4D6),
    secondary = Color(0xFFFFD54F),
    onSecondary = ChampInk,
    tertiary = Color(0xFFFFE082),
    onTertiary = ChampInk,
    background = ChampNight,
    onBackground = Color(0xFFFFF4D6),
    surface = ChampSurfaceDark,
    onSurface = Color(0xFFFFF4D6),
)

@Composable
fun AntiGastosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    championTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        championTheme && darkTheme -> DarkChampionColors
        championTheme && !darkTheme -> LightChampionColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme && !championTheme
                isAppearanceLightNavigationBars = !darkTheme && !championTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
