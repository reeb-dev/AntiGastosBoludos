package com.antigastos.boludos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.AgSectionCard
import com.antigastos.boludos.ui.common.EmptyStateWithPersona
import com.antigastos.boludos.ui.common.MonthSelector
import com.antigastos.boludos.ui.home.anim.StaggeredEntry
import com.antigastos.boludos.ui.home.cards.CalendarBanner
import com.antigastos.boludos.ui.home.cards.CompactMonthHeatmap
import com.antigastos.boludos.ui.home.cards.GoalSection
import com.antigastos.boludos.ui.home.cards.HomeTipsBanner
import com.antigastos.boludos.ui.home.cards.MoreSection
import com.antigastos.boludos.ui.home.cards.PersonalityCard
import com.antigastos.boludos.ui.home.cards.PhantomRecurringCard
import com.antigastos.boludos.ui.home.cards.QuickLinks
import com.antigastos.boludos.ui.home.cards.ShameBar
import com.antigastos.boludos.ui.home.cards.StreakRibbon
import com.antigastos.boludos.ui.home.cards.StreakShieldCard
import com.antigastos.boludos.ui.home.cards.SummarySection
import com.antigastos.boludos.ui.home.cards.TodayHero
import com.antigastos.boludos.ui.home.cards.TrophyOfDayBanner
import com.antigastos.boludos.ui.home.cards.WeeklyChallengeCard
import com.antigastos.boludos.ui.home.cards.StreakShieldRewardedCard
import com.antigastos.boludos.ui.home.cards.showStreakRewardedOffer
import com.antigastos.boludos.ui.home.cards.showStreakShield
import com.antigastos.boludos.ads.AdHelper
import com.antigastos.boludos.ads.AdHelper.RewardedSlot
import com.antigastos.boludos.ads.CompactAdStrip
import com.antigastos.boludos.domain.AdFreePolicy
import com.antigastos.boludos.ui.share.ShareCardRenderer
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Orquestador del Home: combina todas las cards (importadas desde
 * `ui.home.cards`), el `ConsejoBubble`, el snackbar local y el dialog
 * de "llegó el recap". Toda la UI propia vive en archivos separados.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    mainViewModel: MainViewModel,
    onOpenRecap: () -> Unit = {},
    onQuickAdd: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenRuleta: () -> Unit = {},
    onOpenQuiz: () -> Unit = {},
    onOpenLoteria: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
) {
    val ui by viewModel.uiState.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()
    val consejo by viewModel.consejo.collectAsState()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    // Ticker reactivo: refresca cada 60 s para detectar cruce de medianoche
    // y cambios de zona horaria (viajeros). Recalcula `today` + `now` juntos.
    val clock by produceState(initialValue = LocalDate.now() to LocalTime.now()) {
        while (true) {
            val zone = ZoneId.systemDefault()
            value = LocalDate.now(zone) to LocalTime.now(zone)
            delay(60_000L)
        }
    }
    val today = clock.first
    val now = clock.second
    val todayStr = today.toString()
    val currentYm = YearMonth.from(today).toString()
    val viewingCurrentMonth = month == YearMonth.from(today)
    val showRecapPrompt = today.dayOfMonth in 1..3 && settings.lastRecapPromptMonth != currentYm
    var recapOpen by remember { mutableStateOf(false) }
    var wrapOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(showRecapPrompt) {
        if (showRecapPrompt) recapOpen = true
    }

    LaunchedEffect(
        todayStr,
        now.hour,
        now.minute,
        settings.lastDailyWrapDay,
        settings.lastDayClosedDay,
        settings.lastCleanDayHapticDay,
        ui.model,
    ) {
        val m = ui.model ?: return@LaunchedEffect
        // Solo después de las 21:30 hora local del usuario.
        val afterEvening = now.hour > 21 || (now.hour == 21 && now.minute >= 30)
        if (!afterEvening) return@LaunchedEffect
        if (m.expenseCount == 0) return@LaunchedEffect
        // Si ya cerró el día (gesto explícito o feedback de día limpio), no insistas.
        if (settings.lastDayClosedDay == todayStr) return@LaunchedEffect
        if (settings.lastCleanDayHapticDay == todayStr) return@LaunchedEffect
        if (settings.lastDailyWrapDay == todayStr) return@LaunchedEffect
        wrapOpen = true
    }

    LaunchedEffect(ui.model?.daySpentPesos, ui.model?.expenseCount, todayStr, settings.lastCleanDayHapticDay) {
        val m = ui.model ?: return@LaunchedEffect
        if (m.daySpentPesos == 0L && m.expenseCount > 0 && settings.lastCleanDayHapticDay != todayStr) {
            AppFeedback.cleanDayClosed(ctx, settings.soundEnabled)
            app.settingsRepository.update { it.copy(lastCleanDayHapticDay = todayStr) }
        }
    }

    val markRecapShown: () -> Unit = {
        recapOpen = false
        scope.launch {
            app.settingsRepository.update { it.copy(lastRecapPromptMonth = currentYm) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSelector(
                yearMonth = month,
                onPrev = { mainViewModel.shiftMonth(-1) },
                onNext = { mainViewModel.shiftMonth(1) },
                modifier = Modifier.fillMaxWidth(),
            )

            ui.model?.let { state ->
                if (state.expenseCount == 0) {
                    val persona = remember(state.personaKey) { PersonaCatalog.byKey(state.personaKey) }
                    StaggeredEntry(delayMs = 0) {
                        EmptyStateWithPersona(
                            personaEmoji = persona.emoji,
                            personaName = persona.displayName,
                            headline = "Acá no hay un mango cargado",
                            body = "Cargá tu primer gasto y arranco a hacerte la vida " +
                                "imposible (con onda). Si no, no puedo opinar nada del mes.",
                            primaryActionLabel = "➕ Cargá tu primer gasto",
                            onPrimaryAction = onQuickAdd,
                            secondaryActionLabel = "🎲 Cambiar de personaje",
                            onSecondaryAction = {
                                AppFeedback.personaShuffle(ctx)
                                viewModel.shufflePersona()
                            },
                        )
                    }
                } else {
                    StaggeredEntry(delayMs = 0) {
                        TodayHero(
                            daySpentPesos = state.daySpentPesos,
                            budget = ui.dailyBudget,
                            streakDays = state.streakDaysNoSpend,
                            scoreLevel = ui.scoreLevel,
                            scorePoints = ui.scorePoints,
                            lifeHoursLine = ui.dayLifeHoursLabel,
                            onAdd = onQuickAdd,
                        )
                    }
                    // Noche y todavía no cerró el día: ofrecemos un gesto
                    // explícito para anticiparse al modal automático.
                    val nightHour = now.hour >= 21 || now.hour < 5
                    val canCloseDay = nightHour &&
                        settings.lastDayClosedDay != todayStr &&
                        settings.lastCleanDayHapticDay != todayStr
                    if (canCloseDay) {
                        StaggeredEntry(delayMs = 20) {
                            CloseDayChip(
                                onCloseDay = {
                                    AppFeedback.cleanDayClosed(ctx, settings.soundEnabled)
                                    scope.launch {
                                        app.settingsRepository.update {
                                            it.copy(
                                                lastDailyWrapDay = todayStr,
                                                lastDayClosedDay = todayStr,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    // Escudo / video rewarded: justo bajo el resumen del día (máximo contexto de racha).
                    if (showStreakShield(state, settings)) {
                        StaggeredEntry(delayMs = 25) {
                            StreakShieldCard(
                                charges = settings.streakFreezeCharges,
                                onUse = {
                                    scope.launch {
                                        if (app.settingsRepository.consumeStreakFreezeForYesterday()) {
                                            AppFeedback.streakShieldUsed(ctx, settings.soundEnabled)
                                            snackbar.showSnackbar(
                                                "Escudo usado: ayer no cuenta para la racha.",
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (showStreakRewardedOffer(state, settings)) {
                        StaggeredEntry(delayMs = 30) {
                            val activity = ctx as? android.app.Activity
                            StreakShieldRewardedCard(
                                onWatchVideo = {
                                    val act = activity
                                    if (act == null) {
                                        scope.launch {
                                            snackbar.showSnackbar(
                                                "No se pudo abrir el video. Reintentá.",
                                            )
                                        }
                                        return@StreakShieldRewardedCard
                                    }
                                    val shown = AdHelper.showRewarded(
                                        act,
                                        RewardedSlot.STREAK_SHIELD,
                                    ) {
                                        scope.launch {
                                            app.settingsRepository.update {
                                                it.copy(
                                                    streakFreezeCharges = it.streakFreezeCharges + 1,
                                                )
                                            }
                                            snackbar.showSnackbar(
                                                "🛡️ ¡Ganaste un escudo! Usalo para salvar tu racha.",
                                            )
                                        }
                                    }
                                    if (!shown) {
                                        scope.launch {
                                            snackbar.showSnackbar(AdHelper.REWARDED_NOT_READY_MESSAGE)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                if (!settings.homeTipsShown) {
                    StaggeredEntry(delayMs = 30) {
                        HomeTipsBanner(
                            onClose = {
                                scope.launch { app.settingsRepository.markHomeTipsShown() }
                            },
                        )
                    }
                }
                StaggeredEntry(delayMs = 50) {
                    CalendarBanner(
                        budget = ui.dailyBudget,
                        holiday = ui.holidayToday,
                        nextHoliday = ui.nextHoliday,
                    )
                }
                ui.phantomSuggestion?.let { ph ->
                    StaggeredEntry(delayMs = 55) {
                        PhantomRecurringCard(
                            suggestion = ph,
                            onOpenSubscriptions = {
                                mainViewModel.postSubscriptionPrefill(
                                    MainViewModel.SubscriptionFormPrefill(
                                        name = ph.suggestedName,
                                        amountPesos = ph.typicalAmountPesos,
                                        categoryId = ph.categoryId,
                                        dayOfMonth = ph.suggestedDayOfMonth,
                                    ),
                                )
                                onOpenSubscriptions()
                            },
                            onDismiss = { viewModel.dismissPhantomBanner(ph.suggestionKey) },
                        )
                    }
                }
                if (viewingCurrentMonth && state.dailySpendByDay.isNotEmpty()) {
                    StaggeredEntry(delayMs = 70) {
                        CompactMonthHeatmap(
                            yearMonth = month,
                            dailySpendByDay = state.dailySpendByDay,
                            todayDayOfMonth = today.dayOfMonth,
                            showPaceProjection = ui.heatmapPaceProjectionEnabled,
                        )
                    }
                }
                ui.weeklyChallenge?.let { ch ->
                    StaggeredEntry(delayMs = 85) {
                        WeeklyChallengeCard(ch)
                    }
                }
                if (today.dayOfMonth in 25..30) {
                    StaggeredEntry(delayMs = 60) {
                        AgSectionCard(
                            title = "📊 Se viene el recap",
                            body = "Estamos en la recta final del mes. Entrá al resumen y fijate cómo venís antes del cierre.",
                            onClick = onOpenRecap,
                        )
                    }
                }
                StaggeredEntry(delayMs = 120) {
                    PersonalityCard(
                        state = state,
                        aiBusy = ui.aiBusy,
                        aiUsed = ui.aiUsed,
                        onShare = {
                            AppFeedback.shareAction(ctx)
                            ShareCardRenderer.shareCopy(ctx, state.personalityMessage, state.personalityMood)
                        },
                        onShareDaily = {
                            AppFeedback.shareAction(ctx)
                            val p = PersonaCatalog.byKey(state.personaKey)
                            ShareCardRenderer.shareDailyCard(
                                context = ctx,
                                daySpentPesos = state.daySpentPesos,
                                personaEmoji = p.emoji,
                                personaName = p.displayName,
                                quote = state.personalityMessage,
                                mood = state.personalityMood,
                            )
                        },
                        onSpeak = { app.tts.speakForPersona(state.personaKey, state.personalityMessage) },
                        onRegenerate = { viewModel.regenerateAiPhrase() },
                        onShufflePersona = {
                            AppFeedback.personaShuffle(ctx)
                            viewModel.shufflePersona()
                        },
                    )
                }
                StaggeredEntry(delayMs = 160) { SummarySection(state, ui.dailyBudget) }
                StaggeredEntry(delayMs = 190) { ShameBar(state) }
                StaggeredEntry(delayMs = 210) { GoalSection(state) }
                StaggeredEntry(delayMs = 230) { StreakRibbon(streakDays = state.streakDaysNoSpend) }
                if (viewingCurrentMonth && AdFreePolicy.shouldShowAds(settings)) {
                    StaggeredEntry(delayMs = 240) {
                        CompactAdStrip()
                    }
                }
                ui.trophyOfDay?.let { trophy ->
                    StaggeredEntry(delayMs = 250) { TrophyOfDayBanner(trophy) }
                }
                StaggeredEntry(delayMs = 280) {
                    MoreSection(
                        expanded = moreOpen,
                        onToggle = { moreOpen = !moreOpen },
                    ) {
                        QuickLinks(
                            onOpenRecap = onOpenRecap,
                            onQuickAdd = onQuickAdd,
                            onOpenAchievements = onOpenAchievements,
                            onOpenRuleta = onOpenRuleta,
                            onOpenQuiz = onOpenQuiz,
                            onOpenLoteria = onOpenLoteria,
                            onShareMonthStory = {
                                ui.model?.let { m ->
                                    AppFeedback.shareAction(ctx)
                                    val p = PersonaCatalog.byKey(m.personaKey)
                                    val highlights = buildList {
                                        m.topCategories.firstOrNull()?.let { add("Top: ${it.name}") }
                                        if (m.globalGoalPesos != null && m.globalProgress != null) {
                                            val pct = (m.globalProgress * 100).toInt().coerceIn(0, 150)
                                            add("Meta del mes ~$pct%")
                                        }
                                        add("Gasto de hoy: ${MoneyFormat.formatPesos(m.daySpentPesos)}")
                                    }
                                    ShareCardRenderer.shareMonthlyRecapStory(
                                        context = ctx,
                                        monthLabel = "${SpanishMonths.name(month.monthValue)} ${month.year}",
                                        totalPesos = m.totalPesos,
                                        personaEmoji = p.emoji,
                                        personaName = p.displayName,
                                        highlights = highlights,
                                        closingQuote = m.personalityMessage,
                                        mood = m.personalityMood,
                                    )
                                }
                            },
                        )
                    }
                }
            } ?: Text("Cargando tu día…")
        }

        ConsejoBubble(
            consejo = consejo,
            onDismiss = { viewModel.dismissConsejo() },
            onCyclePersona = {
                AppFeedback.personaShuffle(ctx)
                viewModel.cycleConsejoPersona()
            },
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (wrapOpen) {
        val m = ui.model
        // Dismiss "tibio": marca el wrap como visto pero NO cierra el día.
        // Sirve para "Ver el mes" o tap fuera del dialog.
        val dismissWrap: () -> Unit = {
            wrapOpen = false
            scope.launch {
                app.settingsRepository.update { it.copy(lastDailyWrapDay = todayStr) }
            }
        }
        // Gesto **explícito**: cierra el día. No vuelve a aparecer hoy ni aunque
        // el usuario reentre a la app. También se cuenta como wrap visto.
        val closeDayExplicit: () -> Unit = {
            wrapOpen = false
            AppFeedback.cleanDayClosed(ctx, settings.soundEnabled)
            scope.launch {
                app.settingsRepository.update {
                    it.copy(
                        lastDailyWrapDay = todayStr,
                        lastDayClosedDay = todayStr,
                    )
                }
            }
        }
        AlertDialog(
            onDismissRequest = dismissWrap,
            title = { Text("🌙 Cierre del día") },
            text = {
                if (m != null) {
                    val streakLine = when (m.streakDaysNoSpend) {
                        0 -> "Hoy todavía no sumás racha."
                        1 -> "Llevás 1 día sin gastar hoy."
                        else -> "Llevás ${m.streakDaysNoSpend} días sin gastar hoy."
                    }
                    Text(
                        buildString {
                            append("Hoy gastaste ${MoneyFormat.formatPesos(m.daySpentPesos)}. ")
                            append("$streakLine ")
                            append("Mes acumulado: ${MoneyFormat.formatPesos(m.totalPesos)}. ")
                            append("Si te falta cargar algo, hacelo ahora. Si no, cerrá el día.")
                        },
                    )
                } else {
                    Text("Así cerramos el día. Mañana seguimos.")
                }
            },
            confirmButton = {
                Button(onClick = closeDayExplicit) { Text("Cerrar el día") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    dismissWrap()
                    onOpenRecap()
                }) { Text("Ver el mes") }
            },
        )
    }

    if (recapOpen) {
        AlertDialog(
            onDismissRequest = markRecapShown,
            title = { Text("📊 Llegó el recap") },
            text = {
                Text(
                    "Cerró un mes más. ¿Querés ver cómo te fue con la guita? " +
                        "Te armé un resumen estilo wrapped que podés compartir.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    markRecapShown()
                    onOpenRecap()
                }) { Text("Ver mi recap") }
            },
            dismissButton = {
                OutlinedButton(onClick = markRecapShown) {
                    Text("Después")
                }
            },
        )
    }
}

/**
 * Chip "Cerré el día" que aparece de noche en el Home. Permite al usuario
 * cerrar el día con un gesto explícito antes de que aparezca el wrap modal.
 */
@Composable
private fun CloseDayChip(onCloseDay: () -> Unit) {
    AssistChip(
        onClick = onCloseDay,
        label = { Text("🌙 Cerré el día") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}
