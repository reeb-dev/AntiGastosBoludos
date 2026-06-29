package com.antigastos.boludos.domain

import android.util.Log
import com.antigastos.boludos.data.AiQuota
import com.antigastos.boludos.data.GeminiCredentials
import com.antigastos.boludos.domain.chat.LocalPersonaEngine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Coordina las llamadas a Gemini para frases del Home / Ruleta. Le suma:
 *
 *  - Cache por firma del contexto (mismo mood + mismo "evento" → misma frase, no
 *    quemamos quota cada vez que el usuario abre el Home).
 *  - Throttle global: máximo [MAX_CALLS_PER_MINUTE] requests por minuto sumando
 *    todos los orígenes (Home + Ruleta + futuras integraciones).
 *  - Fallback transparente al pack local si la IA falla, devuelto con un flag
 *    `aiUsed=false` para que la UI pueda mostrar un badge "offline / local".
 */
class AiCopyOrchestrator(
    private val engine: PersonaChatEngine,
    private val quota: AiQuota? = null,
) {

    data class Result(
        val text: String,
        val aiUsed: Boolean,
        /** Mensaje de error breve si el fallback se disparó. */
        val error: String? = null,
    )

    /** key → frase. Limitado para no crecer indefinido. */
    private val cache = object : LinkedHashMap<String, Result>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Result>?): Boolean = size > 64
    }
    private val cacheLock = Any()

    /** Timestamps recientes para el bucket de throttle. */
    private val recentCallsMs = ArrayDeque<Long>()
    private val callsLock = Any()

    private val inFlight = ConcurrentHashMap<String, Boolean>()
    private val lastError = AtomicLong(0L)

    /**
     * Pide una frase IA si las condiciones lo permiten; si no, devuelve el pack local.
     *
     * @param aiEnabled toggle del usuario en Settings
     * @param apiKey key de Gemini (vacía → fallback local)
     * @param fallbackText texto del pack local listo (usado si IA off, throttled o falla)
     */
    suspend fun frasePersonaje(
        personaKey: String,
        ctx: CopyContext,
        mood: CopyMood,
        aiEnabled: Boolean,
        apiKey: String,
        fallbackText: String,
        cacheBucket: String = "home",
    ): Result {
        // Sin IA o sin forma de llegar a la nube (Vertex + key vacía): local.
        if (!aiEnabled || (!GeminiCredentials.firebaseAiReady && apiKey.isBlank())) {
            return Result(
                text = LocalPersonaEngine.shortPhrase(personaKey, ctx, mood),
                aiUsed = false,
            )
        }

        val key = signature(cacheBucket, personaKey, mood, ctx)

        synchronized(cacheLock) {
            cache[key]?.let { return it }
        }

        if (!throttleOk()) {
            Log.i(TAG, "Throttle activo, devolvemos pack local sin tocar la red.")
            return Result(text = fallbackText, aiUsed = false, error = "Throttle")
        }
        if (quota != null && !quota.tryConsume()) {
            Log.i(TAG, "Cupo diario IA al tope, pack local hasta mañana.")
            return Result(text = fallbackText, aiUsed = false, error = "Cupo IA del día completo")
        }

        // Evitar dos llamadas simultáneas a la misma firma.
        if (inFlight.putIfAbsent(key, true) != null) {
            return Result(text = fallbackText, aiUsed = false, error = "Pidiendo en paralelo, usamos local")
        }

        try {
            registerCall()
            val reply = engine.shortPhrase(personaKey, ctx, mood, apiKey)
            val r = when (reply) {
                is PersonaChatEngine.Reply.FromAi -> Result(text = reply.text, aiUsed = true)
                is PersonaChatEngine.Reply.Fallback -> Result(
                    text = LocalPersonaEngine.shortPhrase(personaKey, ctx, mood),
                    aiUsed = false,
                    error = reply.reason,
                )
            }
            synchronized(cacheLock) { cache[key] = r }
            if (!r.aiUsed) lastError.set(System.currentTimeMillis())
            return r
        } finally {
            inFlight.remove(key)
        }
    }

    /**
     * Frase de la ruleta: reescribe el texto del catálogo con la voz del personaje.
     * Cache y throttle compartidos con el Home ([frasePersonaje]).
     */
    suspend fun fraseRuleta(
        personaKey: String,
        ctx: CopyContext,
        mood: CopyMood,
        tipo: RuletaCatalog.Tipo,
        aiEnabled: Boolean,
        apiKey: String,
        fallbackText: String,
        baseCatalogText: String,
        /** Distinto en cada giro → la IA no devuelve siempre la misma frase cacheada. */
        spinNonce: Long = 0L,
    ): Result {
        if (!aiEnabled || (!GeminiCredentials.firebaseAiReady && apiKey.isBlank())) {
            return Result(text = fallbackText, aiUsed = false)
        }

        val key = ruletaSignature(personaKey, mood, ctx, tipo, baseCatalogText, spinNonce)

        synchronized(cacheLock) {
            cache[key]?.let { return it }
        }

        if (!throttleOk()) {
            Log.i(TAG, "Throttle activo (ruleta), pack local.")
            return Result(text = fallbackText, aiUsed = false, error = "Throttle")
        }
        if (quota != null && !quota.tryConsume()) {
            return Result(text = fallbackText, aiUsed = false, error = "Cupo IA del día completo")
        }

        if (inFlight.putIfAbsent(key, true) != null) {
            return Result(text = fallbackText, aiUsed = false, error = "Pidiendo en paralelo, usamos local")
        }

        try {
            registerCall()
            val reply = engine.ruletaPhrase(personaKey, ctx, tipo, baseCatalogText, apiKey)
            val r = when (reply) {
                is PersonaChatEngine.Reply.FromAi -> Result(text = reply.text, aiUsed = true)
                is PersonaChatEngine.Reply.Fallback -> Result(
                    text = fallbackText,
                    aiUsed = false,
                    error = reply.reason,
                )
            }
            synchronized(cacheLock) { cache[key] = r }
            if (!r.aiUsed) lastError.set(System.currentTimeMillis())
            return r
        } finally {
            inFlight.remove(key)
        }
    }

    /** Reescribe el cuerpo del modal de consejos manteniendo [ConsejoKind] en UI. */
    suspend fun consejoBody(
        personaKey: String,
        ctx: CopyContext,
        consejoId: String,
        kind: ConsejoKind,
        title: String,
        body: String,
        aiEnabled: Boolean,
        apiKey: String,
    ): Result {
        if (!aiEnabled || (!GeminiCredentials.firebaseAiReady && apiKey.isBlank())) {
            return Result(
                text = LocalPersonaEngine.consejoBody(personaKey, title, body),
                aiUsed = false,
            )
        }

        val key = listOf(
            "consejo",
            personaKey,
            consejoId,
            kind.name,
            body,
            ctx.monthLabel,
            ctx.totalLucas / 5,
        ).joinToString("|")

        synchronized(cacheLock) {
            cache[key]?.let { return it }
        }

        if (!throttleOk()) {
            return Result(text = body, aiUsed = false, error = "Throttle")
        }
        if (quota != null && !quota.tryConsume()) {
            return Result(text = body, aiUsed = false, error = "Cupo IA del día completo")
        }

        if (inFlight.putIfAbsent(key, true) != null) {
            return Result(text = body, aiUsed = false, error = "Pidiendo en paralelo")
        }

        try {
            registerCall()
            val reply = engine.rewriteConsejoBody(personaKey, ctx, kind, title, body, apiKey)
            val r = when (reply) {
                is PersonaChatEngine.Reply.FromAi -> Result(text = reply.text, aiUsed = true)
                is PersonaChatEngine.Reply.Fallback -> Result(text = body, aiUsed = false, error = reply.reason)
            }
            synchronized(cacheLock) { cache[key] = r }
            if (!r.aiUsed) lastError.set(System.currentTimeMillis())
            return r
        } finally {
            inFlight.remove(key)
        }
    }

    private fun ruletaSignature(
        personaKey: String,
        mood: CopyMood,
        ctx: CopyContext,
        tipo: RuletaCatalog.Tipo,
        baseCatalogText: String,
        spinNonce: Long,
    ): String = listOf(
        "ruleta",
        personaKey,
        mood.name,
        tipo.name,
        baseCatalogText,
        ctx.monthLabel,
        ctx.totalLucas / 5,
        spinNonce.toString(),
    ).joinToString("|")

    /** Limpia la cache (lo usamos cuando el usuario cambia personaje en Settings). */
    fun invalidate() {
        synchronized(cacheLock) { cache.clear() }
    }

    /** Cuántas frases IA quedan hoy antes del fallback automático. */
    fun remainingDailyQuota(): Int = quota?.remaining() ?: AiQuota.DAILY_CAP

    private fun throttleOk(): Boolean {
        val now = System.currentTimeMillis()
        synchronized(callsLock) {
            while (recentCallsMs.isNotEmpty() && now - recentCallsMs.first() > 60_000L) {
                recentCallsMs.removeFirst()
            }
            return recentCallsMs.size < MAX_CALLS_PER_MINUTE
        }
    }

    private fun registerCall() {
        synchronized(callsLock) {
            recentCallsMs.addLast(System.currentTimeMillis())
        }
    }

    private fun signature(bucket: String, persona: String, mood: CopyMood, ctx: CopyContext): String =
        listOf(
            bucket,
            persona,
            mood.name,
            ctx.monthLabel,
            ctx.totalLucas / 5, // bucket de 5k lucas para no invalidar por cada peso
            ctx.expenseCount,
            ctx.daySpentLucas / 5,
            ctx.lastExpenseSlug ?: "-",
            ctx.topCategorySlug ?: "-",
        ).joinToString("|")

    companion object {
        private const val TAG = "AiCopyOrchestrator"
        /** 8 = bien por debajo del free tier (15 req/min) y deja margen para retries. */
        private const val MAX_CALLS_PER_MINUTE = 8
    }
}
