package com.antigastos.boludos.data

import android.util.Log
import com.antigastos.boludos.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Cliente multi-fuente:
 *  1. Giphy (recomendado, sigue activo) si hay GIPHY_API_KEY
 *  2. Tenor v2 (deprecado, cierra 30/06/2026) si hay TENOR_API_KEY
 *  3. Klipy (drop-in para Tenor) si hay KLIPY_API_KEY
 *
 * Si no hay ninguna key, devuelve null y la UI cae a un sticker animado en Compose.
 */
class GifRepository(
    private val client: OkHttpClient = defaultClient(),
) {

    private val cache = ConcurrentHashMap<String, List<String>>()

    suspend fun randomGifUrl(keyword: String): String? = withContext(Dispatchers.IO) {
        val normalized = keyword.trim().ifBlank { "argentina meme" }
        val urls = cache[normalized] ?: fetchAny(normalized).also { fetched ->
            if (fetched.isNotEmpty()) cache[normalized] = fetched
        }
        if (urls.isEmpty()) null else urls[Random.nextInt(urls.size)]
    }

    private fun fetchAny(keyword: String): List<String> {
        val giphy = BuildConfig.GIPHY_API_KEY
        if (giphy.isNotBlank()) {
            val r = fetchGiphy(keyword, giphy)
            if (r.isNotEmpty()) return r
        }
        val tenor = BuildConfig.TENOR_API_KEY
        if (tenor.isNotBlank()) {
            val r = fetchTenor(keyword, tenor)
            if (r.isNotEmpty()) return r
        }
        val klipy = BuildConfig.KLIPY_API_KEY
        if (klipy.isNotBlank()) {
            val r = fetchKlipy(keyword, klipy)
            if (r.isNotEmpty()) return r
        }
        return emptyList()
    }

    private fun fetchGiphy(keyword: String, key: String): List<String> = try {
        val q = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://api.giphy.com/v1/gifs/search?api_key=$key&q=$q&limit=25&offset=0&rating=pg-13&lang=es"
        Log.d(TAG, "Giphy → $keyword")
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@use emptyList()
            val body = resp.body?.string() ?: return@use emptyList()
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: return@use emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val r = data.optJSONObject(i) ?: continue
                    val images = r.optJSONObject("images") ?: continue
                    val downsized = images.optJSONObject("downsized")?.optString("url")
                    val original = images.optJSONObject("original")?.optString("url")
                    val pick = downsized?.takeIf { it.isNotBlank() } ?: original?.takeIf { it.isNotBlank() }
                    if (pick != null) add(pick)
                }
            }
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Giphy fail", t)
        emptyList()
    }

    private fun fetchTenor(keyword: String, key: String): List<String> = try {
        val q = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://tenor.googleapis.com/v2/search?q=$q&key=$key" +
            "&client_key=antigastosboludos&country=AR&locale=es_AR" +
            "&limit=25&random=true&media_filter=tinygif,gif&contentfilter=medium"
        Log.d(TAG, "Tenor → $keyword")
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@use emptyList()
            val body = resp.body?.string() ?: return@use emptyList()
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return@use emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val r = results.optJSONObject(i) ?: continue
                    val media = r.optJSONObject("media_formats") ?: continue
                    val tiny = media.optJSONObject("tinygif")?.optString("url")
                    val gif = media.optJSONObject("gif")?.optString("url")
                    val pick = tiny?.takeIf { it.isNotBlank() } ?: gif?.takeIf { it.isNotBlank() }
                    if (pick != null) add(pick)
                }
            }
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Tenor fail", t)
        emptyList()
    }

    private fun fetchKlipy(keyword: String, key: String): List<String> = try {
        val q = URLEncoder.encode(keyword, "UTF-8")
        val url = "https://api.klipy.com/api/v1/$key/gifs/search?q=$q&per_page=25&page=1&locale=es-AR"
        Log.d(TAG, "Klipy → $keyword")
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@use emptyList()
            val body = resp.body?.string() ?: return@use emptyList()
            val root = JSONObject(body)
            val data = root.optJSONObject("data")?.optJSONArray("data") ?: return@use emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val r = data.optJSONObject(i) ?: continue
                    val file = r.optJSONObject("file")?.optJSONObject("md")
                        ?: r.optJSONObject("file")?.optJSONObject("hd")
                        ?: continue
                    val pick = file.optString("gif")
                    if (pick.isNotBlank()) add(pick)
                }
            }
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Klipy fail", t)
        emptyList()
    }

    fun anyKeyConfigured(): Boolean =
        BuildConfig.GIPHY_API_KEY.isNotBlank() ||
            BuildConfig.TENOR_API_KEY.isNotBlank() ||
            BuildConfig.KLIPY_API_KEY.isNotBlank()

    companion object {
        private const val TAG = "GifRepository"
        private fun defaultClient() = OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
