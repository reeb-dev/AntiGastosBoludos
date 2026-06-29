package com.antigastos.boludos.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente para meme-api.com. Es gratis, sin auth.
 * Tira `https://meme-api.com/gimme/{subreddit}/{count}` y devuelve URLs de imágenes.
 *
 * Usamos subreddits argentinos / hispanoparlantes con humor del lado de acá.
 * NSFW se filtra siempre.
 */
class MemeApi(
    private val client: OkHttpClient = defaultClient,
) {

    data class Meme(
        val title: String,
        val url: String,
        val subreddit: String,
        val author: String,
    )

    /** Trae 1 meme random de un subreddit argentino al azar. */
    suspend fun randomArgMeme(): Meme? = withContext(Dispatchers.IO) {
        val sub = ARG_SUBREDDITS.random()
        runCatching { fetchOne(sub) }.getOrNull().also {
            if (it == null) Log.w(TAG, "no meme from r/$sub")
        }
    }

    /** Trae varios memes de un subreddit. */
    suspend fun manyArgMemes(count: Int = 5): List<Meme> = withContext(Dispatchers.IO) {
        val sub = ARG_SUBREDDITS.random()
        runCatching { fetchMany(sub, count.coerceIn(1, 50)) }.getOrElse {
            Log.w(TAG, "manyArgMemes failed", it)
            emptyList()
        }
    }

    private fun fetchOne(sub: String): Meme? {
        val req = Request.Builder()
            .url("https://meme-api.com/gimme/$sub")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val obj = JSONObject(body)
            return obj.toMemeOrNull()
        }
    }

    private fun fetchMany(sub: String, count: Int): List<Meme> {
        val req = Request.Builder()
            .url("https://meme-api.com/gimme/$sub/$count")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val obj = JSONObject(body)
            val arr = obj.optJSONArray("memes") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.toMemeOrNull()
            }
        }
    }

    private fun JSONObject.toMemeOrNull(): Meme? {
        if (optBoolean("nsfw") || optBoolean("spoiler")) return null
        val url = optString("url").orEmpty()
        if (url.isBlank()) return null
        // Filtramos por extensiones de imagen para no traer videos.
        val low = url.lowercase()
        val isImage = low.endsWith(".jpg") || low.endsWith(".jpeg") ||
            low.endsWith(".png") || low.endsWith(".gif") || low.endsWith(".webp")
        if (!isImage) return null
        return Meme(
            title = optString("title").orEmpty(),
            url = url,
            subreddit = optString("subreddit").orEmpty(),
            author = optString("author").orEmpty(),
        )
    }

    companion object {
        private const val TAG = "MemeApi"

        /**
         * Subreddits con humor argentino o hispano. `dankgentina` y `argaming`
         * son los más activos en español / criollo.
         */
        private val ARG_SUBREDDITS = listOf(
            "dankgentina",
            "argaming",
            "futbolargento",
            "RepublicaArgentina",
            "MAAU",
            "argentina",
            "memesESP",
            "SpanishMeme",
        )

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        }
    }
}
