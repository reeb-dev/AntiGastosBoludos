package com.antigastos.boludos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de la app. Conviven con MaterialTheme: Material da la
 * paleta primaria/superficie, [AgColors] da los semánticos por mood y los
 * azulitos especiales de la app (cards de “sección”, gradientes calendario,
 * highlights de racha, etc.).
 *
 * Si una pantalla necesita un color, primero buscar acá. Sólo dejar
 * `Color(0xFF...)` cuando es realmente único de ese composable.
 */
object AgColors {
    // --- Mood semántico ---
    val CheerGreen = Color(0xFF2E7D32)
    val CheerGreenSoft = Color(0xFFE8F5E9)
    val NeutralBlue = Color(0xFF1976D2)
    val BurnAmber = Color(0xFFE0A030)
    val BurnAmberSoft = Color(0xFFFFF3E0)
    val AlarmRed = Color(0xFFD7263D)
    val AlarmRedSoft = Color(0xFFFDE7EA)

    // --- Permisos del SpendingBudgetEngine ---
    val PermissionGreen = CheerGreen
    val PermissionYellow = Color(0xFFE0A030)
    val PermissionRed = Color(0xFFEF6C00)
    val PermissionForbidden = AlarmRed

    // --- Niveles de “shame” / vergüenza del Home ---
    val ShameOk = CheerGreen
    val ShameAware = Color(0xFFE0A030)
    val ShameDanger = Color(0xFFEF6C00)
    val ShameLudopata = AlarmRed

    // --- Accents de UI ---
    val SectionSurface = Color(0xFFF5F9FF)
    val GoldHighlight = Color(0xFFFFD54F)
    val StreakFire = Color(0xFFFB8C00)
    val LegendaryPurple = Color(0xFF8E24AA)
}
