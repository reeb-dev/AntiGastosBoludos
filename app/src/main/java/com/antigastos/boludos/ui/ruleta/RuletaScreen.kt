package com.antigastos.boludos.ui.ruleta

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate as rotateScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import android.app.Activity
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.ads.AdHelper
import com.antigastos.boludos.data.MemeApi
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.RuletaCatalog
import com.antigastos.boludos.ui.common.ArgSticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RuletaScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val today = LocalDate.now().toString()
    var current by remember(settings.lastRouletteDay, settings.lastRouletteText) {
        mutableStateOf(
            if (settings.lastRouletteDay == today && settings.lastRouletteText.isNotBlank()) {
                RuletaCatalog.Frase(RuletaCatalog.Tipo.CONSEJO, settings.lastRouletteText)
            } else {
                null
            },
        )
    }

    // Seed para el sticker local: cambia cada giro.
    var stickerSeed by remember { mutableLongStateOf(System.nanoTime()) }

    // Meme remoto (solo si el modo local está apagado)
    var meme by remember { mutableStateOf<MemeApi.Meme?>(null) }
    var fetchingMeme by remember { mutableStateOf(false) }
    var memeFailed by remember { mutableStateOf(false) }

    var spinTarget by remember { mutableFloatStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var spinSeq by remember { mutableIntStateOf(0) }
    /** True si el usuario ganó un giro extra viendo un video rewarded. */
    var extraSpinGranted by remember { mutableStateOf(false) }
    /** True si el usuario ya usó el giro extra de hoy. */
    var extraSpinUsed by remember { mutableStateOf(false) }

    val angle by animateFloatAsState(
        targetValue = spinTarget,
        animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        label = "spin",
    )

    LaunchedEffect(angle, spinTarget) {
        if (isSpinning && angle >= spinTarget - 0.1f) {
            isSpinning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "La Ruleta del Destino 🎰",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Una vuelta por día. Te puede tocar un desafío, un insulto, una predicción falopa o un consejo serio.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Wheel(angle = angle, modifier = Modifier.fillMaxSize())
            Text("🎯", style = MaterialTheme.typography.headlineMedium)
        }

        val alreadySpunToday = settings.lastRouletteDay == today && current != null
        val canSpin = !isSpinning && (!alreadySpunToday || (extraSpinGranted && !extraSpinUsed))

        Button(
            onClick = {
                if (isSpinning) return@Button
                if (alreadySpunToday && extraSpinGranted && !extraSpinUsed) {
                    extraSpinUsed = true
                }
                AppFeedback.rouletteSpin(ctx)
                isSpinning = true
                memeFailed = false
                spinSeq += 1
                val thisSpin = spinSeq
                val spinNonce = java.security.SecureRandom().nextLong() xor System.nanoTime()
                val frase = RuletaCatalog.spin()
                spinTarget += (3..6).random() * 360f + (0..359).random()
                stickerSeed = spinNonce

                if (!settings.localStickersOnly) {
                    scope.launch {
                        fetchingMeme = true
                        meme = null
                        val fetched = app.memeApi.randomArgMeme()
                        fetchingMeme = false
                        if (fetched == null) memeFailed = true else meme = fetched
                    }
                } else {
                    meme = null
                    fetchingMeme = false
                    memeFailed = false
                }

                scope.launch {
                    delay(2200)
                    if (thisSpin != spinSeq) return@launch
                    current = frase
                    AppFeedback.rouletteReveal(ctx, settings.soundEnabled)
                    app.settingsRepository.update {
                        it.copy(
                            lastRouletteDay = today,
                            lastRouletteText = frase.texto,
                        )
                    }

                    val snap = app.settingsRepository.getSnapshot()
                    val aiKey = com.antigastos.boludos.data.GeminiCredentials.resolve(snap)
                    if (com.antigastos.boludos.data.GeminiCredentials.isEffectiveEnabled(snap)) {
                        val ctx = runCatching { app.repository.snapshotCopyContext() }.getOrNull()
                        if (ctx != null && thisSpin == spinSeq) {
                            val mood = moodForTipo(frase.tipo)
                            val r = app.aiCopyOrchestrator.fraseRuleta(
                                personaKey = snap.personaKey,
                                ctx = ctx,
                                mood = mood,
                                tipo = frase.tipo,
                                aiEnabled = true,
                                apiKey = aiKey,
                                fallbackText = frase.texto,
                                baseCatalogText = frase.texto,
                                spinNonce = spinNonce,
                            )
                            if (thisSpin == spinSeq) {
                                val nueva = RuletaCatalog.Frase(frase.tipo, r.text)
                                current = nueva
                                app.settingsRepository.update { st ->
                                    st.copy(lastRouletteText = r.text)
                                }
                            }
                        }
                    }
                }
            },
            enabled = canSpin,
            modifier = Modifier.height(52.dp),
        ) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                when {
                    isSpinning -> "Girando…"
                    extraSpinGranted && !extraSpinUsed && alreadySpunToday -> "🎬 Giro extra"
                    alreadySpunToday -> "Ya tiraste hoy"
                    else -> "Tirar la ruleta"
                },
            )
        }
        if (alreadySpunToday && !isSpinning) {
            if (!extraSpinGranted && com.antigastos.boludos.domain.AdFreePolicy.shouldShowAds(settings)) {
                OutlinedButton(
                    onClick = {
                        val activity = ctx as? Activity
                        if (activity == null) {
                            scope.launch {
                                snackbar.showSnackbar("No se pudo abrir el video. Reintentá.")
                            }
                            return@OutlinedButton
                        }
                        val shown = AdHelper.showRewarded(
                            activity,
                            AdHelper.RewardedSlot.EXTRA_RULETA,
                        ) {
                            extraSpinGranted = true
                            scope.launch {
                                snackbar.showSnackbar("¡Giro extra desbloqueado!")
                            }
                        }
                        if (!shown) {
                            scope.launch {
                                snackbar.showSnackbar(AdHelper.REWARDED_NOT_READY_MESSAGE)
                            }
                        }
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("🎬 Mirá un video → tirá de nuevo")
                }
            }
            Text(
                if (extraSpinUsed) "✅ Usaste tu giro extra. Mañana hay más."
                else if (extraSpinGranted) "¡Tenés un giro extra! Tocá el botón de arriba."
                else "✅ Ya tiraste hoy. Volvé mañana o mirá un video para un giro extra.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (scaleIn(tween(360)) + fadeIn(tween(360)))
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "fraseCard",
        ) { frase ->
            if (frase == null) {
                Text(
                    "Tirá la ruleta para ver qué te dice el universo financiero.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorForTipo(frase.tipo),
                    ),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${frase.tipo.emoji} ${frase.tipo.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            frase.texto,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { app.tts.speakForPersona(settings.personaKey, frase.texto) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Escuchar")
                            }
                        }
                    }
                }
            }
        }

        val cur = current
        if (cur != null) {
            if (settings.localStickersOnly) {
                LocalStickerCard(
                    seed = stickerSeed,
                    mood = moodForTipo(cur.tipo),
                    onShuffle = { stickerSeed = System.nanoTime() },
                )
            } else {
                MemeCard(
                    meme = meme,
                    fetching = fetchingMeme,
                    failed = memeFailed,
                    onRefresh = {
                        scope.launch {
                            fetchingMeme = true
                            meme = null
                            memeFailed = false
                            val fetched = app.memeApi.randomArgMeme()
                            fetchingMeme = false
                            if (fetched == null) memeFailed = true else meme = fetched
                        }
                    },
                )
            }
        }

        SnackbarHost(snackbar)
    }
}

