package com.antigastos.boludos.ui.crush

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.notifications.Notifier
import com.antigastos.boludos.work.CrushScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrushResolveUi(
    val amountPesos: Long,
    val categoryName: String,
    val note: String?,
)

class CrushResolveViewModel(
    private val app: AntiGastosApplication,
    private val crushId: Long,
) : ViewModel() {

    private val _ui = MutableStateFlow<CrushResolveUi?>(null)
    val uiState: StateFlow<CrushResolveUi?> = _ui.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    suspend fun reload() {
        val crush = app.repository.getPendingCrush(crushId)
        if (crush == null) {
            _ui.value = null
            _loaded.value = true
            return
        }
        val cat = app.database.categoryDao().getById(crush.categoryId)
        _ui.value = CrushResolveUi(
            amountPesos = crush.amountPesos,
            categoryName = cat?.name ?: "Categoría",
            note = crush.note,
        )
        _loaded.value = true
    }

    fun confirmLoad(onDone: () -> Unit) {
        viewModelScope.launch {
            val crush = app.repository.getPendingCrush(crushId)
            if (crush == null) {
                _error.value = "Este antojo ya no está pendiente."
                onDone()
                return@launch
            }
            val now = System.currentTimeMillis()
            app.repository.insertExpense(
                crush.amountPesos,
                crush.categoryId,
                crush.note,
                now,
                sharedSplitPercent = null,
            )
            val cat = app.database.categoryDao().getById(crush.categoryId)
            if (cat != null) {
                val broken = app.pactRepository.onExpenseInserted(cat.slug, now)
                if (broken.isNotEmpty()) {
                    Notifier.postFail(
                        app,
                        "❌ Rompiste un pacto",
                        "Cargaste un gasto en ${cat.name} estando en pacto.",
                    )
                }
            }
            app.repository.deletePendingCrush(crushId)
            CrushScheduler.cancel(app, crushId)
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            app.repository.deletePendingCrush(crushId)
            CrushScheduler.cancel(app, crushId)
            onDone()
        }
    }

    fun consumeError() {
        _error.value = null
    }

    companion object {
        fun factory(app: AntiGastosApplication, crushId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CrushResolveViewModel(app, crushId) as T
                }
            }
    }
}
