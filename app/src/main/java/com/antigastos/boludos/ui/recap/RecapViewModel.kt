package com.antigastos.boludos.ui.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.domain.model.StatsSnapshot
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.antigastos.boludos.ui.MainViewModel

/**
 * Tile (cuadrado) del recap. Cada uno se renderiza con su propio gradiente.
 */
data class RecapTile(
    val emoji: String,
    val label: String,
    val value: String,
    val sub: String? = null,
)

data class RecapUi(
    val yearMonth: YearMonth,
    val title: String,
    val heroLabel: String,
    val heroValue: String,
    val heroSub: String,
    val tiles: List<RecapTile>,
    val punchline: String,
    val mood: CopyMood,
    /** Líneas planas para compartir / TTS. */
    val shareLines: List<String>,
    /** Datos crudos para que la pantalla pueda armar el share story 9:16. */
    val totalPesos: Long = 0L,
    val personaKey: String = "termo",
    val personaEmoji: String = "🧉",
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecapViewModel(
    private val app: AntiGastosApplication,
    mainViewModel: MainViewModel,
) : ViewModel() {

    val ui: StateFlow<RecapUi?> = mainViewModel.selectedMonth
        .flatMapLatest { ym ->
            combine(
                app.repository.observeHome(ym),
                app.repository.observeStats(ym),
            ) { home, stats -> buildRecap(home, stats) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RecapViewModel(app, main) as T
            }
    }
}

private fun buildRecap(home: HomeUiModel, stats: StatsSnapshot): RecapUi {
    val monthName = SpanishMonths.name(home.yearMonth.monthValue)

    val tiles = mutableListOf<RecapTile>()
    val shareLines = mutableListOf<String>()

    tiles += RecapTile(
        emoji = "🧾",
        label = "Movimientos",
        value = home.expenseCount.toString(),
        sub = "gastos cargados",
    )
    shareLines += "Gastos registrados: ${home.expenseCount}"

    val prev = stats.previousMonthTotalPesos
    if (prev > 0L) {
        val delta = home.totalPesos - prev
        val pct = ((delta.toDouble() / prev.toDouble()) * 100.0).toInt()
        val sign = if (delta >= 0) "+" else "−"
        val arrow = if (delta >= 0) "📈" else "📉"
        tiles += RecapTile(
            emoji = arrow,
            label = "Vs mes anterior",
            value = "$sign${MoneyFormat.formatPesos(abs(delta))}",
            sub = "≈ $pct%",
        )
        shareLines += "Vs mes anterior: $sign${MoneyFormat.formatPesos(abs(delta))} (~$pct%)"
    }

    val topCat = home.topCategories.firstOrNull()
    if (topCat != null) {
        tiles += RecapTile(
            emoji = "🏆",
            label = "Categoría top",
            value = topCat.name,
            sub = MoneyFormat.formatPesos(topCat.amountPesos),
        )
        shareLines += "Categoría top: ${topCat.name} (${MoneyFormat.formatPesos(topCat.amountPesos)})"
    }

    val worstDay = stats.topDays.maxByOrNull { it.amountPesos }
    if (worstDay != null && worstDay.amountPesos > 0L) {
        tiles += RecapTile(
            emoji = "💸",
            label = "Día más caro",
            value = "${worstDay.date.dayOfMonth}/${worstDay.date.monthValue}",
            sub = MoneyFormat.formatPesos(worstDay.amountPesos),
        )
        shareLines += "Día más caro: ${worstDay.date.dayOfMonth}/${worstDay.date.monthValue} " +
            "(${MoneyFormat.formatPesos(worstDay.amountPesos)})"
    }

    val goal = home.globalGoalPesos
    if (goal != null && goal > 0L) {
        val pct = ((home.totalPesos.toDouble() / goal) * 100).toInt().coerceIn(0, 999)
        val emoji = when {
            pct < 75 -> "✅"
            pct < 100 -> "⚠️"
            else -> "🚨"
        }
        tiles += RecapTile(
            emoji = emoji,
            label = "Meta del mes",
            value = "$pct%",
            sub = "del tope (${MoneyFormat.formatPesos(goal)})",
        )
        shareLines += "Meta del mes: $pct% del tope"
    }

    if (home.streakDaysNoSpend > 0) {
        tiles += RecapTile(
            emoji = "🔥",
            label = "Racha",
            value = "${home.streakDaysNoSpend}d",
            sub = "sin gastar",
        )
        shareLines += "Racha sin gastar: ${home.streakDaysNoSpend} día(s)"
    }

    val punchline = punchlineFor(home, stats)
    val title = "Tu $monthName, en corto"

    return RecapUi(
        yearMonth = home.yearMonth,
        title = title,
        heroLabel = "Total en $monthName",
        heroValue = MoneyFormat.formatPesos(home.totalPesos),
        heroSub = "≈ ${MoneyFormat.pesosToLucas(home.totalPesos)} lucas",
        tiles = tiles,
        punchline = punchline,
        mood = home.personalityMood,
        shareLines = shareLines,
        totalPesos = home.totalPesos,
        personaKey = home.personaKey,
        personaEmoji = home.personaEmoji,
    )
}

private fun punchlineFor(home: HomeUiModel, stats: StatsSnapshot): String {
    val pct = home.globalProgress
    return when {
        home.expenseCount == 0 -> "Mes en blanco. ¿Lo cargás o me lo invento?"
        pct != null && pct >= 1.0f -> "Te zarpaste, capo. El bolsillo te llora."
        pct != null && pct >= 0.85f -> "Vas justito, no te confíes en los últimos días."
        home.streakDaysNoSpend >= 3 -> "Bancando como rey: racha sólida."
        stats.previousMonthTotalPesos > 0L && home.totalPesos < stats.previousMonthTotalPesos ->
            "Mejor que el mes pasado. ¡Seguila así!"
        else -> "Mes equilibrado. Mañana ponele el ojo igual."
    }
}
