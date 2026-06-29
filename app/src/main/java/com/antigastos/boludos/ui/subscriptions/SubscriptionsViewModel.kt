package com.antigastos.boludos.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.SubscriptionEntity
import com.antigastos.boludos.domain.AchievementsCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubsUiState(
    val items: List<SubscriptionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
)

class SubscriptionsViewModel(private val app: AntiGastosApplication) : ViewModel() {

    val uiState: StateFlow<SubsUiState> = combine(
        app.repository.observeSubscriptions(),
        app.repository.observeCategories(),
    ) { subs, cats -> SubsUiState(subs, cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubsUiState())

    fun add(
        name: String,
        amountPesos: Long,
        categoryId: Long,
        dayOfMonth: Int,
        installmentsTotal: Int? = null,
    ) {
        viewModelScope.launch {
            app.repository.upsertSubscription(
                SubscriptionEntity(
                    name = name.trim(),
                    amountPesos = amountPesos,
                    categoryId = categoryId,
                    dayOfMonth = dayOfMonth.coerceIn(1, 28),
                    active = true,
                    installmentsTotal = installmentsTotal?.takeIf { it > 0 },
                ),
            )
            app.achievementsRepository.unlock(AchievementsCatalog.SUBS_LOADED)
            app.repository.seedSubscriptionsForCurrentMonth()
        }
    }

    /**
     * Atajo: registrar un gasto en N cuotas. Internamente es una suscripción
     * con `installmentsTotal=n` y monto = total/n. La primera cuota se siembra
     * inmediatamente al guardar.
     */
    fun addInstallments(
        name: String,
        totalPesos: Long,
        categoryId: Long,
        installments: Int,
        dayOfMonth: Int,
    ) {
        if (installments <= 0 || totalPesos <= 0L) return
        val per = totalPesos / installments
        add(
            name = name,
            amountPesos = per,
            categoryId = categoryId,
            dayOfMonth = dayOfMonth,
            installmentsTotal = installments,
        )
    }

    fun toggle(sub: SubscriptionEntity) {
        viewModelScope.launch {
            app.repository.upsertSubscription(sub.copy(active = !sub.active))
        }
    }

    fun delete(sub: SubscriptionEntity) {
        viewModelScope.launch { app.repository.deleteSubscription(sub) }
    }

    companion object {
        fun factory(app: AntiGastosApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionsViewModel(app) as T
                }
            }
    }
}
