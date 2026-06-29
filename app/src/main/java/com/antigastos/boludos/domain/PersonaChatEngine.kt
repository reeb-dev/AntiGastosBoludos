package com.antigastos.boludos.domain

import com.antigastos.boludos.data.FirebaseAiClient
import com.antigastos.boludos.data.GeminiApi
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import com.antigastos.boludos.domain.ConsejoKind
import com.antigastos.boludos.domain.RuletaCatalog
import com.antigastos.boludos.domain.chat.LocalPersonaEngine

/**
 * Engine que toma:
 *  - persona elegida
 *  - contexto financiero del usuario
 *  - historial del chat
 *  - último mensaje del user
 * y le pide a Gemini una respuesta en personaje.
 *
 * Si la API key no está configurada o la red falla, devuelve una respuesta
 * fallback usando los packs locales del personaje (`PersonaCopyPacks`) para
 * que el chat NUNCA quede vacío.
 *
 * Orden de red: primero **Vertex / Firebase AI Logic** si está listo; si falla
 * o no hay Vertex, usa **GeminiApi** (REST) con la API key resuelta por
 * [com.antigastos.boludos.data.GeminiCredentials].
 */
class PersonaChatEngine(
    private val gemini: GeminiApi,
    private val firebaseAi: FirebaseAiClient?,
) {

    sealed interface Reply {
        data class FromAi(val text: String) : Reply
        data class Fallback(val text: String, val reason: String) : Reply
    }

    suspend fun reply(
        personaKey: String,
        ctx: CopyContext?,
        history: List<ChatMessageEntity>,
        userMessage: String,
        apiKey: String,
    ): Reply {
        val systemPrompt = PersonaPrompts.chatPrompt(personaKey, ctx)
        // Para Gemini el role es "user" o "model".
        val turns = history.map {
            GeminiApi.Turn(
                role = if (it.role == "persona") "model" else "user",
                text = it.text,
            )
        }

        val res = viaCloud(
            apiKey,
            vertex = { client ->
                client.chat(
                    systemInstruction = systemPrompt,
                    history = turns,
                    userMessage = userMessage,
                    maxOutputTokens = 220,
                    temperature = 0.95,
                )
            },
            rest = { key ->
                gemini.chat(
                    systemInstruction = systemPrompt,
                    history = turns,
                    userMessage = userMessage,
                    apiKey = key,
                )
            },
        )

        return when (res) {
            is GeminiApi.Result.Ok -> Reply.FromAi(res.text)
            is GeminiApi.Result.Err -> Reply.Fallback(
                text = localFallback(personaKey, userMessage, ctx),
                reason = res.message,
            )
        }
    }

    /** Frase suelta para Home/Ruleta (sin historial). */
    suspend fun shortPhrase(
        personaKey: String,
        ctx: CopyContext,
        mood: CopyMood,
        apiKey: String,
    ): Reply {
        val prompt = PersonaPrompts.shortPhrasePrompt(personaKey, ctx, mood)
        val res = viaCloud(
            apiKey,
            vertex = {
                it.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Tirá la frase, una sola.",
                    maxOutputTokens = 90,
                    temperature = 0.95,
                )
            },
            rest = { key ->
                gemini.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Tirá la frase, una sola.",
                    apiKey = key,
                    maxOutputTokens = 90,
                )
            },
        )
        return when (res) {
            is GeminiApi.Result.Ok -> Reply.FromAi(res.text)
            is GeminiApi.Result.Err -> {
                val text = LocalPersonaEngine.shortPhrase(personaKey, ctx, mood)
                Reply.Fallback(text = text, reason = res.message)
            }
        }
    }

    suspend fun ruletaPhrase(
        personaKey: String,
        ctx: CopyContext,
        tipo: RuletaCatalog.Tipo,
        basePhrase: String,
        apiKey: String,
    ): Reply {
        val prompt = PersonaPrompts.ruletaPhrasePrompt(personaKey, ctx, tipo, basePhrase)
        val res = viaCloud(
            apiKey,
            vertex = {
                it.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Reescribí la frase ahora.",
                    maxOutputTokens = 120,
                    temperature = 0.9,
                )
            },
            rest = { key ->
                gemini.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Reescribí la frase ahora.",
                    apiKey = key,
                    maxOutputTokens = 120,
                )
            },
        )
        return when (res) {
            is GeminiApi.Result.Ok -> Reply.FromAi(res.text)
            is GeminiApi.Result.Err -> Reply.Fallback(text = basePhrase, reason = res.message)
        }
    }

    suspend fun rewriteConsejoBody(
        personaKey: String,
        ctx: CopyContext,
        kind: ConsejoKind,
        title: String,
        body: String,
        apiKey: String,
    ): Reply {
        val prompt = PersonaPrompts.consejoBodyRewritePrompt(personaKey, ctx, kind, title, body)
        val res = viaCloud(
            apiKey,
            vertex = {
                it.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Reescribí solo el cuerpo.",
                    maxOutputTokens = 220,
                    temperature = 0.9,
                )
            },
            rest = { key ->
                gemini.oneShot(
                    systemInstruction = prompt,
                    userMessage = "Reescribí solo el cuerpo.",
                    apiKey = key,
                    maxOutputTokens = 220,
                    temperature = 0.9,
                )
            },
        )
        return when (res) {
            is GeminiApi.Result.Ok -> Reply.FromAi(res.text)
            is GeminiApi.Result.Err -> Reply.Fallback(text = body, reason = res.message)
        }
    }

    private suspend fun viaCloud(
        apiKey: String,
        vertex: suspend (FirebaseAiClient) -> GeminiApi.Result,
        rest: suspend (String) -> GeminiApi.Result,
    ): GeminiApi.Result {
        val client = firebaseAi
        if (client != null && client.isReady) {
            val vr = vertex(client)
            if (vr is GeminiApi.Result.Ok) return vr
        }
        if (apiKey.isBlank()) {
            val hadVertex = client?.isReady == true
            val msg = if (hadVertex) {
                "La nube (Vertex) falló y no hay API key de respaldo."
            } else {
                "Sin API key de Gemini."
            }
            return GeminiApi.Result.Err(GeminiApi.ErrorKind.AUTH, msg)
        }
        return rest(apiKey)
    }

    private fun localFallback(personaKey: String, userMessage: String, ctx: CopyContext?): String =
        LocalPersonaEngine.chatReply(
            personaKey = personaKey,
            ctx = ctx,
            userMessage = userMessage,
        )
}
