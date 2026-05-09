package com.antigastos.boludos.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.model.StatsSnapshot
import com.antigastos.boludos.ui.MainViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    private val repository: com.antigastos.boludos.data.ExpenseRepository,
    mainViewModel: MainViewModel,
) : ViewModel() {

    val snapshot: StateFlow<StatsSnapshot?> = mainViewModel.selectedMonth
        .flatMapLatest { repository.observeStats(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(app.repository, main) as T
                }
            }
    }
}
