package com.antigastos.boludos.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini vía **Firebase AI Logic** con backend **Gemini Developer API**
 * (`GenerativeBackend.googleAI()`), que es el que tiene **free tier**:
 * ~15 req/min y 1500 req/día en `gemini-2.5-flash` (o el modelo configurado).
 *
 * Es distinto de Vertex AI: Vertex requiere proyecto Blaze y se factura por
 * tokens. Acá usamos el backend gratis para que el usuario final no pague nada
 * y vos no entres en facturación por uso.
 *
 * Requiere `google-services.json` en `app/`. Si Firebase no está inicializado,
 * [isReady] queda en false y el [PersonaChatEngine] cae a [GeminiApi] REST con
 * la API key (también con free tier) o, si no hay key, al pack local.
 */
class FirebaseAiClient {

    @Volatile
    var isReady: Boolean = false
        private set

    private var initError: String? = null

    private val firebaseAi: FirebaseAI? = try {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app, backend = GenerativeBackend.googleAI())
    } catch (t: Throwable) {
        Log.w(TAG, "Firebase AI (Google AI backend) no disponible: ${t.message}")
        initError = t.message
        null
    }

    init {
        isReady = firebaseAi != null
        if (!isReady) {
            Log.i(TAG, "Firebase AI (Google AI) deshabilitado: ${initError ?: "sin Firebase"}")
        }
    }

    suspend fun oneShot(
        systemInstruction: String,
        userMessage: String,
        maxOutputTokens: Int,
        temperature: Double,
    ): GeminiApi.Result = withContext(Dispatchers.IO) {
        val ai = firebaseAi ?: return@withContext GeminiApi.Result.Err(
            GeminiApi.ErrorKind.AUTH,
            "Firebase AI no configurado en este build.",
        )
        return@withContext runCatching {
            val model = ai.generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    this.maxOutputTokens = maxOutputTokens
                    this.temperature = temperature.toFloat()
                    topP = 0.95f
                },
                systemInstruction = content { text(systemInstruction) },
                safetySettings = safetyList(),
            )
            val response = model.generateContent(userMessage)
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                GeminiApi.Result.Err(GeminiApi.ErrorKind.EMPTY, "Respuesta vacía (Firebase AI).")
            } else {
                GeminiApi.Result.Ok(text)
            }
        }.getOrElse { e -> mapError(e) }
    }

    suspend fun chat(
        systemInstruction: String,
        history: List<GeminiApi.Turn>,
        userMessage: String,
        maxOutputTokens: Int,
        temperature: Double,
    ): GeminiApi.Result = withContext(Dispatchers.IO) {
        val ai = firebaseAi ?: return@withContext GeminiApi.Result.Err(
            GeminiApi.ErrorKind.AUTH,
            "Firebase AI no configurado.",
        )
        return@withContext runCatching {
            val contents = ArrayList<com.google.firebase.ai.type.Content>()
            for (t in history) {
                val role = if (t.role == "model") "model" else "user"
                contents += content(role = role) { text(t.text) }
            }
            contents += content(role = "user") { text(userMessage) }
            val model = ai.generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    this.maxOutputTokens = maxOutputTokens
                    this.temperature = temperature.toFloat()
                    topP = 0.95f
                },
                systemInstruction = content { text(systemInstruction) },
                safetySettings = safetyList(),
            )
            val response = model.generateContent(contents)
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                GeminiApi.Result.Err(GeminiApi.ErrorKind.EMPTY, "Respuesta vacía (Firebase AI).")
            } else {
                GeminiApi.Result.Ok(text)
            }
        }.getOrElse { e -> mapError(e) }
    }

    private fun mapError(e: Throwable): GeminiApi.Result.Err {
        Log.w(TAG, "Firebase AI: ${e.message}")
        val msg = e.message.orEmpty()
        return when {
            msg.contains("429", ignoreCase = true) ->
                GeminiApi.Result.Err(GeminiApi.ErrorKind.RATE_LIMIT, "Pasaste el cupo gratis. Probá en un rato.")
            msg.contains("403", ignoreCase = true) || msg.contains("401", ignoreCase = true) ->
                GeminiApi.Result.Err(GeminiApi.ErrorKind.AUTH, "Permisos: revisá Firebase AI Logic en la consola.")
            else ->
                GeminiApi.Result.Err(GeminiApi.ErrorKind.UNKNOWN, msg.ifBlank { "Error Firebase AI" })
        }
    }

    private fun safetyList() = listOf(
        SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.ONLY_HIGH),
    )

    companion object {
        private const val TAG = "FirebaseAiClient"
        /** Flash 2.5 estable. Es el que entra en el free tier del Google AI backend. */
        private const val MODEL_NAME = "gemini-2.5-flash"
    }
}
