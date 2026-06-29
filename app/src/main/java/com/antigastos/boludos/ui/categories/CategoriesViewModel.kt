package com.antigastos.boludos.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val app: AntiGastosApplication) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = app.repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, slug: String) {
        viewModelScope.launch {
            val safeSlug = slug.trim().ifBlank { name.trim().lowercase().replace(" ", "_") }
            val existing = app.repository.getCategoryBySlug(safeSlug)
            if (existing != null) return@launch
            val sortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            app.repository.upsertCategory(
                CategoryEntity(
                    name = name.trim(),
                    slug = safeSlug,
                    iconName = "more",
                    sortOrder = sortOrder,
                ),
            )
        }
    }

    fun delete(c: CategoryEntity) {
        viewModelScope.launch { app.repository.deleteCategory(c) }
    }

    companion object {
        fun factory(app: AntiGastosApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CategoriesViewModel(app) as T
                }
            }
    }
}
