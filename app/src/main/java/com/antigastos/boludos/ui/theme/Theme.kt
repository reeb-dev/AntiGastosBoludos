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

private val VerdeBandera = Color(0xFF2E7D32)
private val OroSol = Color(0xFFF9A825)

private val LightColors = lightColorScheme(
    primary = VerdeBandera,
    secondary = OroSol,
    tertiary = Color(0xFF00695C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = OroSol,
    tertiary = Color(0xFF4DB6AC),
)

@Composable
fun AntiGastosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
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
