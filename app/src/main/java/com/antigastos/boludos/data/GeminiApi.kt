package com.antigastos.boludos.data

import android.util.Log
import com.antigastos.boludos.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente mínimo para Google Gemini (REST). Usa el endpoint
 * `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.
 *
 * No depende de SDKs propietarios: solo OkHttp + JSONObject. La API key va en el
 * header `X-goog-api-key` (recomendado en la doc REST).
 *
 * Modelo por defecto: `gemini-flash-latest` (alias estable a la última Flash),
 * con buen español rioplatense.
 */
class GeminiApi(
    private val client: OkHttpClient = defaultClient,
    private val model: String = DEFAULT_MODEL,
) {

    /** Mensaje del historial: role debe ser "user" o "model" para Gemini. */
    data class Turn(val role: String, val text: String)

    /**
     * Resultado de una llamada. `error` es null si todo bien.
     * Si `error == ErrorKind.RATE_LIMIT` o `NETWORK`, conviene reintentar luego.
     */
    sealed interface Result {
        data class Ok(val text: String) : Result
        data class Err(val kind: ErrorKind, val message: String) : Result
    }

    enum class ErrorKind { NETWORK, AUTH, RATE_LIMIT, BLOCKED, EMPTY, UNKNOWN }

    /**
     * @param systemInstruction descripción del personaje (system prompt).
     * @param history historial de la conversación.
     * @param userMessage mensaje nuevo del usuario.
     * @param apiKey la API key del usuario.
     * @param maxOutputTokens cuánto puede responder.
     * @param temperature creatividad (0..1). 0.95 para que el lunfardo varíe.
     */
    suspend fun chat(
        systemInstruction: String,
        history: List<Turn>,
        userMessage: String,
        apiKey: String,
        maxOutputTokens: Int = 220,
        temperature: Double = 0.95,
    ): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.Err(ErrorKind.AUTH, "Falta la API key de Gemini.")
        }

        val payload = buildPayload(
            systemInstruction = systemInstruction,
            history = history,
            userMessage = userMessage,
            maxOutputTokens = maxOutputTokens,
            temperature = temperature,
        )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val req = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .header("X-goog-api-key", apiKey)
            .build()

        val response = try {
            client.newCall(req).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Network error: ${e.message}")
            return@withContext Result.Err(ErrorKind.NETWORK, e.message ?: "Sin red")
        }

        response.use { res ->
            val bodyStr = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "HTTP ${res.code}: $bodyStr")
                } else {
                    Log.w(TAG, "HTTP ${res.code}")
                }
                return@withContext when (res.code) {
                    400, 401, 403 -> Result.Err(ErrorKind.AUTH, "API key inválida o sin permiso.")
                    429 -> Result.Err(ErrorKind.RATE_LIMIT, "Te pasaste del límite gratis. Probá en un rato.")
                    else -> Result.Err(ErrorKind.UNKNOWN, "HTTP ${res.code}")
                }
            }
            val text = parseText(bodyStr)
            if (text.isNullOrBlank()) {
                val finishReason = parseFinishReason(bodyStr)
                return@withContext if (finishReason == "SAFETY" || finishReason == "PROHIBITED_CONTENT") {
                    Result.Err(ErrorKind.BLOCKED, "Gemini bloqueó la respuesta por seguridad.")
                } else {
                    Result.Err(ErrorKind.EMPTY, "Respuesta vacía.")
                }
            }
            Result.Ok(text.trim())
        }
    }

    /** Una sola "frase" sin historial: ideal para Home/Ruleta. */
    suspend fun oneShot(
        systemInstruction: String,
        userMessage: String,
        apiKey: String,
        maxOutputTokens: Int = 90,
        temperature: Double = 1.0,
    ): Result = chat(
        systemInstruction = systemInstruction,
        history = emptyList(),
        userMessage = userMessage,
        apiKey = apiKey,
        maxOutputTokens = maxOutputTokens,
        temperature = temperature,
    )

    private fun buildPayload(
        systemInstruction: String,
        history: List<Turn>,
        userMessage: String,
        maxOutputTokens: Int,
        temperature: Double,
    ): String {
        val root = JSONObject()
        if (systemInstruction.isNotBlank()) {
            root.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction)),
                ),
            )
        }
        val contents = JSONArray()
        for (turn in history) {
            contents.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text))),
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage))),
        )
        root.put("contents", contents)

        root.put(
            "generationConfig",
            JSONObject()
                .put("temperature", temperature)
                .put("topP", 0.95)
                .put("maxOutputTokens", maxOutputTokens),
        )

        // Safety: dejamos que pase humor pesado pero no contenido peligroso de verdad.
        val safety = JSONArray()
        listOf("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT")
            .forEach { cat ->
                safety.put(
                    JSONObject()
                        .put("category", cat)
                        .put("threshold", "BLOCK_ONLY_HIGH"),
                )
            }
        root.put("safetySettings", safety)

        return root.toString()
    }

    private fun parseText(body: String): String? {
        return try {
            val obj = JSONObject(body)
            val candidates = obj.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            buildString {
                for (i in 0 until parts.length()) {
                    val t = parts.getJSONObject(i).optString("text", "")
                    if (t.isNotEmpty()) append(t)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseText failed: ${e.message}")
            null
        }
    }

    private fun parseFinishReason(body: String): String? = try {
        JSONObject(body)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optString("finishReason")
    } catch (_: Exception) { null }

    companion object {
        private const val TAG = "GeminiApi"
        const val DEFAULT_MODEL = "gemini-flash-latest"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
