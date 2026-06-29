package com.antigastos.boludos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.AchievementsRepository
import com.antigastos.boludos.data.ExpenseRepository
import com.antigastos.boludos.data.ScoreRepository
import com.antigastos.boludos.data.SettingsRepository
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.AchievementMeta
import com.antigastos.boludos.domain.AiCopyOrchestrator
import com.antigastos.boludos.domain.Consejo
import com.antigastos.boludos.domain.ConsejoEngine
import com.antigastos.boludos.domain.ConsejoVoicer
import com.antigastos.boludos.domain.HolidayCalendar
import com.antigastos.boludos.domain.LifeHoursEngine
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.RandomTrophyCatalog
import com.antigastos.boludos.domain.RecurringPhantomSuggestion
import com.antigastos.boludos.domain.ScoreEngine
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ui.MainViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Mini-desafío semanal (categoría + tope) mostrado en Home.
 */
data class WeeklyChallengeUi(
    val title: String,
    val spentPesos: Long,
    val limitPesos: Long,
)

/**
 * Estado completo que consume HomeScreen: el modelo de gastos + flags IA.
 */
data class HomeUiState(
    val model: HomeUiModel? = null,
    val aiBusy: Boolean = false,
    /** True si el `personalityMessage` actual viene de la IA. */
    val aiUsed: Boolean = false,
    val aiError: String? = null,
    val dailyBudget: SpendingBudgetEngine.DailyBudget? = null,
    val holidayToday: HolidayCalendar.Holiday? = null,
    val nextHoliday: HolidayCalendar.Holiday? = null,
    val trophyOfDay: AchievementMeta? = null,
    val weeklyChallenge: WeeklyChallengeUi? = null,
    val scorePoints: Long = 0L,
    val scoreLevel: Int = 1,
    /** Línea opcional “costo en horas de tu vida” para el gasto de hoy. */
    val dayLifeHoursLabel: String? = null,
    /** Proyección de ritmo en el heatmap del Home. */
    val heatmapPaceProjectionEnabled: Boolean = true,
    /** Gasto manual recurrente que podría ser suscripción (opción B). */
    val phantomSuggestion: RecurringPhantomSuggestion? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val ai: AiCopyOrchestrator,
    private val achievementsRepository: AchievementsRepository,
    private val scoreRepository: ScoreRepository,
    private val mainViewModel: MainViewModel,
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _ui.asStateFlow()

    /**
     * Stream "crudo" del repo. Lo usamos para detectar cambios de contexto y
     * disparar la generación IA.
     */
    private val baseFlow: StateFlow<HomeUiModel?> = mainViewModel.selectedMonth
        .flatMapLatest { repository.observeHome(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _consejo = MutableStateFlow<Consejo?>(null)
    val consejo: StateFlow<Consejo?> = _consejo.asStateFlow()

    /**
     * Consejo "raw" sin voz, que sirve de base para volver a aplicarle distintas
     * personalidades cuando el usuario tira la ruleta.
     */
    private var consejoBase: Consejo? = null

    private var lastSeenSignature: String? = null
    private var aiJob: Job? = null
    private var consejoAiJob: Job? = null
    private var mergeJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.flow,
                baseFlow,
                scoreRepository.observeScore(),
            ) { settings, model, score -> Triple(settings, model, score) }
                .collect { (settings, model, score) ->
                    mergeJob?.cancel()
                    mergeJob = viewModelScope.launch {
                        if (model == null) {
                            lastSeenSignature = null
                            _ui.value = HomeUiState()
                            return@launch
                        }

                        val today = LocalDate.now()
                        val budget = SpendingBudgetEngine.evaluate(model, today)
                        val holidayToday = HolidayCalendar.forDate(today)
                        val nextHoliday = HolidayCalendar.nextHoliday(today.plusDays(1))
                        val trophy = RandomTrophyCatalog.trophyOfDay(today)
                        val weekly = loadWeeklyChallenge(settings)
                        val pts = score?.points ?: 0L
                        val lvl = ((pts.coerceAtLeast(0L)) / 100L + 1L).toInt().coerceAtLeast(1)
                        val rate = LifeHoursEngine.hourlyRatePesos(
                            settings.monthlyNetIncomePesos,
                            settings.weeklyWorkHours,
                        )
                        val dayLife = if (
                            settings.lifeHoursEnabled &&
                            rate != null &&
                            model.daySpentPesos > 0L
                        ) {
                            "Eso son ${LifeHoursEngine.formatLifeHours(model.daySpentPesos, rate)} " +
                                "de tu laburo (${LifeHoursEngine.formatHourlyRateLabel(rate)})."
                        } else {
                            null
                        }
                        val nowYm = YearMonth.from(today)
                        val phantom = if (
                            model.yearMonth == nowYm &&
                            settings.phantomRecurringHintsEnabled
                        ) {
                            repository.detectRecurringPhantomSuggestion()?.takeIf {
                                it.suggestionKey != settings.dismissedPhantomSuggestionKey
                            }
                        } else {
                            null
                        }
                        _ui.value = _ui.value.copy(
                            model = model,
                            aiUsed = false,
                            dailyBudget = budget,
                            holidayToday = holidayToday,
                            nextHoliday = nextHoliday,
                            trophyOfDay = trophy,
                            weeklyChallenge = weekly,
                            scorePoints = pts,
                            scoreLevel = lvl,
                            dayLifeHoursLabel = dayLife,
                            heatmapPaceProjectionEnabled = settings.heatmapPaceProjectionEnabled,
                            phantomSuggestion = phantom,
                        )

                        launch {
                            val unlocks = achievementsRepository.evaluate(model, repository)
                            if (unlocks.isNotEmpty()) {
                                unlocks.forEach { meta ->
                                    scoreRepository.applyDelta(
                                        ScoreEngine.evaluateAchievementUnlock(meta.key, meta.title),
                                    )
                                }
                                mainViewModel.enqueueAchievementUnlocks(unlocks)
                            }
                        }

                        val sig = "${model.yearMonth}|${model.expenseCount}|${model.totalPesos}|${model.lastExpenseSlug}"
                        if (lastSeenSignature == null) {
                            lastSeenSignature = sig
                        } else if (sig != lastSeenSignature) {
                            lastSeenSignature = sig
                            val picked = ConsejoEngine.pick(model)
                            consejoBase = picked
                            if (picked != null) {
                                val randomPersona = PersonaCatalog.all.random()
                                val voiced = ConsejoVoicer.voice(picked, randomPersona)
                                _consejo.value = voiced
                                rewriteConsejoBody(model, voiced)
                            } else {
                                _consejo.value = null
                            }
                        }

                        triggerAiPhrase(model)
                    }
                }
        }
    }

    private suspend fun loadWeeklyChallenge(settings: SettingsEntity): WeeklyChallengeUi? {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        if (settings.weeklyChallengeWeekStart != monday) return null
        if (settings.weeklyChallengeSlug.isBlank() || settings.weeklyChallengeLimitPesos <= 0L) return null
        val spent = repository.sumCategorySlugCalendarWeek(settings.weeklyChallengeSlug, 0)
        val title = settings.weeklyChallengeTitle.ifBlank { "Desafío semanal" }
        return WeeklyChallengeUi(
            title = title,
            spentPesos = spent,
            limitPesos = settings.weeklyChallengeLimitPesos,
        )
    }

    private fun triggerAiPhrase(model: HomeUiModel) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val s = settingsRepository.getSnapshot()
            val key = com.antigastos.boludos.data.GeminiCredentials.resolve(s)
            if (!com.antigastos.boludos.data.GeminiCredentials.isEffectiveEnabled(s)) {
                _ui.value = _ui.value.copy(aiBusy = false, aiUsed = false, aiError = null)
                return@launch
            }
            _ui.value = _ui.value.copy(aiBusy = true, aiError = null)
            val ctx = runCatching { repository.snapshotCopyContext(model.yearMonth) }
                .getOrNull()
            if (ctx == null) {
                _ui.value = _ui.value.copy(aiBusy = false)
                return@launch
            }
            val r = ai.frasePersonaje(
                personaKey = model.personaKey,
                ctx = ctx,
                mood = model.personalityMood,
                aiEnabled = true,
                apiKey = key,
                fallbackText = model.personalityMessage,
                cacheBucket = "home",
            )
            // Si el modelo cambió mientras esperábamos la respuesta, descartamos.
            val current = _ui.value.model ?: return@launch
            if (current.personalityTemplateId != model.personalityTemplateId ||
                current.yearMonth != model.yearMonth) {
                return@launch
            }
            _ui.value = _ui.value.copy(
                model = current.copy(personalityMessage = r.text),
                aiBusy = false,
                aiUsed = r.aiUsed,
                aiError = r.error.takeIf { !r.aiUsed },
            )
        }
    }

    private fun rewriteConsejoBody(model: HomeUiModel, consejo: Consejo) {
        consejoAiJob?.cancel()
        consejoAiJob = viewModelScope.launch {
            val s = settingsRepository.getSnapshot()
            val key = com.antigastos.boludos.data.GeminiCredentials.resolve(s)
            if (!com.antigastos.boludos.data.GeminiCredentials.isEffectiveEnabled(s)) return@launch
            val ctx = runCatching { repository.snapshotCopyContext(model.yearMonth) }.getOrNull()
                ?: return@launch
            val voicePersona = consejo.personaKey ?: model.personaKey
            // Usamos el body BASE para que el modelo no se confunda con el saludo offline.
            val baseBody = consejoBase?.body ?: consejo.body
            val r = ai.consejoBody(
                personaKey = voicePersona,
                ctx = ctx,
                consejoId = "${consejo.id}|$voicePersona",
                kind = consejo.kind,
                title = consejo.title,
                body = baseBody,
                aiEnabled = true,
                apiKey = key,
            )
            val cur = _consejo.value
            // Si el consejo cambió (otra persona, otro id), descarto.
            if (cur?.id != consejo.id || cur.personaKey != consejo.personaKey) return@launch
            if (r.text.isNotBlank() && r.text != consejo.body) {
                _consejo.value = cur.copy(body = r.text.trim())
            }
        }
    }

    /**
     * Tira la "ruleta" de personalidad para el consejo activo. Vuelve a
     * renderizar el body con el saludo de otra personalidad random (≠ actual)
     * y, si la IA está prendida, dispara una re-escritura.
     */
    fun cycleConsejoPersona() {
        val base = consejoBase ?: return
        val current = _consejo.value ?: return
        val candidates = PersonaCatalog.all.filter { it.key != current.personaKey }
        val nextPersona = candidates.randomOrNull() ?: PersonaCatalog.all.random()
        val voiced = ConsejoVoicer.voice(base, nextPersona)
        _consejo.value = voiced
        val model = _ui.value.model ?: return
        rewriteConsejoBody(model, voiced)
    }

    fun dismissConsejo() {
        _consejo.value = null
        consejoBase = null
    }

    fun dismissPhantomBanner(key: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(dismissedPhantomSuggestionKey = key) }
        }
    }

    /** Forzá una nueva frase IA (botón "regenerar" si lo querés exponer). */
    fun regenerateAiPhrase() {
        ai.invalidate()
        _ui.value.model?.let { triggerAiPhrase(it) }
    }

    /**
     * Sortea una persona al azar y la guarda como persona principal del usuario.
     * El resto de la app reaccionará vía settingsRepository.flow.
     */
    fun shufflePersona() {
        viewModelScope.launch {
            val current = settingsRepository.getSnapshot().personaKey
            val pool = PersonaCatalog.all.filter { it.key != current }
            val pick = pool.randomOrNull() ?: PersonaCatalog.all.random()
            settingsRepository.update { it.copy(personaKey = pick.key) }
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        app.repository,
                        app.settingsRepository,
                        app.aiCopyOrchestrator,
                        app.achievementsRepository,
                        app.scoreRepository,
                        main,
                    ) as T
                }
            }
    }
}
