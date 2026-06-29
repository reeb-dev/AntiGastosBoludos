package com.antigastos.boludos.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wrapper de TextToSpeech que se inicializa perezosamente y respeta una sola instancia
 * a nivel de Application. Usa locale es-AR si está disponible, si no es-ES, si no default.
 *
 * [speakForPersona] ajusta tono y ritmo por personaje (gato más agudo, predicador grave, etc.).
 */
class TtsSpeaker(private val appContext: Context) {

    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    private val pending = ArrayDeque<Pair<String, String?>>()

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(appContext.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val arOk = tts?.setLanguage(Locale("es", "AR"))
                if (arOk == TextToSpeech.LANG_MISSING_DATA || arOk == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val esOk = tts?.setLanguage(Locale("es", "ES"))
                    if (esOk == TextToSpeech.LANG_MISSING_DATA || esOk == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }
                resetVoiceParams()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w("TtsSpeaker", "tts error: $utteranceId code=$errorCode")
                    }
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        Log.w("TtsSpeaker", "tts error legacy: $utteranceId")
                    }
                })
                ready.set(true)
                while (pending.isNotEmpty()) {
                    val (t, p) = pending.removeFirst()
                    speakInternal(t, p)
                }
            } else {
                Log.w("TtsSpeaker", "tts init failed: $status")
            }
        }
    }

    /** Compat: voz neutra. */
    fun speak(text: String) = speakForPersona(null, text)

    fun speakForPersona(personaKey: String?, text: String) {
        if (text.isBlank()) return
        init()
        if (!ready.get()) {
            pending.addLast(text to personaKey)
            return
        }
        speakInternal(text, personaKey)
    }

    private fun resetVoiceParams() {
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.05f)
    }

    private fun applyPersona(personaKey: String?) {
        resetVoiceParams()
        when (personaKey) {
            "gato" -> {
                tts?.setPitch(1.62f)
                tts?.setSpeechRate(1.14f)
            }
            "predicador" -> {
                tts?.setPitch(0.78f)
                tts?.setSpeechRate(0.86f)
            }
            "abuela", "madre" -> {
                tts?.setPitch(1.12f)
                tts?.setSpeechRate(0.95f)
            }
            "jefe", "mecanico" -> {
                tts?.setPitch(0.92f)
                tts?.setSpeechRate(0.98f)
            }
            "streamer", "trapero" -> {
                tts?.setPitch(1.18f)
                tts?.setSpeechRate(1.18f)
            }
            "vidente" -> {
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(0.82f)
            }
            "cunadocripto" -> {
                tts?.setPitch(1.08f)
                tts?.setSpeechRate(1.12f)
            }
            "politico" -> {
                tts?.setPitch(0.88f)
                tts?.setSpeechRate(0.92f)
            }
            "diez" -> {
                tts?.setPitch(0.96f)
                tts?.setSpeechRate(0.84f)
            }
            "geniopotrero" -> {
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(1.06f)
            }
            else -> { /* neutro */ }
        }
    }

    private fun speakInternal(text: String, personaKey: String?) {
        tts?.stop()
        applyPersona(personaKey)
        val idMain = "ag-${System.currentTimeMillis()}"
        val params = Bundle()
        when (personaKey) {
            "predicador" -> {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, idMain)
                tts?.setPitch(0.64f)
                tts?.setSpeechRate(0.74f)
                tts?.speak("Amén, hermano.", TextToSpeech.QUEUE_ADD, params, "$idMain-amen")
            }
            "politico" -> {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, idMain)
                tts?.setPitch(0.78f)
                tts?.setSpeechRate(0.82f)
                tts?.speak("Muchas gracias compañeros.", TextToSpeech.QUEUE_ADD, params, "$idMain-cierre")
            }
            "diez" -> {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, idMain)
                tts?.setPitch(0.94f)
                tts?.setSpeechRate(0.8f)
                tts?.speak("Andá pa' allá, bobo.", TextToSpeech.QUEUE_ADD, params, "$idMain-cierre")
            }
            "geniopotrero" -> {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, idMain)
                tts?.setPitch(1.02f)
                tts?.setSpeechRate(1.02f)
                tts?.speak("La zurda no perdona, pa.", TextToSpeech.QUEUE_ADD, params, "$idMain-cierre")
            }
            else -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, idMain)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        ready.set(false)
    }
}
