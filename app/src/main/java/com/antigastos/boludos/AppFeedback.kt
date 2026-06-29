package com.antigastos.boludos

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

object AppFeedback {

    private const val TAG = "AppFeedback"

    /**
     * Tipos semánticos de feedback. En Android 31+ los mapeamos a
     * [VibrationEffect] predefinidos (CLICK / TICK / DOUBLE_CLICK / HEAVY_CLICK)
     * que se sienten más naturales y respetan el perfil de háptica del OEM.
     * En 26-30 caemos a one-shots con duración aproximada.
     */
    private enum class HapticKind { CLICK, TICK, DOUBLE_CLICK, HEAVY_CLICK }

    /** Pequeño tick háptico + beep corto al guardar un gasto nuevo. */
    fun expenseSaved(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.CLICK)
        if (soundEnabled) playAck()
    }

    /** Tick más liviano para clicks importantes (votos, atajos del Home). */
    fun clickTick(ctx: Context) {
        haptic(ctx, HapticKind.TICK)
    }

    fun rouletteSpin(ctx: Context) {
        haptic(ctx, HapticKind.CLICK)
    }

    fun rouletteReveal(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.HEAVY_CLICK)
        if (soundEnabled) playAck()
    }

    fun achievementUnlocked(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.DOUBLE_CLICK)
        if (soundEnabled) playAck()
    }

    fun streakShieldUsed(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.HEAVY_CLICK)
        if (soundEnabled) playAck()
    }

    /** Día en curso sin gastos (una vez por día natural). */
    fun cleanDayClosed(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.DOUBLE_CLICK)
        if (soundEnabled) playAck()
    }

    fun quizOptionTap(ctx: Context) {
        clickTick(ctx)
    }

    fun shareAction(ctx: Context) {
        clickTick(ctx)
    }

    fun personaShuffle(ctx: Context) {
        haptic(ctx, HapticKind.TICK)
    }

    /** Drop ~1% al guardar gasto. */
    fun legendaryDrop(ctx: Context, soundEnabled: Boolean) {
        haptic(ctx, HapticKind.HEAVY_CLICK)
        if (soundEnabled) {
            playAck()
            // segundo tono corto para que se sienta “épico”
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        120,
                    )
                }
            }, 140)
        }
    }

    private fun resolveVibrator(ctx: Context): Vibrator? {
        val app = ctx.applicationContext
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = ContextCompat.getSystemService(app, VibratorManager::class.java)
                mgr?.defaultVibrator
                    ?: ContextCompat.getSystemService(app, Vibrator::class.java)
            } else {
                ContextCompat.getSystemService(app, Vibrator::class.java)
            }
        }.getOrNull()
    }

    private fun haptic(ctx: Context, kind: HapticKind) {
        val vib = resolveVibrator(ctx) ?: return
        if (!vib.hasVibrator()) return
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val effectId = when (kind) {
                        HapticKind.CLICK -> VibrationEffect.EFFECT_CLICK
                        HapticKind.TICK -> VibrationEffect.EFFECT_TICK
                        HapticKind.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
                        HapticKind.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
                    }
                    vib.vibrate(VibrationEffect.createPredefined(effectId))
                }
                else -> {
                    val durationMs = when (kind) {
                        HapticKind.TICK -> 18L
                        HapticKind.CLICK -> 35L
                        HapticKind.DOUBLE_CLICK -> 60L
                        HapticKind.HEAVY_CLICK -> 90L
                    }
                    vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }.onFailure { Log.w(TAG, "vibrate falló: ${it.message}") }
    }

    private fun playAck() {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55).startTone(
                ToneGenerator.TONE_PROP_ACK,
                90,
            )
        }.onFailure { Log.w(TAG, "tone falló: ${it.message}") }
    }
}
