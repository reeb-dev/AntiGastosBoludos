package com.antigastos.boludos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.domain.AchievementMeta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

class MainViewModel : ViewModel() {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    /**
     * Ruta pendiente de navegar al iniciar (alimentada por deep links de
     * notificaciones). El NavHost la consume vía [consumePendingRoute].
     */
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /** Auto-lock: pedir biometría de nuevo sin cerrar sesión (no hay login). */
    private val _appLockRequested = MutableStateFlow(false)
    val appLockRequested: StateFlow<Boolean> = _appLockRequested.asStateFlow()

    fun requestAppLock() {
        _appLockRequested.value = true
    }

    fun clearAppLockRequest() {
        _appLockRequested.value = false
    }

    /**
     * Logro mostrado actualmente en pantalla. Solo uno a la vez para no
     * abusar al usuario. Si llegan más en el mismo lote se cuentan como
     * "extras" silenciosos visibles en [pendingExtras].
     */
    private val _currentAchievement = MutableStateFlow<AchievementMeta?>(null)
    val currentAchievement: StateFlow<AchievementMeta?> = _currentAchievement.asStateFlow()

    /** Cuántos logros más quedaron sin celebrar pero ya guardados en Trofeos. */
    private val _pendingExtras = MutableStateFlow(0)
    val pendingExtras: StateFlow<Int> = _pendingExtras.asStateFlow()

    /** Bus de avisos cortos (snackbar) provocados por el motor de logros. */
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    private val _legendaryDropVisible = MutableStateFlow(false)
    val legendaryDropVisible: StateFlow<Boolean> = _legendaryDropVisible.asStateFlow()

    /** Anti-spam: marcamos cuándo se mostró el último modal celebratorio. */
    private var lastModalShownAtMs: Long = 0L

    fun setMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(delta)
    }

    fun postPendingRoute(route: String?) {
        _pendingRoute.value = route
    }

    fun consumePendingRoute(): String? {
        val r = _pendingRoute.value
        _pendingRoute.value = null
        return r
    }

    /** Snackbar global (shell de la app). */
    fun postSnack(message: String) {
        viewModelScope.launch {
            _toasts.emit(message)
        }
    }

    /**
     * Recibe nuevos logros desbloqueados. Sólo muestra UN modal por minuto;
     * el resto se acumula silenciosamente en [pendingExtras] y se anuncia
     * via snackbar para que el usuario sepa que tiene cosas en Trofeos.
     */
    fun enqueueAchievementUnlocks(new: List<AchievementMeta>) {
        if (new.isEmpty()) return
        val now = System.currentTimeMillis()
        val canShowModal = _currentAchievement.value == null &&
            (now - lastModalShownAtMs) > MODAL_COOLDOWN_MS

        if (canShowModal) {
            _currentAchievement.value = new.first()
            lastModalShownAtMs = now
            val extras = new.size - 1
            if (extras > 0) {
                _pendingExtras.value = _pendingExtras.value + extras
                viewModelScope.launch {
                    _toasts.emit("+$extras trofeos más esperándote en Trofeos.")
                }
            }
        } else {
            _pendingExtras.value = _pendingExtras.value + new.size
            viewModelScope.launch {
                _toasts.emit("+${new.size} trofeos esperándote en Trofeos.")
            }
        }
    }

    fun dismissCurrentAchievement() {
        _currentAchievement.value = null
        _pendingExtras.value = 0
    }

    /** Compatibilidad: API previa basada en cola. */
    @Deprecated("Usá currentAchievement + dismissCurrentAchievement", ReplaceWith("dismissCurrentAchievement()"))
    fun dismissFirstAchievement() = dismissCurrentAchievement()

    fun showLegendaryDrop() {
        // El drop legendario también respeta el cooldown global del modal.
        val now = System.currentTimeMillis()
        if (_currentAchievement.value != null) return
        if ((now - lastModalShownAtMs) < MODAL_COOLDOWN_MS / 2) return
        _legendaryDropVisible.value = true
        lastModalShownAtMs = now
    }

    fun consumeLegendaryDrop() {
        _legendaryDropVisible.value = false
    }

    /**
     * Precarga el formulario de Suscripciones cuando el usuario acepta
     * una sugerencia de gasto fijo “fantasma” desde el Home.
     */
    data class SubscriptionFormPrefill(
        val name: String,
        val amountPesos: Long,
        val categoryId: Long,
        val dayOfMonth: Int,
    )

    private val _subscriptionPrefill = MutableStateFlow<SubscriptionFormPrefill?>(null)
    val subscriptionPrefill: StateFlow<SubscriptionFormPrefill?> = _subscriptionPrefill.asStateFlow()

    fun postSubscriptionPrefill(p: SubscriptionFormPrefill) {
        _subscriptionPrefill.value = p
    }

    fun consumeSubscriptionPrefill() {
        _subscriptionPrefill.value = null
    }

    companion object {
        /**
         * Cooldown entre modales celebratorios. Subir si la app sigue
         * resultando "ruidosa", bajar si parece muy avara con la celebración.
         */
        private const val MODAL_COOLDOWN_MS: Long = 60_000L
    }
}
