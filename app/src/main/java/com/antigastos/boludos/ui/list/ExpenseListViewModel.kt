package com.antigastos.boludos.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.dao.ExpenseListRow
import com.antigastos.boludos.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseListUiState(
    val rows: List<ExpenseListRow> = emptyList(),
    val query: String = "",
    val activeCategorySlug: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(
    private val repository: com.antigastos.boludos.data.ExpenseRepository,
    mainViewModel: MainViewModel,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeCategory = MutableStateFlow<String?>(null)
    val activeCategory: StateFlow<String?> = _activeCategory.asStateFlow()

    val rows: StateFlow<List<ExpenseListRow>> = mainViewModel.selectedMonth
        .flatMapLatest { repository.observeExpenseRows(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<ExpenseListUiState> = combine(rows, _query, _activeCategory) { all, q, slug ->
        val qLower = q.trim().lowercase()
        val filtered = all.filter { row ->
            (slug == null || row.categorySlug == slug) &&
                (qLower.isEmpty() ||
                    row.categoryName.lowercase().contains(qLower) ||
                    (row.note?.lowercase()?.contains(qLower) == true) ||
                    row.amountPesos.toString().contains(qLower))
        }
        ExpenseListUiState(filtered, q, slug)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setCategoryFilter(slug: String?) {
        _activeCategory.value = if (_activeCategory.value == slug) null else slug
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteExpense(id) }
    }

    /** Crea un gasto idéntico a [id] con timestamp actual. */
    fun duplicate(id: Long) {
        viewModelScope.launch {
            val src = repository.getExpense(id) ?: return@launch
            repository.insertExpense(
                amountPesos = src.amountPesos,
                categoryId = src.categoryId,
                note = src.note,
                occurredAt = System.currentTimeMillis(),
            )
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
