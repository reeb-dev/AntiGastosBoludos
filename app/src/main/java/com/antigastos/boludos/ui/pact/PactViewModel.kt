package com.antigastos.boludos.ui.pact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.PactEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PactUiState(
    val pacts: List<PactEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
)

class PactViewModel(private val app: AntiGastosApplication) : ViewModel() {

    val uiState: StateFlow<PactUiState> = combine(
        app.pactRepository.observe(),
        app.repository.observeCategories(),
    ) { p, c -> PactUiState(p, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PactUiState())

    fun create(
        slug: String,
        days: Int,
        friendName: String? = null,
        stakeText: String? = null,
    ) {
        viewModelScope.launch {
            app.pactRepository.create(slug, days, friendName, stakeText)
        }
    }

    companion object {
        fun factory(app: AntiGastosApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PactViewModel(app) as T
                }
            }
    }
}
