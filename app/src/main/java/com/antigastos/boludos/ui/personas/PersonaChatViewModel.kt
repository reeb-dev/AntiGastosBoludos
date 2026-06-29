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
import com.antigastos.boludos.domain.chat.LocalPersonaEngine
import com.antigastos.boludos.domain.chat.PersonaQuickSuggestion
import com.antigastos.boludos.domain.chat.PersonaQuickSuggestions
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
 */
typealias QuickPrompt = PersonaQuickSuggestion

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
        PersonaQuickSuggestions.build(ctx, mood)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PersonaQuickSuggestions.defaults())

    init {
        // Cargamos el contexto al abrir el chat así los chips arrancan con info real.
        viewModelScope.launch {
            val ctx = buildCtxOrNull()
            _context.value = ctx
            _mood.value = LocalPersonaEngine.moodFor(ctx)
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
            val mood = LocalPersonaEngine.moodFor(ctx)
            _mood.value = mood
            val greet = LocalPersonaEngine.greet(personaKey, ctx)
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
            val ctx = _context.value ?: buildCtxOrNull().also { _context.value = it }
            _mood.value = LocalPersonaEngine.moodFor(ctx)
            val history = app.chatRepository.history(personaKey)
            val recent = history.filter { it.role == "persona" }.map { it.text }
            val text = LocalPersonaEngine.chatReply(
                personaKey = personaKey,
                ctx = ctx,
                userMessage = userText,
                recentPersonaLines = recent,
            )
            app.chatRepository.appendPersona(personaKey, text)
            _lastReplyFromAi.value = false
            _lastError.value = null
            return
        }

        _typing.value = true
        try {
            val ctx = _context.value ?: buildCtxOrNull().also { _context.value = it }
            _mood.value = LocalPersonaEngine.moodFor(ctx)

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

    private fun moodFor(ctx: CopyContext?): CopyMood = LocalPersonaEngine.moodFor(ctx)

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
