package com.antigastos.boludos.data

import com.antigastos.boludos.BuildConfig
import com.antigastos.boludos.data.local.entity.SettingsEntity

/**
 * Resuelve qué API key de Gemini usar.
 *
 * Prioridad:
 *  1. La que viajó embebida en el APK vía BuildConfig (paga el dev, no
 *     el usuario final). Configurala en `local.properties` como
 *     `GEMINI_API_KEY=AIza...`.
 *  2. La que el usuario haya pegado manualmente en Ajustes (legacy).
 *
 * Si ninguna de las dos existe, devuelve string vacío y el llamador debe
 * caer al catálogo local sin IA.
 */
object GeminiCredentials {

    /** True si en este APK viaja una key embebida (no requiere acción del usuario). */
    val isBundled: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Firebase AI Logic (backend Gemini Developer API) inicializó bien en este
     * dispositivo. Lo actualiza [AntiGastosApplication] al crear
     * [FirebaseAiClient]. Cuando es true, el usuario no necesita pegar API key.
     */
    @Volatile
    var firebaseAiReady: Boolean = false
        internal set

    fun resolve(settings: SettingsEntity): String {
        val bundled = BuildConfig.GEMINI_API_KEY
        if (bundled.isNotBlank()) return bundled
        return settings.geminiApiKey
    }

    /**
     * Indica si tiene sentido usar IA en la nube: toggle on + (key embebida |
     * Vertex listo | key pegada por el usuario).
     */
    fun isEffectiveEnabled(settings: SettingsEntity): Boolean {
        if (!settings.aiPhrasesEnabled) return false
        if (isBundled) return true
        return firebaseAiReady || settings.geminiApiKey.isNotBlank()
    }
}
