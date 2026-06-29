package com.antigastos.boludos.ui.bolsillo

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.BudgetEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.Forecast
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.MoneySlang
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ads.CompactAdStrip
import com.antigastos.boludos.domain.AdFreePolicy
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.EmptyStateWithPersona
import com.antigastos.boludos.ui.common.MonthSelector
import com.antigastos.boludos.ui.theme.AgColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

@Composable
fun BolsilloScreen(
    viewModel: BolsilloViewModel,
    mainViewModel: MainViewModel,
    onAdd: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication

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

        val home = state.home
        if (home == null) {
            Text("Cargando…")
        } else {
            val noBudget = state.budget == null ||
                ((state.budget?.salaryPesos ?: 0L) == 0L &&
                    (state.budget?.otherIncomePesos ?: 0L) == 0L &&
                    (state.budget?.fixedExpensesPesos ?: 0L) == 0L)
            if (home.expenseCount == 0 && noBudget) {
                val persona = remember(state.settings.personaKey) {
                    PersonaCatalog.byKey(state.settings.personaKey)
                }
                EmptyStateWithPersona(
                    personaEmoji = persona.emoji,
                    personaName = persona.displayName,
                    headline = "Bolsillo vacío, mente liviana",
                    body = "Cargá tu sueldo y gastos fijos abajo, o tirá tu primer gasto del mes. " +
                        "Sin datos no te puedo decir si vas bien o si la estás pifiando.",
                    primaryActionLabel = "➕ Cargá tu primer gasto",
                    onPrimaryAction = onAdd,
                )
            }
            MonthlyBudgetCard(
                yearMonth = month,
                home = home,
                budget = state.budget,
                settings = state.settings,
                onSave = viewModel::saveBudget,
            )
            state.forecast?.let { ForecastCard(it) }
            if (state.settings.partnerEnabled && state.partnerSharedCount > 0) {
                PartnerOweCard(
                    partnerName = state.settings.partnerName.ifBlank { "tu pareja" },
                    pesosOwed = state.partnerOwesPesos,
                    sharedCount = state.partnerSharedCount,
                )
            }
            DailyBudgetCard(home)
            CanGoOutCard(home)
            StreakCard(home)
            AutoRegaloCard(home)
            TipsCard(home)
            if (AdFreePolicy.shouldShowAds(state.settings)) {
                CompactAdStrip()
            }
        }
    }
}

