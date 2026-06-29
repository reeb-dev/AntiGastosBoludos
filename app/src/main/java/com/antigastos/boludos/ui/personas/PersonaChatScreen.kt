package com.antigastos.boludos.ui.personas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.Persona
import com.antigastos.boludos.ui.theme.AgColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaChatScreen(
    viewModel: PersonaChatViewModel,
    onBack: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val typing by viewModel.typing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val mood by viewModel.mood.collectAsState()
    val context by viewModel.context.collectAsState()
    val quickPrompts by viewModel.quickPrompts.collectAsState()
    val lastReplyFromAi by viewModel.lastReplyFromAi.collectAsState()

    var draft by remember { mutableStateOf("") }
    var greeted by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Saludo automático: la primera vez que entrás y el chat está vacío,
    // el personaje tira un mensaje contextual (sin gastar IA).
    LaunchedEffect(messages.size) {
        if (!greeted && messages.isEmpty() && !typing) {
            greeted = true
            viewModel.greetIfEmpty()
        }
        if (messages.isNotEmpty()) greeted = true
    }

    LaunchedEffect(messages.size, typing) {
        val target = (messages.size - 1 + if (typing) 1 else 0).coerceAtLeast(0)
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ChatTitle(persona = viewModel.persona, typing = typing, mood = mood)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (settings.personaKey != viewModel.persona.key) {
                        IconButton(onClick = { viewModel.makePrimary() }) {
                            Icon(Icons.Default.PersonPin, contentDescription = "Hacer mi voz")
                        }
                    }
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            ChatComposer(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    val toSend = draft.trim()
                    if (toSend.isNotBlank()) {
                        viewModel.send(toSend)
                        draft = ""
                    }
                },
                quickPrompts = quickPrompts,
                onQuickPrompt = { qp ->
                    viewModel.sendQuick(qp)
                    draft = ""
                },
                enabled = !typing,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            ContextStrip(
                ctx = context,
                mood = mood,
                personaName = viewModel.persona.displayName,
            )
            if (lastError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        lastError ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (messages.isEmpty() && !typing) {
                EmptyState(
                    persona = viewModel.persona,
                    prompts = quickPrompts,
                    onUseSample = { viewModel.sendQuick(it) },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isLastPersona = msg.id == messages.lastOrNull { it.role == "persona" }?.id
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(220)) +
                                slideInVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 4 },
                                ),
                        ) {
                            MessageBubble(
                                message = msg,
                                personaEmoji = viewModel.persona.emoji,
                                onSpeak = if (msg.role == "persona") {
                                    { viewModel.speak(msg.text) }
                                } else null,
                                onRegenerate = if (isLastPersona && !typing) {
                                    { viewModel.regenerateLast() }
                                } else null,
                                showAiBadge = isLastPersona && lastReplyFromAi,
                                showOfflineBadge = isLastPersona && !lastReplyFromAi && msg.role == "persona",
                            )
                        }
                    }
                    if (typing) {
                        item { TypingBubble(viewModel.persona.emoji) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTitle(persona: Persona, typing: Boolean, mood: CopyMood) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Text(persona.emoji, style = MaterialTheme.typography.titleLarge)
            // Dot de mood en la esquina superior derecha del emoji.
            MoodIndicator(mood = mood, modifier = Modifier.size(10.dp))
        }
        Spacer(Modifier.size(8.dp))
        Column {
            Text(persona.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                if (typing) "está escribiendo…" else persona.tagline,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

/**
 * Punto pulsante con el color del [CopyMood] actual. Se ubica con un
 * `Modifier` arbitrario para que el caller decida posicionamiento.
 */
@Composable
private fun MoodIndicator(mood: CopyMood, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "moodPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "moodPulseV",
    )
    val color = when (mood) {
        CopyMood.CHEER -> AgColors.CheerGreen
        CopyMood.NEUTRAL -> Color(0xFF90CAF9)
        CopyMood.BURN -> AgColors.BurnAmber
        CopyMood.ALARM -> AgColors.AlarmRed
    }
    Box(
        modifier = modifier
            .scale(pulse)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

/**
 * Tira fina arriba del chat con el contexto financiero del usuario.
 * Aparece sólo si tenemos contexto válido (ya cargó al menos un gasto).
 */
@Composable
private fun ContextStrip(ctx: CopyContext?, mood: CopyMood, personaName: String) {
    if (ctx == null || ctx.totalLucas == 0) return
    val accent = when (mood) {
        CopyMood.CHEER -> AgColors.CheerGreen
        CopyMood.NEUTRAL -> MaterialTheme.colorScheme.primary
        CopyMood.BURN -> AgColors.BurnAmber
        CopyMood.ALARM -> AgColors.AlarmRed
    }
    val pesos = ctx.totalLucas.toLong() * 1000L
    val pctTxt = ctx.pctOfMonthlyGoal?.let { "${(it * 100).toInt()}% de tu meta" }
    Surface(
        color = accent.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$personaName ya vio tus números:",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                buildString {
                    append("📈 ${MoneyFormat.formatPesos(pesos)} en ${ctx.monthLabel}")
                    if (pctTxt != null) append(" · $pctTxt")
                    if (!ctx.topCategoryName.isNullOrBlank() && ctx.topCategoryLucas > 0) {
                        append(" · top: ${ctx.topCategoryName}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    personaEmoji: String,
    onSpeak: (() -> Unit)?,
    onRegenerate: (() -> Unit)?,
    showAiBadge: Boolean,
    showOfflineBadge: Boolean,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Text(
                personaEmoji,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 6.dp, top = 4.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 1.dp,
            modifier = Modifier
                .padding(if (isUser) PaddingValues(start = 48.dp) else PaddingValues(end = 48.dp)),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    message.text,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (showAiBadge) {
                            BubbleBadge(text = "IA", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(4.dp))
                        }
                        if (showOfflineBadge) {
                            BubbleBadge(text = "offline", color = AgColors.BurnAmber)
                            Spacer(Modifier.size(4.dp))
                        }
                        if (onSpeak != null) {
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Escuchar",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (onRegenerate != null) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Otra respuesta",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TypingBubble(emoji: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 6.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedDot(delayMs = 0)
                Spacer(Modifier.size(4.dp))
                AnimatedDot(delayMs = 160)
                Spacer(Modifier.size(4.dp))
                AnimatedDot(delayMs = 320)
            }
        }
    }
}

/**
 * Dot que sube/baja en loop y arranca con un delay propio para que los
 * 3 puntos se vean sincronizados como un "loading" estilo iMessage.
 */
@Composable
private fun AnimatedDot(delayMs: Int) {
    val infinite = rememberInfiniteTransition(label = "dotBounce")
    val s by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotBounceV",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(s)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
    )
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    quickPrompts: List<QuickPrompt>,
    onQuickPrompt: (QuickPrompt) -> Unit,
    enabled: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (quickPrompts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    quickPrompts.forEach { qp ->
                        AssistChip(
                            onClick = { if (enabled) onQuickPrompt(qp) },
                            enabled = enabled,
                            label = { Text("${qp.emoji} ${qp.label}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribile…") },
                    enabled = enabled,
                    maxLines = 4,
                )
                Spacer(Modifier.size(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = enabled && value.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (enabled && value.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    persona: Persona,
    prompts: List<QuickPrompt>,
    onUseSample: (QuickPrompt) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(persona.emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            persona.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            persona.tagline,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        Text("Probá uno de estos para arrancar:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        // Mostramos hasta 4 prompts contextuales como cards grandes.
        prompts.take(4).forEach { qp ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { onUseSample(qp) },
            ) {
                Text(
                    "${qp.emoji} ${qp.label}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
