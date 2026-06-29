package com.antigastos.boludos.ui.bolsillo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.ExpenseRepository
import com.antigastos.boludos.data.SettingsRepository
import com.antigastos.boludos.data.local.entity.BudgetEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.Forecast
import com.antigastos.boludos.domain.ForecastEngine
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ui.MainViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BolsilloUiState(
    val home: HomeUiModel? = null,
    val budget: BudgetEntity? = null,
    val forecast: Forecast? = null,
    val settings: SettingsEntity = SettingsEntity(),
    /** Total que la pareja le debe al usuario en el mes (pesos). 0 = nada. */
    val partnerOwesPesos: Long = 0L,
    val partnerSharedCount: Long = 0L,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BolsilloViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val mainViewModel: MainViewModel,
) : ViewModel() {

    val uiState: StateFlow<BolsilloUiState> = mainViewModel.selectedMonth
        .flatMapLatest { ym ->
            combine(
                repository.observeHome(ym),
                repository.observeBudget(ym),
                settingsRepository.flow,
            ) { home, budget, settings ->
                val (owed, count) = if (settings.partnerEnabled) {
                    val (start, end) = monthRangeMillis(ym)
                    val o = runCatching { repository.partnerOwedPesosInRange(start, end) }.getOrDefault(0L)
                    val c = runCatching { repository.countSharedExpensesInRange(start, end) }.getOrDefault(0L)
                    o to c
                } else {
                    0L to 0L
                }
                BolsilloUiState(
                    home = home,
                    budget = budget,
                    forecast = ForecastEngine.forecast(home),
                    settings = settings,
                    partnerOwesPesos = owed,
                    partnerSharedCount = count,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BolsilloUiState())

    /**
     * Convierte un mes en (startMillis, endMillis) zona local para los
     * agregados de gastos compartidos. Lo dejamos privado porque el resto
     * del codebase ya tiene su propio rango.
     */
    private fun monthRangeMillis(ym: YearMonth): Pair<Long, Long> {
        val zone = java.time.ZoneId.systemDefault()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun saveBudget(salaryPesos: Long, otherIncomePesos: Long, fixedExpensesPesos: Long) {
        val ym: YearMonth = mainViewModel.selectedMonth.value
        viewModelScope.launch {
            repository.upsertBudget(
                yearMonth = ym,
                salaryPesos = salaryPesos,
                otherIncomePesos = otherIncomePesos,
                fixedExpensesPesos = fixedExpensesPesos,
            )
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BolsilloViewModel(app.repository, app.settingsRepository, main) as T
                }
            }
    }
}