@Composable
private fun PartnerOweCard(partnerName: String, pesosOwed: Long, sharedCount: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AgColors.CheerGreen.copy(alpha = 0.12f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "👥 $partnerName te debe",
                style = MaterialTheme.typography.labelLarge,
                color = AgColors.CheerGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                MoneyFormat.formatPesos(pesosOwed),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Sobre $sharedCount gasto(s) compartido(s) este mes.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ForecastCard(forecast: Forecast) {
    val color = when {
        forecast.ratioVsGoal == null -> AgColors.NeutralBlue
        forecast.ratioVsGoal >= 1.2 -> AgColors.AlarmRed
        forecast.ratioVsGoal >= 1.0 -> AgColors.BurnAmber
        forecast.ratioVsGoal >= 0.85 -> AgColors.BurnAmber
        else -> AgColors.CheerGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📈 Proyección del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                MoneyFormat.formatPesos(forecast.projectedTotalPesos),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Llevás ${forecast.daysElapsed} de ${forecast.daysInMonth} días.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(forecast.verdict, color = color)
        }
    }
}

@Composable
private fun MonthlyBudgetCard(
    yearMonth: YearMonth,
    home: HomeUiModel,
    budget: BudgetEntity?,
    settings: SettingsEntity,
    onSave: (salary: Long, other: Long, fixed: Long) -> Unit,
) {
    var salary by remember(yearMonth) { mutableStateOf(budget?.salaryPesos?.takeIf { it > 0L }?.toString() ?: "") }
    var other by remember(yearMonth) { mutableStateOf(budget?.otherIncomePesos?.takeIf { it > 0L }?.toString() ?: "") }
    var fixed by remember(yearMonth) { mutableStateOf(budget?.fixedExpensesPesos?.takeIf { it > 0L }?.toString() ?: "") }
    var dirty by remember(yearMonth) { mutableStateOf(false) }

    LaunchedEffect(budget?.period, budget?.salaryPesos, budget?.otherIncomePesos, budget?.fixedExpensesPesos) {
        if (!dirty) {
            salary = budget?.salaryPesos?.takeIf { it > 0L }?.toString() ?: ""
            other = budget?.otherIncomePesos?.takeIf { it > 0L }?.toString() ?: ""
            fixed = budget?.fixedExpensesPesos?.takeIf { it > 0L }?.toString() ?: ""
        }
    }

    val salaryPesos = salary.toLongOrNull() ?: 0L
    val otherPesos = other.toLongOrNull() ?: 0L
    val fixedPesos = fixed.toLongOrNull() ?: 0L
    val ingresoTotal = salaryPesos + otherPesos
    val variable = home.totalPesos
    val quedaPesos = ingresoTotal - fixedPesos - variable
    val ratioGastado = if (ingresoTotal > 0L) {
        (fixedPesos + variable).toDouble() / ingresoTotal.toDouble()
    } else 0.0

    val color = when {
        ingresoTotal == 0L -> AgColors.NeutralBlue
        quedaPesos < 0L -> AgColors.AlarmRed
        ratioGastado >= 0.85 -> AgColors.BurnAmber
        else -> AgColors.CheerGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("💼 Mi mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Cargá tu sueldo, ingresos extra y gastos fijos. Te calculo cuánto te queda.")

            OutlinedTextField(
                value = salary,
                onValueChange = { dirty = true; salary = it.filter(Char::isDigit).take(12) },
                label = { Text("Sueldo (pesos)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = other,
                onValueChange = { dirty = true; other = it.filter(Char::isDigit).take(12) },
                label = { Text("Otros ingresos (changas, freelo, etc)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = fixed,
                onValueChange = { dirty = true; fixed = it.filter(Char::isDigit).take(12) },
                label = { Text("Gastos fijos (alquiler, servicios, etc)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        onSave(salaryPesos, otherPesos, fixedPesos)
                        dirty = false
                    },
                    enabled = dirty,
                ) { Text("Guardar") }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (dirty) "Cambios sin guardar" else "Guardado",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ingresoTotal == 0L) {
                        Text(
                            "Sin sueldo cargado, no puedo calcular cuánto te queda.",
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        val over = quedaPesos < 0L
                        val broke = ratioGastado >= 0.95
                        val daySalt = remember(over, broke) {
                            java.time.LocalDate.now().toEpochDay() xor
                                ((if (over) 1L else 0L) shl 8) xor
                                ((if (broke) 1L else 0L) shl 16)
                        }
                        val rngForLabel = remember(daySalt) { kotlin.random.Random(daySalt) }
                        val balanceTitle = remember(daySalt) {
                            com.antigastos.boludos.domain.MoneySlang.balanceLabel(rngForLabel)
                        }
                        val verboTeQueda = remember(daySalt) {
                            com.antigastos.boludos.domain.MoneySlang.remainingLabel(over, rngForLabel)
                        }
                        val walletState = remember(daySalt) {
                            com.antigastos.boludos.domain.MoneySlang.walletStateLabel(over, broke, rngForLabel)
                        }
                        Text(
                            balanceTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(verboTeQueda, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            MoneyFormat.formatPesos(kotlin.math.abs(quedaPesos)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                        val lucasQuedan = MoneyFormat.pesosToLucas(kotlin.math.abs(quedaPesos))
                        val nounForBalance = remember(daySalt) {
                            com.antigastos.boludos.domain.MoneySlang.noun(rngForLabel)
                        }
                        Text(
                            "(${if (over) "-" else ""}$lucasQuedan $nounForBalance · $walletState)",
                            color = color,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (settings.usdRate > 0) {
                            val usd = (kotlin.math.abs(quedaPesos).toDouble() / settings.usdRate)
                            Text(
                                "≈ ${if (quedaPesos < 0L) "-" else ""}USD ${"%.2f".format(usd)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratioGastado.coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = color,
                        )
                        Text(
                            buildString {
                                append("Sueldo + ingresos: ${MoneyFormat.formatPesos(ingresoTotal)}\n")
                                append("Gastos fijos: ${MoneyFormat.formatPesos(fixedPesos)}\n")
                                append("Ya gastaste este mes: ${MoneyFormat.formatPesos(variable)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(verdictForBudget(quedaPesos, ratioGastado), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun verdictForBudget(quedaPesos: Long, ratioGastado: Double): String {
    val lucasQuedan = MoneyFormat.pesosToLucas(kotlin.math.abs(quedaPesos))
    return when {
        quedaPesos < 0L -> "🚨 Te fuiste $lucasQuedan lucas en rojo. Modo emergencia: hacé vaquita y bancá la semana."
        ratioGastado >= 0.95 -> "💀 Estás raspando. Quedan $lucasQuedan lucas y todavía falta mes."
        ratioGastado >= 0.8 -> "⚠️ Apretá el cinturón. Restan $lucasQuedan lucas pero estás caliente."
        ratioGastado >= 0.5 -> "👌 Vas pareja. Te quedan $lucasQuedan lucas. Cuidá el bolsillo."
        ratioGastado > 0.0 -> "✅ Bien parado. Tenés $lucasQuedan lucas de margen sano."
        else -> "🧉 Mes virgen. Cargá los gastos y te aviso cómo va."
    }
}

@Composable
private fun DailyBudgetCard(state: HomeUiModel) {
    val today = LocalDate.now()
    val isCurrent = today.year == state.yearMonth.year && today.monthValue == state.yearMonth.monthValue
    val daysInMonth = state.yearMonth.lengthOfMonth()
    val daysLeft = if (isCurrent) max(1, daysInMonth - today.dayOfMonth + 1) else daysInMonth

    val goal = state.globalGoalPesos
    val remaining = if (goal != null) (goal - state.totalPesos).coerceAtLeast(0L) else 0L
    val daily = if (goal != null && goal > 0L) remaining / daysLeft else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💸 Tu margen del día", style = MaterialTheme.typography.titleMedium)
            if (goal == null) {
                Text("Cargá una meta global del mes y te digo cuánto podés bancar por día.")
            } else {
                Text(
                    MoneyFormat.formatPesos(daily),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("(${MoneyFormat.pesosToLucas(daily)} lucas / día — quedan $daysLeft días)")
                Spacer(Modifier.height(4.dp))
                Text("Plata restante del mes: ${MoneyFormat.formatPesos(remaining)}")
                LinearProgressIndicator(
                    progress = { (state.globalProgress ?: 0f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CanGoOutCard(state: HomeUiModel) {
    val today = LocalDate.now()
    val isCurrent = today.year == state.yearMonth.year && today.monthValue == state.yearMonth.monthValue
    val daysInMonth = state.yearMonth.lengthOfMonth()
    val daysLeft = if (isCurrent) max(1, daysInMonth - today.dayOfMonth + 1) else daysInMonth
    val daySpentLucas = MoneyFormat.pesosToLucas(state.daySpentPesos)
    val goal = state.globalGoalPesos
    val remaining = if (goal != null) (goal - state.totalPesos).coerceAtLeast(0L) else 0L
    val dailyLucas = if (goal != null && goal > 0L) MoneyFormat.pesosToLucas(remaining / daysLeft) else -1
    val pctGoal = state.globalProgress?.toDouble() ?: 0.0

    val (verdict, body, color) = when {
        pctGoal >= 1.0 -> Triple(
            "❌ Hoy no podés salir",
            "Te pasaste la meta del mes. Modo emergencia: hacé vaquita 🐄.",
            Color(0xFFD7263D),
        )
        dailyLucas in 1..3 && daySpentLucas == 0 -> Triple(
            "🤏 Salida modesta",
            "Te quedan apenas ~$dailyLucas lucas por día. Una birra como mucho 🍺.",
            Color(0xFFB97A0A),
        )
        dailyLucas >= 5 && daySpentLucas == 0 -> Triple(
            "✅ Hoy podés salir",
            "Tenés ~$dailyLucas lucas de margen sin romper la meta. Avivate igual.",
            Color(0xFF2E7D32),
        )
        daySpentLucas >= 30 -> Triple(
            "🛑 Cuidadito",
            "Hoy ya soltaste $daySpentLucas lucas. Después no llores a fin de mes.",
            Color(0xFFB97A0A),
        )
        dailyLucas < 0 -> Triple(
            "Sin meta cargada",
            "Cargá una meta global y te digo si podés salir tranqui.",
            Color(0xFF1F4E8A),
        )
        else -> Triple(
            "Andá tirando",
            "${MoneySlang.statusForGoalPct(pctGoal).replaceFirstChar { it.uppercase() }}.",
            Color(0xFF1F4E8A),
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(verdict, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StreakCard(state: HomeUiModel) {
    val streak = state.streakDaysNoSpend
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔥 Racha sin gastar", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE7F1FB), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        streak.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F4E8A),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    when (streak) {
                        0 -> "Hoy ya gastaste. Mañana arrancás de cero."
                        1 -> "1 día sin tocar la billetera. Empezá a hacer canuto."
                        in 2..3 -> "$streak días limpios. Vas para premio."
                        in 4..6 -> "$streak días seguidos sin patinar la guita 🐀"
                        else -> "$streak días en modo monje 🧘. Inimputable."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun AutoRegaloCard(state: HomeUiModel) {
    val streak = state.streakDaysNoSpend
    val pctGoal = state.globalProgress?.toDouble() ?: 1.1

    val regalo = when {
        pctGoal >= 1.0 -> "Cero auto-regalos. Estás reventado financieramente, capo."
        streak >= 7 -> "🏆 Te ganaste un capricho grande: una salida o un kit de cosas que querías hace rato."
        streak >= 4 -> "🎁 Te merecés una cena rica en casa o una cerveza buena."
        streak >= 2 -> "🍫 Premio chico: un alfajor premium o un cafecito de los caros."
        streak == 1 -> "👏 Bien ahí. Si seguís un día más, te ganás algo lindo."
        else -> "Hoy gastaste algo. Mañana podés empezar a ganar premios."
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎁 Auto-regalo", style = MaterialTheme.typography.titleMedium)
            Text(regalo, style = MaterialTheme.typography.bodyLarge)
            Text(
                "La regla: cuanto más bancás sin gastar, más grande el premio que te merecés.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TipsCard(state: HomeUiModel) {
    val tips = buildList {
        add("Pagá en efectivo cuando puedas — duele más, pero gastás menos.")
        add("Hacé vaquita 🐄 con amigos para salidas grandes.")
        add("Antes de comprar online, esperá 24hs. Si todavía lo querés, dale.")
        if ((state.globalProgress ?: 0f) >= 0.8f) {
            add("Cuoteá lo que salga $20+ lucas para no romper el chanchito.")
        }
        if (state.expenseCount >= 10) {
            add("Cargás muchos gastos chicos — ojo con los gastos hormiga 🐜.")
        }
        if ((state.streakDaysNoSpend) == 0 && (state.globalProgress ?: 0f) >= 0.5f) {
            add("Probá un día sin gastar a la semana. Modo rata = ahorro inmediato.")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💡 Consejos", style = MaterialTheme.typography.titleMedium)
            tips.forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
