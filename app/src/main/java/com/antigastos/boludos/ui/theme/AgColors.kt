package com.antigastos.boludos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens semánticos de la app. Los acentos argentinos salen de [ArgPalette]
 * (colores de la bandera en `arg.png`); el resto son estados (verde/rojo).
 */
object AgColors {
    // --- Mood semántico ---
    val CheerGreen = Color(0xFF2E7D32)
    val CheerGreenSoft = Color(0xFFE8F5E9)
    val NeutralBlue = ArgPalette.CelesteDark
    val BurnAmber = ArgPalette.Sun
    val BurnAmberSoft = ArgPalette.SunContainer
    val AlarmRed = Color(0xFFD7263D)
    val AlarmRedSoft = Color(0xFFFDE7EA)

    // --- Permisos del SpendingBudgetEngine ---
    val PermissionGreen = CheerGreen
    val PermissionYellow = ArgPalette.Sun
    val PermissionRed = Color(0xFFEF6C00)
    val PermissionForbidden = AlarmRed

    // --- Niveles de “shame” / vergüenza del Home ---
    val ShameOk = CheerGreen
    val ShameAware = ArgPalette.Sun
    val ShameDanger = Color(0xFFEF6C00)
    val ShameLudopata = AlarmRed

    // --- Accents de UI (bandera arg) ---
    val SectionSurface = ArgPalette.CelesteSection
    val GoldHighlight = ArgPalette.Sun
    val StreakFire = Color(0xFFFB8C00)
    val LegendaryPurple = Color(0xFF8E24AA)
}
