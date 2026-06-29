package com.antigastos.boludos.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.GoalEntity
import com.antigastos.boludos.ui.MainViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GoalsViewModel(
    private val repository: com.antigastos.boludos.data.ExpenseRepository,
    private val mainViewModel: MainViewModel,
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = mainViewModel.selectedMonth
        .flatMapLatest { repository.observeGoals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repository.observeCategories()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveGoal(categoryId: Long, limitPesos: Long) {
        if (limitPesos <= 0L) return
        viewModelScope.launch {
            val month = mainViewModel.selectedMonth.value
            repository.upsertGoal(month, categoryId, limitPesos)
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication, main: MainViewModel): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GoalsViewModel(app.repository, main) as T
                }
            }
    }
}
