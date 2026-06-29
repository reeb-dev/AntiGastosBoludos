package com.antigastos.boludos.ui.home.anim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Helpers de animación reutilizados por las distintas cards del Home.
 *
 * Antes vivían inline dentro de `HomeScreen.kt` y se mezclaban con la
 * lógica de la pantalla. Acá quedan agrupados para que sean fáciles
 * de testear visualmente y de tunear sin tocar el resto del Home.
 */

/**
 * Renderiza `content` con un fade + slide vertical después de `delayMs`.
 * Lo usamos para "presentar" las cards del Home una atrás de otra.
 */
@Composable
internal fun StaggeredEntry(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 360)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, easing = LinearOutSlowInEasing),
                initialOffsetY = { it / 4 },
            ) +
            expandVertically(animationSpec = tween(durationMillis = 360)),
    ) {
        content()
    }
}

/**
 * Devuelve un offset horizontal animado que sacude la UI cada vez que
 * cambia `triggerKey`. Si `amplitudeDp <= 0` no hace nada.
 */
@Composable
internal fun rememberShakeOffset(triggerKey: Int, amplitudeDp: Float): Float {
    var phase by remember { mutableStateOf(0f) }
    LaunchedEffect(triggerKey) {
        if (amplitudeDp <= 0f || triggerKey == 0) return@LaunchedEffect
        val frames = 18
        val totalMs = 520L
        val perFrame = totalMs / frames
        repeat(frames) { i ->
            val progress = i.toFloat() / frames
            val decay = 1f - progress
            phase = (sin(progress * Math.PI.toFloat() * 6f) * decay) * amplitudeDp
            delay(perFrame)
        }
        phase = 0f
    }
    return phase
}

/**
 * Pequeño "bounce" multiplicativo (escala) para enfatizar moods felices.
 */
@Composable
internal fun rememberBounceScale(triggerKey: Int, active: Boolean): Float {
    var s by remember { mutableStateOf(1f) }
    LaunchedEffect(triggerKey) {
        if (!active || triggerKey == 0) return@LaunchedEffect
        val frames = 14
        val totalMs = 420L
        val perFrame = totalMs / frames
        repeat(frames) { i ->
            val progress = i.toFloat() / frames
            val bump = sin(progress * Math.PI.toFloat()) * 0.08f
            s = 1f + bump
            delay(perFrame)
        }
        s = 1f
    }
    return s
}
