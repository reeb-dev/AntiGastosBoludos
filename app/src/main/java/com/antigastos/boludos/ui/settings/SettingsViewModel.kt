package com.antigastos.boludos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.app.NotificationManagerCompat
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.notifications.StreakStatusNotifier
import com.antigastos.boludos.work.WorkSchedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: AntiGastosApplication) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = app.settingsRepository.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast

    fun toggleStreakNudge(enabled: Boolean) = update { it.copy(streakNudgeEnabled = enabled) }

    fun togglePersistStreakNotification(enabled: Boolean) {
        viewModelScope.launch {
            app.settingsRepository.update { it.copy(persistStreakNotification = enabled) }
            WorkSchedulers.applyFromSettings(app, app.settingsRepository.getSnapshot())
            if (enabled) {
                StreakStatusNotifier.refresh(app)
            } else {
                NotificationManagerCompat.from(app).cancel(StreakStatusNotifier.NOTIFICATION_ID)
            }
        }
    }

    fun toggleChampionTheme(enabled: Boolean) {
        viewModelScope.launch {
            val pts = app.scoreRepository.snapshot().points
            if (enabled && pts < 150) {
                _toast.emit("Necesitás al menos 150 puntos para el tema dorado «campeón».")
                return@launch
            }
            app.settingsRepository.update { it.copy(championThemeEnabled = enabled) }
        }
    }

    fun toggleDailyReminder(enabled: Boolean) = update { it.copy(dailyReminderEnabled = enabled) }
    fun setReminderHour(hour: Int) = update { it.copy(dailyReminderHour = hour.coerceIn(0, 23)) }
    fun toggleBudgetAlerts(enabled: Boolean) = update { it.copy(budgetAlertsEnabled = enabled) }
    fun toggleBiometric(enabled: Boolean) = update { it.copy(biometricLockEnabled = enabled) }
    fun setAutoLockMinutes(minutes: Int) = update { it.copy(autoLockMinutes = minutes.coerceIn(0, 60)) }
    fun setPersonaKey(key: String) = update { it.copy(personaKey = key) }
    fun toggleTts(enabled: Boolean) {
        if (!enabled) app.tts.stop()
        update { it.copy(ttsEnabled = enabled) }
    }
    fun toggleSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun toggleLocalStickersOnly(enabled: Boolean) = update { it.copy(localStickersOnly = enabled) }
    fun toggleAiPhrases(enabled: Boolean) = update { it.copy(aiPhrasesEnabled = enabled) }
    fun togglePersonaRotation(enabled: Boolean) = update {
        it.copy(personaRotateEnabled = enabled)
    }
    fun resetHomeTips() = update { it.copy(homeTipsShown = false) }
    fun rotatePersonaNow() {
        viewModelScope.launch {
            val s = app.settingsRepository.getSnapshot()
            val candidates = com.antigastos.boludos.domain.PersonaCatalog.all
                .filter { it.key != s.personaKey }
            val pick = candidates.randomOrNull() ?: return@launch
            app.settingsRepository.update { it.copy(personaKey = pick.key) }
            _toast.emit("Ahora te habla ${pick.emoji} ${pick.displayName}")
        }
    }
    fun setGeminiApiKey(key: String) = update { it.copy(geminiApiKey = key.trim()) }
    fun enableDeveloperMode() = update { it.copy(developerMode = true) }
    fun disableDeveloperMode() = update { it.copy(developerMode = false) }
    fun markRecapPromptShown(monthYm: String) = update { it.copy(lastRecapPromptMonth = monthYm) }

    fun testGemini() {
        viewModelScope.launch {
            val s = app.settingsRepository.getSnapshot()
            if (!s.aiPhrasesEnabled) {
                _toast.emit("Activá primero la IA en el interruptor de arriba.")
                return@launch
            }
            val key = com.antigastos.boludos.data.GeminiCredentials.resolve(s)
            val firebase = app.firebaseAiClient
            val res = when {
                firebase.isReady -> firebase.oneShot(
                    systemInstruction = "Sos un porteño termo. Una sola frase corta, sin emojis al inicio.",
                    userMessage = "Decime una frase de bienvenida cargosa y argentina, una sola.",
                    maxOutputTokens = 80,
                    temperature = 0.9,
                )
                key.isNotBlank() -> app.geminiApi.oneShot(
                    systemInstruction = "Sos un porteño termo. Una sola frase corta, sin emojis al inicio.",
                    userMessage = "Decime una frase de bienvenida cargosa y argentina, una sola.",
                    apiKey = key,
                    maxOutputTokens = 80,
                    temperature = 0.9,
                )
                else -> {
                    _toast.emit("La IA no está disponible: la app va a usar las frases locales.")
                    return@launch
                }
            }
            val msg = when (res) {
                is com.antigastos.boludos.data.GeminiApi.Result.Ok -> "✅ Gemini: ${res.text}"
                is com.antigastos.boludos.data.GeminiApi.Result.Err -> "❌ ${res.message}"
            }
            _toast.emit(msg)
        }
    }
    fun togglePartner(enabled: Boolean) = update { it.copy(partnerEnabled = enabled) }
    fun setPartnerName(name: String) = update { it.copy(partnerName = name.trim().take(20)) }
    fun setPartnerDefaultSplit(percent: Int) = update {
        it.copy(partnerDefaultSplit = percent.coerceIn(1, 99))
    }

    fun toggleLifeHours(enabled: Boolean) = update { it.copy(lifeHoursEnabled = enabled) }
    fun setMonthlyNetIncomePesos(v: Long) = update { it.copy(monthlyNetIncomePesos = v.coerceAtLeast(0L)) }
    fun setWeeklyWorkHours(h: Int) = update { it.copy(weeklyWorkHours = h.coerceIn(5, 60)) }
    fun toggleHeatmapPaceProjection(enabled: Boolean) = update { it.copy(heatmapPaceProjectionEnabled = enabled) }
    fun toggleCrushCooldown(enabled: Boolean) = update { it.copy(crushCooldownEnabled = enabled) }
    fun setCrushMinPesos(v: Long) = update { it.copy(crushMinPesos = v.coerceIn(1_000L, 500_000_000L)) }

    fun togglePhantomRecurringHints(enabled: Boolean) = update { it.copy(phantomRecurringHintsEnabled = enabled) }

    fun setUserDisplayName(name: String) = update {
        it.copy(userDisplayName = name.trim().take(24).ifBlank { "Vecino" })
    }

    fun setUsdRate(rate: Double) = update {
        if (rate > 0) {
            viewModelScope.launch {
                app.achievementsRepository.unlock(com.antigastos.boludos.domain.AchievementsCatalog.USD_TRACKER)
            }
        }
        it.copy(usdRate = rate.coerceAtLeast(0.0))
    }

    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            app.repository.deleteEverything()
            _toast.emit("Borré todos los gastos. Las categorías se reseteán.")
            onDone()
        }
    }

    private fun update(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            app.settingsRepository.update(transform)
            WorkSchedulers.applyFromSettings(app, app.settingsRepository.getSnapshot())
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(app) as T
                }
            }
    }
}