@Composable
private fun LocalStickerCard(seed: Long, mood: CopyMood, onShuffle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "🎨 Sticker argentino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onShuffle) {
                    Icon(Icons.Default.Refresh, contentDescription = "Otro")
                    Spacer(Modifier.size(6.dp))
                    Text("Otro")
                }
            }
            ArgSticker(
                mood = mood,
                key = seed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun MemeCard(
    meme: MemeApi.Meme?,
    fetching: Boolean,
    failed: Boolean,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "🎭 Meme del momento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onRefresh, enabled = !fetching) {
                    Icon(Icons.Default.Refresh, contentDescription = "Otro")
                    Spacer(Modifier.size(6.dp))
                    Text("Otro")
                }
            }
            when {
                fetching -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                meme != null -> {
                    SubcomposeAsyncImage(
                        model = meme.url,
                        contentDescription = meme.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        },
                        error = {
                            Text(
                                "No se pudo cargar el meme. Probá refrescar.",
                                modifier = Modifier.padding(16.dp),
                            )
                        },
                    )
                    if (meme.title.isNotBlank()) {
                        Text(meme.title, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "r/${meme.subreddit} · u/${meme.author}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                failed -> Text(
                    "Esta vez no salió. Tocá 'Otro' para probar de nuevo.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> Text(
                    "Tirá la ruleta y te buscamos un meme argentino.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun moodForTipo(tipo: RuletaCatalog.Tipo): CopyMood = when (tipo) {
    RuletaCatalog.Tipo.DESAFIO -> CopyMood.NEUTRAL
    RuletaCatalog.Tipo.INSULTO -> CopyMood.BURN
    RuletaCatalog.Tipo.PREDICCION -> CopyMood.ALARM
    RuletaCatalog.Tipo.CONSEJO -> CopyMood.CHEER
}

@Composable
private fun colorForTipo(tipo: RuletaCatalog.Tipo): Color = when (tipo) {
    RuletaCatalog.Tipo.DESAFIO -> Color(0xFFE3F2FD)
    RuletaCatalog.Tipo.INSULTO -> Color(0xFFFFEBEE)
    RuletaCatalog.Tipo.PREDICCION -> Color(0xFFF3E5F5)
    RuletaCatalog.Tipo.CONSEJO -> Color(0xFFE8F5E9)
}

@Composable
private fun Wheel(angle: Float, modifier: Modifier = Modifier) {
    val sliceColors = listOf(
        Color(0xFF6EC1E4),
        Color(0xFFFFCC4D),
        Color(0xFF6EC1E4),
        Color(0xFFFFCC4D),
        Color(0xFF6EC1E4),
        Color(0xFFFFCC4D),
        Color(0xFF6EC1E4),
        Color(0xFFFFCC4D),
    )
    Canvas(modifier = modifier.rotate(angle)) {
        val sliceAngle = 360f / sliceColors.size
        sliceColors.forEachIndexed { idx, color ->
            rotateScope(degrees = idx * sliceAngle, pivot = center) {
                val path = Path().apply {
                    moveTo(center.x, center.y)
                    val r = size.minDimension / 2f
                    arcTo(
                        rect = Rect(
                            offset = Offset(center.x - r, center.y - r),
                            size = Size(r * 2, r * 2),
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = sliceAngle,
                        forceMoveTo = false,
                    )
                    close()
                }
                drawPath(path = path, color = color)
            }
        }
    }
}
