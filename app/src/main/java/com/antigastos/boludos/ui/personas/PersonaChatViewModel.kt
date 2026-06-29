package com.antigastos.boludos.ui.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.GeminiCredentials
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.PersonaChatEngine
import com.antigastos.boludos.domain.PersonaCopyPacks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sugerencia rápida que aparece como chip en el chat.
 *
 * Es un par "label visible" + "texto que efectivamente se envía", para
 * que las sugerencias contextuales (ej. "¿qué hago con el delivery?")
 * se sientan ricas pero no terminen poniéndolas literal en el input.
 */
data class QuickPrompt(
    val emoji: String,
    val label: String,
    val message: String,
)

class PersonaChatViewModel(
    private val app: AntiGastosApplication,
    private val personaKey: String,
) : ViewModel() {

    val persona = PersonaCatalog.byKey(personaKey)

    val messages: StateFlow<List<ChatMessageEntity>> =
        app.chatRepository.observe(personaKey)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val settingsFlow: StateFlow<SettingsEntity> = app.settingsRepository.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())
    val settings: StateFlow<SettingsEntity> = settingsFlow

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Heurística de mood que mostramos como chip arriba del chat. Lo
     * derivamos del [CopyContext] en cuanto está disponible.
     */
    private val _mood = MutableStateFlow(CopyMood.NEUTRAL)
    val mood: StateFlow<CopyMood> = _mood.asStateFlow()

    /** Snapshot del contexto financiero para mostrarlo como chip. */
    private val _context = MutableStateFlow<CopyContext?>(null)
    val context: StateFlow<CopyContext?> = _context.asStateFlow()

    /** Marca si el último reply vino de IA o de fallback offline. */
    private val _lastReplyFromAi = MutableStateFlow(true)
    val lastReplyFromAi: StateFlow<Boolean> = _lastReplyFromAi.asStateFlow()

    /**
     * Sugerencias rápidas contextuales para el composer. Se mantienen
     * sincronizadas con `context` así si el usuario carga gastos durante
     * el chat, las sugerencias se actualizan solas.
     */
    val quickPrompts: StateFlow<List<QuickPrompt>> = combine(_context, _mood) { ctx, mood ->
        buildQuickPrompts(ctx, mood)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultPrompts())

    init {
        // Cargamos el contexto al abrir el chat así los chips arrancan con info real.
        viewModelScope.launch {
            val ctx = buildCtxOrNull()
            _context.value = ctx
            _mood.value = moodFor(ctx)
        }
    }

    fun send(text: String) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            _lastError.value = null
            app.chatRepository.appendUser(personaKey, cleaned)
            replyToLastUser(cleaned)
        }
    }

    /**
     * Envío directo de una sugerencia rápida. La labelizamos para que
     * en el chat aparezca el "label" del usuario y no el "message"
     * más largo si es muy verboso.
     */
    fun sendQuick(prompt: QuickPrompt) = send(prompt.message)

    /**
     * Re-pide la última respuesta del personaje. Útil cuando salió flojita
     * y querés otra toma sin volver a tipear lo mismo.
     */
    fun regenerateLast() {
        viewModelScope.launch {
            val history = app.chatRepository.history(personaKey)
            val lastUser = history.lastOrNull { it.role == "user" } ?: return@launch
            // Borramos el último persona reply (si existía) y volvemos a pedir.
            val lastPersona = history.lastOrNull { it.role == "persona" }
            if (lastPersona != null && lastPersona.id > lastUser.id) {
                app.chatRepository.deleteById(lastPersona.id)
            }
            replyToLastUser(lastUser.text)
        }
    }

    /**
     * Saludo automático cuando entrás a un chat vacío. Mete un mensaje
     * del personaje sin disparar a Gemini (offline + barato).
     */
    fun greetIfEmpty() {
        viewModelScope.launch {
            val msgs = app.chatRepository.history(personaKey)
            if (msgs.isNotEmpty()) return@launch
            val ctx = _context.value ?: buildCtxOrNull()
            val mood = moodFor(ctx)
            _mood.value = mood
            val pack = PersonaCopyPacks.pool(personaKey, mood, ctx ?: emptyContext())
            val greet = pack.randomOrNull()
                ?: "Acá ando. Tirale un mensaje y te bardéo (con onda)."
            app.chatRepository.appendPersona(personaKey, greet)
            _lastReplyFromAi.value = false
        }
    }

    fun clear() {
        viewModelScope.launch {
            app.chatRepository.clear(personaKey)
            _lastError.value = null
            _lastReplyFromAi.value = true
        }
    }

    fun makePrimary() {
        viewModelScope.launch {
            app.settingsRepository.update { it.copy(personaKey = personaKey) }
        }
    }

    fun speak(text: String) {
        if (settingsFlow.value.ttsEnabled) app.tts.speakForPersona(personaKey, text)
    }

    private suspend fun replyToLastUser(userText: String) {
        val s = app.settingsRepository.flow.first()
        val key = GeminiCredentials.resolve(s)
        if (!GeminiCredentials.isEffectiveEnabled(s)) {
            _lastError.value = "Activá la IA en Ajustes (y API key si no usás Vertex en Firebase)."
            // Usamos pack offline para no dejar el chat colgado.
            val pack = PersonaCopyPacks.pool(personaKey, _mood.value, _context.value ?: emptyContext())
            val text = pack.randomOrNull()
                ?: "Tirá la moneda nomás. Sin nube no puedo improvisar mucho."
            app.chatRepository.appendPersona(personaKey, text)
            _lastReplyFromAi.value = false
            return
        }

        _typing.value = true
        try {
            val ctx = _context.value ?: buildCtxOrNull().also { _context.value = it }
            _mood.value = moodFor(ctx)

            val history = app.chatRepository.history(personaKey)
                .takeLast(MAX_HISTORY_TURNS)

            val reply = app.personaChatEngine.reply(
                personaKey = personaKey,
                ctx = ctx,
                history = history.dropLast(1), // el último es el user que acabamos de meter
                userMessage = userText,
                apiKey = key,
            )

            val (replyText, errMsg, fromAi) = when (reply) {
                is PersonaChatEngine.Reply.FromAi -> Triple(reply.text, null, true)
                is PersonaChatEngine.Reply.Fallback -> Triple(reply.text, reply.reason, false)
            }
            app.chatRepository.appendPersona(personaKey, replyText)
            _lastError.value = errMsg
            _lastReplyFromAi.value = fromAi
        } finally {
            _typing.value = false
        }
    }

    private suspend fun buildCtxOrNull(): CopyContext? = try {
        app.repository.snapshotCopyContext()
    } catch (e: Exception) {
        null
    }

    private fun emptyContext() = CopyContext(
        monthLabel = "este mes",
        totalLucas = 0,
        topCategoryName = null,
        topCategorySlug = null,
        topCategoryLucas = 0,
        deltaLucasVsPrevMonth = null,
        pctOfMonthlyGoal = null,
    )

    private fun moodFor(ctx: CopyContext?): CopyMood = when {
        ctx == null || ctx.totalLucas == 0 -> CopyMood.NEUTRAL
        (ctx.pctOfMonthlyGoal ?: 0.0) >= 1.05 -> CopyMood.ALARM
        ctx.totalLucas >= 150 -> CopyMood.BURN
        ctx.daySpentLucas == 0 && ctx.expenseCount > 0 -> CopyMood.CHEER
        else -> CopyMood.NEUTRAL
    }

    /**
     * Construye sugerencias rápidas mirando la categoría más cara, el día
     * del mes, el % de meta y el mood. La idea: que el usuario pueda
     * mantener la conversa sin tipear.
     */
    private fun buildQuickPrompts(ctx: CopyContext?, mood: CopyMood): List<QuickPrompt> {
        val out = mutableListOf<QuickPrompt>()

        // Universal: "cómo voy"
        out += QuickPrompt(
            emoji = "🎯",
            label = "¿Cómo voy?",
            message = "Decime con onda cómo voy con la guita este mes y dame un consejo concreto.",
        )

        // Categoría top → prompt específico
        val topSlug = ctx?.topCategorySlug
        if (topSlug != null && ctx.topCategoryLucas > 0) {
            val niceCat = ctx.topCategoryName ?: topSlug
            out += QuickPrompt(
                emoji = "💸",
                label = "¿Y mi $niceCat?",
                message = "Mi categoría más cara es \"$niceCat\". Bardeame ahí, pero de paso dame un truco para bajarla.",
            )
        }

        // Mood → prompt afín
        when (mood) {
            CopyMood.ALARM -> out += QuickPrompt(
                emoji = "🚨",
                label = "Estoy hasta las manos",
                message = "Estoy bardo con la guita este mes, ¿qué corno hago para no fundirme?",
            )
            CopyMood.BURN -> out += QuickPrompt(
                emoji = "🔥",
                label = "Bardéame",
                message = "Bardéame en personaje por lo que estoy gastando, ponete pesado pero gracioso.",
            )
            CopyMood.CHEER -> out += QuickPrompt(
                emoji = "🏆",
                label = "Felicitame",
                message = "Hoy no gasté nada. Hacé un comentario en personaje, dame un cierre lindo.",
            )
            CopyMood.NEUTRAL -> out += QuickPrompt(
                emoji = "🫡",
                label = "Tirame un consejo",
                message = "Tirame UN consejo financiero corto, en personaje y rioplatense.",
            )
        }

        // Fin de mes
        val day = ctx?.dayOfMonth ?: 0
        if (day in 22..31) {
            out += QuickPrompt(
                emoji = "📅",
                label = "Llegar al 30",
                message = "Estamos cerca de fin de mes. Tirame una estrategia rapidísima para llegar al 30 sin pedir prestado.",
            )
        }

        // Pasada de meta
        val pct = ctx?.pctOfMonthlyGoal ?: 0.0
        if (pct >= 1.0) {
            out += QuickPrompt(
                emoji = "🎯",
                label = "Pasé la meta",
                message = "Ya pasé la meta del mes. ¿Qué cortes me sugerís en una sola frase?",
            )
        } else if (pct in 0.85..0.999) {
            out += QuickPrompt(
                emoji = "⚠️",
                label = "Casi al límite",
                message = "Estoy al ${(pct * 100).toInt()}% de la meta del mes y faltan días. ¿Qué pinta?",
            )
        }

        // Universal de cierre: psicoanálisis express, divertido.
        out += QuickPrompt(
            emoji = "🧠",
            label = "Analizame",
            message = "Mirá mis gastos del mes y hacé como si fueras psicólogo de billetera: ¿qué patrón ves?",
        )

        return out.distinctBy { it.label }
    }

    private fun defaultPrompts(): List<QuickPrompt> = listOf(
        QuickPrompt("🎯", "¿Cómo voy?", "Decime cómo voy con la guita este mes."),
        QuickPrompt("🔥", "Bardéame", "Bardéame con onda por mis gastos."),
        QuickPrompt("🧠", "Analizame", "Mirá mis gastos y hacé un mini análisis."),
    )

    companion object {
        private const val MAX_HISTORY_TURNS = 16

        fun factory(app: AntiGastosApplication, key: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PersonaChatViewModel(app, key) as T
            }
    }
}
