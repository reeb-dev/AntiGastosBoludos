package com.antigastos.boludos.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class MainViewModel : ViewModel() {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun setMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(delta)
    }
}
