package com.antigastos.boludos.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.dao.ExpenseListRow
import com.antigastos.boludos.ui.MainViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseListViewModel(
    private val repository: com.antigastos.boludos.data.ExpenseRepository,
    mainViewModel: MainViewModel,
) : ViewModel() {

    val rows: StateFlow<List<ExpenseListRow>> = mainViewModel.selectedMonth
        .flatMapLatest { repository.observeExpenseRows(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExpenseListViewModel(app.repository, main) as T
                }
            }
    }
}
