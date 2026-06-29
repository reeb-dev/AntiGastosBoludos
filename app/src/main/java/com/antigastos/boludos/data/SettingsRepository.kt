package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val db: AppDatabase) {

    val flow: Flow<SettingsEntity> = db.settingsDao().observe().map { it ?: SettingsEntity() }

    suspend fun ensureRow() {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(SettingsEntity())
        }
    }

    suspend fun getSnapshot(): SettingsEntity = db.settingsDao().get() ?: SettingsEntity()

    suspend fun update(transform: (SettingsEntity) -> SettingsEntity) {
        val current = getSnapshot()
        db.settingsDao().upsert(transform(current))
    }

    suspend fun setOnboardingDone() = update {
        it.copy(onboardingDone = true)
    }

    suspend fun setUserDisplayName(name: String) = update {
        it.copy(userDisplayName = name.trim().take(24).ifBlank { "Vecino" })
    }

    suspend fun markPersonaQuizDone(personaKey: String) = update {
        it.copy(personaQuizDone = true, personaKey = personaKey)
    }

    suspend fun markHomeTipsShown() = update { it.copy(homeTipsShown = true) }

    suspend fun markPersonaWelcomeShown() = update { it.copy(personaWelcomeShown = true) }

    /**
     * Si la rotación está activa, elige otra persona al azar y la guarda
     * como [personaKey]. Pensado para llamarse al arranque del proceso, así
     * cada vez que el usuario abre la app le saluda y le habla otro
     * personaje distinto.
     */
    suspend fun maybeRotatePersonaOnLaunch() {
        val s = getSnapshot()
        if (!s.personaRotateEnabled) return
        val candidates = com.antigastos.boludos.domain.PersonaCatalog.all
            .filter { it.key != s.personaKey }
        val pick = candidates.randomOrNull() ?: return
        val today = java.time.LocalDate.now().toString()
        update {
            it.copy(personaKey = pick.key, lastPersonaRotationDay = today)
        }
    }

    /** Compat: alias del método anterior. */
    @Deprecated(
        "Renombrado a maybeRotatePersonaOnLaunch; ahora rota en cada arranque.",
        ReplaceWith("maybeRotatePersonaOnLaunch()"),
    )
    suspend fun maybeRotatePersonaForToday() = maybeRotatePersonaOnLaunch()

    /** Garantiza fila settings y código de referencia para donaciones por CBU. */
    suspend fun ensureDefaults() {
        ensureRow()
        ensureDonationReferenceCode()
    }

    suspend fun ensureDonationReferenceCode() {
        val s = getSnapshot()
        if (s.donationReferenceCode.isNotBlank()) return
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = "AGB-" + (1..6).map { alphabet.random() }.joinToString("")
        update { it.copy(donationReferenceCode = code) }
    }

    suspend fun consumeStreakFreezeForYesterday(): Boolean {
        val zone = java.time.ZoneId.systemDefault()
        val yesterday = java.time.LocalDate.now(zone).minusDays(1)
        val dayStr = yesterday.toEpochDay().toString()
        val before = getSnapshot()
        if (before.streakFreezeCharges <= 0) return false
        val set = before.streakForgivenEpochDaysCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        if (set.contains(dayStr)) return false
        set.add(dayStr)
        db.settingsDao().upsert(
            before.copy(
                streakFreezeCharges = before.streakFreezeCharges - 1,
                streakForgivenEpochDaysCsv = set.joinToString(","),
            ),
        )
        return true
    }
}
