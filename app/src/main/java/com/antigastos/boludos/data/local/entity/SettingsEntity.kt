package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una sola fila (id = 1) con preferencias del usuario.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1L,
    /** Nombre con el que te saluda la app (ej. en el Home). */
    val userDisplayName: String = "Vecino",
    val onboardingDone: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 21,
    val budgetAlertsEnabled: Boolean = true,
    /** Aviso ~22 h para no perder la racha / cerrar el día. */
    val streakNudgeEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val usdRate: Double = 0.0,
    /** Personaje que le habla al usuario: termo, tachero, abuela, jefe, comentarista, fisura, streamer. */
    val personaKey: String = "termo",
    /** Si el usuario quiere TTS (lectura por voz). */
    val ttsEnabled: Boolean = true,
    /** Si suenan efectos al tocar botones. */
    val soundEnabled: Boolean = true,
    /** Sello de la última ruleta tirada (yyyy-MM-dd). */
    val lastRouletteDay: String = "",
    /** Texto de la última frase de la ruleta. */
    val lastRouletteText: String = "",
    /** Si es true, jamás llamamos a meme-api ni a Tenor; solo usamos los stickers Compose locales. */
    val localStickersOnly: Boolean = true,
    /** Si es true, las frases del Home y la Ruleta se piden a Gemini. Si falla, fallback a packs locales. */
    val aiPhrasesEnabled: Boolean = true,
    /**
     * API key de Google Gemini que el usuario pegó manualmente. Sólo se usa
     * como fallback si en el APK no viaja una key embebida vía BuildConfig.
     * En la mayoría de los builds este campo queda vacío y nadie lo ve.
     */
    val geminiApiKey: String = "",
    /**
     * Activa controles avanzados (API key, datos crudos). Se prende tocando
     * 7 veces el título "Frases con IA" en Ajustes. El usuario común nunca
     * se entera de que existe una API key.
     */
    val developerMode: Boolean = false,
    /** Día (yyyy-MM) en que se mostró el modal de recap automático. Para no spamear. */
    val lastRecapPromptMonth: String = "",
    /** Cargas de "escudo" para no romper la racha (perdonar un día con gasto). */
    val streakFreezeCharges: Int = 1,
    /**
     * Días (LocalDate.toEpochDay) perdonados al calcular la racha sin gastar.
     * CSV simple, ej. "19876,19877".
     */
    val streakForgivenEpochDaysCsv: String = "",
    /**
     * Si es false, tras el onboarding mostramos el quiz de personaje una vez
     * y guardamos el resultado como [personaKey].
     */
    val personaQuizDone: Boolean = false,
    /** Día (yyyy-MM-dd) en que el usuario ya reclamó la lotería diaria. */
    val lastDailyGiftClaimDay: String = "",
    /** Último día en que sonó el micro-háptico de “cerraste sin gastar hoy”. */
    val lastCleanDayHapticDay: String = "",
    /**
     * Si es true, el personaje cambia automáticamente cada vez que se abre
     * la app (rotación al azar). Si está apagado, queda fija la persona
     * elegida por el usuario.
     */
    val personaRotateEnabled: Boolean = false,
    /** Sello del último día (yyyy-MM-dd) en que rotamos automáticamente la persona. */
    val lastPersonaRotationDay: String = "",
    /** Si es true, ya mostramos el coach mark con tips del Home. */
    val homeTipsShown: Boolean = false,
    /** Si es true, ya mostramos la pantalla "así te va a hablar" tras el quiz. */
    val personaWelcomeShown: Boolean = false,
    /**
     * Modo pareja v1 (local, sin sync). Cuando está prendido, en el editor
     * de gastos aparece un toggle "compartido con [partnerName]" y los
     * gastos cargados con esa marca cuentan como ahorro compartido.
     */
    val partnerEnabled: Boolean = false,
    /** Nombre que mostramos cuando el modo pareja está prendido (ej. "Lu"). */
    val partnerName: String = "",
    /**
     * % del gasto que carga el usuario actual cuando marca un ítem como
     * compartido. 50 = "vamos al 50/50", 70 = "vos pagás 70 y tu pareja 30".
     */
    val partnerDefaultSplit: Int = 50,
    /**
     * Notificación fija (ongoing) con racha + gasto de hoy y atajos +5k/+10k.
     * Opt-in para no molestar sin consentimiento explícito.
     */
    val persistStreakNotification: Boolean = false,
    /** yyyy-MM-dd del último "Daily wrap" nocturno mostrado en Home. */
    val lastDailyWrapDay: String = "",
    /**
     * yyyy-MM-dd del último día cerrado **explícitamente** por el usuario
     * (botón "Cerré el día"). Si coincide con hoy, no aparece el wrap auto.
     */
    val lastDayClosedDay: String = "",
    /** Lunes (yyyy-MM-dd) de la semana del desafío semanal activo. */
    val weeklyChallengeWeekStart: String = "",
    val weeklyChallengeSlug: String = "",
    val weeklyChallengeLimitPesos: Long = 0L,
    val weeklyChallengeTitle: String = "",
    /** Paleta “campeón” (dorado) si ya desbloqueaste por puntaje. */
    val championThemeEnabled: Boolean = false,
    /**
     * Mostrar “costo en horas de tu vida” (sueldo neto mensual + horas semanales).
     */
    val lifeHoursEnabled: Boolean = false,
    /** Sueldo neto mensual en pesos (0 = no configurado). */
    val monthlyNetIncomePesos: Long = 0L,
    /** Horas de laburo por semana (para derivar $/h). */
    val weeklyWorkHours: Int = 40,
    /**
     * En el heatmap del Home, pintar días futuros con el ritmo actual
     * (promedio diario hasta hoy).
     */
    val heatmapPaceProjectionEnabled: Boolean = true,
    /**
     * Modo “antojo 24 h”: en categorías impulsivas y montos altos ofrece
     * posponer el gasto con recordatorio.
     */
    val crushCooldownEnabled: Boolean = false,
    /** Monto mínimo (pesos) para ofrecer la heladera de 24 h. */
    val crushMinPesos: Long = 15_000L,
    /**
     * Sugerir pasar gastos manualmente repetidos a “Suscripciones / fijos”.
     */
    val phantomRecurringHintsEnabled: Boolean = true,
    /**
     * [RecurringPhantomSuggestion.suggestionKey] que el usuario descartó
     * en el Home para no insistir hasta que cambien los datos.
     */
    val dismissedPhantomSuggestionKey: String = "",
    /** Código para concepto de transferencia (ej. AGB-X7K2M9). */
    val donationReferenceCode: String = "",
    val adStatBannerImpressions: Long = 0L,
    val adStatNativeImpressions: Long = 0L,
    val adStatRewardedOffers: Long = 0L,
    val adStatRewardedCompleted: Long = 0L,
)
