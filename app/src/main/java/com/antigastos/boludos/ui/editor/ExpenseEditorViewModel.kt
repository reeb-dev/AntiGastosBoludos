package com.antigastos.boludos.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseEditorUiState(
    val amountDigits: String,
    val selectedCategoryId: Long?,
    val note: String,
    val categories: List<CategoryEntity>,
    val loading: Boolean,
    val isEdit: Boolean,
)

class ExpenseEditorViewModel(
    private val repository: com.antigastos.boludos.data.ExpenseRepository,
    private val expenseId: Long?,
) : ViewModel() {

    private val _amountDigits = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _note = MutableStateFlow("")
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val _loading = MutableStateFlow(expenseId != null)

    private val _uiState = MutableStateFlow(
        ExpenseEditorUiState(
            amountDigits = "",
            selectedCategoryId = null,
            note = "",
            categories = emptyList(),
            loading = expenseId != null,
            isEdit = expenseId != null,
        ),
    )
    val uiState: StateFlow<ExpenseEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { list ->
                _categories.value = list
                if (_selectedCategoryId.value == null && list.isNotEmpty()) {
                    _selectedCategoryId.value = list.first().id
                }
                emitState()
            }
        }
        if (expenseId != null) {
            viewModelScope.launch {
                try {
                    val expense = repository.getExpense(expenseId)
                    if (expense != null) {
                        _amountDigits.value = expense.amountPesos.toString()
                        _selectedCategoryId.value = expense.categoryId
                        _note.value = expense.note.orEmpty()
                    }
                } finally {
                    _loading.value = false
                    emitState()
                }
            }
        }
    }

    fun setAmountDigits(raw: String) {
        _amountDigits.value = raw.filter { it.isDigit() }.take(9)
        emitState()
    }

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
        emitState()
    }

    fun setNote(text: String) {
        _note.value = text
        emitState()
    }

    private fun emitState() {
        _uiState.value = ExpenseEditorUiState(
            amountDigits = _amountDigits.value,
            selectedCategoryId = _selectedCategoryId.value,
            note = _note.value,
            categories = _categories.value,
            loading = _loading.value,
            isEdit = expenseId != null,
        )
    }

    fun save(onDone: () -> Unit) {
        val pesos = _amountDigits.value.toLongOrNull() ?: return
        if (pesos <= 0L) return
        val cat = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (expenseId == null) {
                repository.insertExpense(pesos, cat, _note.value.takeIf { it.isNotBlank() }, now)
            } else {
                repository.updateExpense(expenseId, pesos, cat, _note.value.takeIf { it.isNotBlank() }, now)
            }
            onDone()
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication, expenseId: Long?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExpenseEditorViewModel(app.repository, expenseId) as T
                }
            }
    }
}
