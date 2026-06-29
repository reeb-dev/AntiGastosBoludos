package com.antigastos.boludos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ArgCeleste = Color(0xFF74ACDF)
private val ArgCelesteDark = Color(0xFF1F4E8A)
private val ArgSunYellow = Color(0xFFF6B40E)
private val ArgInk = Color(0xFF0F1B2D)
private val ArgWhite = Color(0xFFFFFFFF)
private val ArgPaper = Color(0xFFF7FBFF)
private val ArgNight = Color(0xFF0B1220)
private val ArgSurfaceDark = Color(0xFF11192A)

private val ChampGold = Color(0xFFC9A227)
private val ChampGoldDark = Color(0xFF8B6914)
private val ChampInk = Color(0xFF1A1206)
private val ChampPaper = Color(0xFFFFFAF0)
private val ChampNight = Color(0xFF120D05)
private val ChampSurfaceDark = Color(0xFF1C1508)

private val LightColors = lightColorScheme(
    primary = ArgCeleste,
    onPrimary = ArgInk,
    primaryContainer = Color(0xFFD6E8F7),
    onPrimaryContainer = ArgInk,
    secondary = ArgSunYellow,
    onSecondary = ArgInk,
    secondaryContainer = Color(0xFFFFE6A6),
    onSecondaryContainer = ArgInk,
    tertiary = ArgCelesteDark,
    onTertiary = ArgWhite,
    background = ArgPaper,
    onBackground = ArgInk,
    surface = ArgWhite,
    onSurface = ArgInk,
    surfaceVariant = Color(0xFFE3EDF7),
    onSurfaceVariant = Color(0xFF263240),
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
    primary = ArgCeleste,
    onPrimary = ArgInk,
    primaryContainer = ArgCelesteDark,
    onPrimaryContainer = ArgWhite,
    secondary = ArgSunYellow,
    onSecondary = ArgInk,
    tertiary = Color(0xFF9CC9F0),
    background = ArgNight,
    onBackground = ArgWhite,
    surface = ArgSurfaceDark,
    onSurface = ArgWhite,
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

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
