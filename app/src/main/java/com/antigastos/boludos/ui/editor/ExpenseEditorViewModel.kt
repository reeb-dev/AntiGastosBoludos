package com.antigastos.boludos.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CategoryRules
import com.antigastos.boludos.domain.CrushCooldownRules
import com.antigastos.boludos.domain.LifeHoursEngine
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.notifications.Notifier
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.work.CrushScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

data class ExpenseEditorUiState(
    val amountDigits: String,
    val selectedCategoryId: Long?,
    val note: String,
    val categories: List<CategoryEntity>,
    val loading: Boolean,
    val isEdit: Boolean,
    val suggestedSlug: String? = null,
    /** Resultado del SpendingBudgetEngine evaluado contra el monto actual. */
    val spendCheck: SpendingBudgetEngine.SpendCheck? = null,
    val askConfirmation: Boolean = false,
    /** Mensaje a mostrar al usuario (snackbar o inline). */
    val errorMessage: String? = null,
    /** Mientras true, deshabilitamos guardar para evitar dobles taps. */
    val saving: Boolean = false,
    /** Modo pareja: visible si el usuario lo prendió en Ajustes. */
    val partnerEnabled: Boolean = false,
    val partnerName: String = "",
    /**
     * Si no es null, este gasto se va a guardar como compartido y este es
     * el % a cargo del usuario actual. Default = `partnerDefaultSplit`.
     */
    val sharedSplitPercent: Int? = null,
    /** Default que toma el toggle al activarse (de Settings). */
    val partnerDefaultSplit: Int = 50,
    /** “Costo en horas de tu vida” bajo el campo monto. */
    val lifeHoursLine: String? = null,
    /** Diálogo heladera 24 h (antojo). */
    val askCrushChoice: Boolean = false,
)

class ExpenseEditorViewModel(
    private val app: AntiGastosApplication,
    private val expenseId: Long?,
    private val mainViewModel: MainViewModel,
) : ViewModel() {

    private val repository = app.repository

    private val _amountDigits = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _note = MutableStateFlow("")
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val _loading = MutableStateFlow(expenseId != null)
    private val _suggestedSlug = MutableStateFlow<String?>(null)
    private val _spendCheck = MutableStateFlow<SpendingBudgetEngine.SpendCheck?>(null)
    private val _askConfirmation = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _saving = MutableStateFlow(false)
    private val _partnerEnabled = MutableStateFlow(false)
    private val _partnerName = MutableStateFlow("")
    private val _partnerDefaultSplit = MutableStateFlow(50)
    private val _sharedSplitPercent = MutableStateFlow<Int?>(null)
    private val _settings = MutableStateFlow(SettingsEntity())
    private val _askCrushChoice = MutableStateFlow(false)
    private var skipCrushOnce = false

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
            try {
                repository.observeCategories().collect { list ->
                    _categories.value = list
                    if ((_selectedCategoryId.value == null) && list.isNotEmpty()) {
                        _selectedCategoryId.value = list.first().id
                    }
                    emitState()
                }
            } catch (t: Throwable) {
                _error.value = "No pude cargar las categorías. Probá reabriendo la pantalla."
                emitState()
            }
        }
        // Settings: pareja + snapshot para antojo / horas de vida.
        viewModelScope.launch {
            app.settingsRepository.flow.collect { settings ->
                _settings.value = settings
                _partnerEnabled.value = settings.partnerEnabled && settings.partnerName.isNotBlank()
                _partnerName.value = settings.partnerName
                _partnerDefaultSplit.value = settings.partnerDefaultSplit.coerceIn(1, 99)
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
                        _sharedSplitPercent.value = expense.sharedSplitPercent
                    } else {
                        _error.value = "Este gasto ya no existe (te lo borraron desde otro lado)."
                    }
                } catch (t: Throwable) {
                    _error.value = "No pude leer el gasto: ${t.message ?: "error desconocido"}"
                } finally {
                    _loading.value = false
                    emitState()
                }
            }
        }
    }

    /**
     * Activa o desactiva el split de pareja para este gasto. Cuando lo
     * activás, arrancamos con el % default del usuario; cuando lo apagás,
     * el gasto vuelve a ser 100% propio.
     */
    fun setShared(enabled: Boolean) {
        _sharedSplitPercent.value = if (enabled) _partnerDefaultSplit.value else null
        emitState()
    }

    /** Actualiza el % cuando el usuario mueve el slider del editor. */
    fun setSharedSplitPercent(p: Int) {
        if (_sharedSplitPercent.value == null) return
        _sharedSplitPercent.value = p.coerceIn(1, 99)
        emitState()
    }

    fun setAmountDigits(raw: String) {
        // Tope 11 dígitos = ~99 mil millones de pesos. Sobra para pesos
        // argentinos con inflación y evita overflows del Long.
        val cleaned = raw.filter { it.isDigit() }.dropWhile { it == '0' }.take(11)
        _amountDigits.value = cleaned
        recomputeSpendCheckDetached()
        emitState()
    }

    fun consumeError() {
        _error.value = null
        emitState()
    }

    /**
     * Recalcula el "spend check" contra el monto actual sin bloquear el hilo.
     * Útil para mostrar warning en vivo en el editor.
     */
    private fun recomputeSpendCheckDetached() {
        val amount = _amountDigits.value.toLongOrNull() ?: 0L
        if (amount <= 0L) {
            _spendCheck.value = null
            return
        }
        viewModelScope.launch {
            try {
                val home = repository.observeHome(YearMonth.now()).first()
                val check = SpendingBudgetEngine.checkSpend(home, amount, LocalDate.now())
                _spendCheck.value = check
                emitState()
            } catch (_: Throwable) {
                // best effort
            }
        }
    }

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
        emitState()
    }

    fun setNote(text: String) {
        _note.value = text
        runAutoCategorize(text)
        emitState()
    }

    private fun runAutoCategorize(text: String) {
        val slug = CategoryRules.suggestSlug(text)
        _suggestedSlug.value = slug
        if (slug != null) {
            val match = _categories.value.firstOrNull { it.slug == slug }
            if (match != null && _selectedCategoryId.value != match.id) {
                _selectedCategoryId.value = match.id
            }
        }
    }

    private fun emitState() {
        val amount = _amountDigits.value.toLongOrNull()
        val rate = LifeHoursEngine.hourlyRatePesos(
            _settings.value.monthlyNetIncomePesos,
            _settings.value.weeklyWorkHours,
        )
        val lifeLine = if (
            _settings.value.lifeHoursEnabled &&
            rate != null &&
            amount != null &&
            amount > 0L
        ) {
            "${LifeHoursEngine.formatLifeHours(amount, rate)} de tu laburo · " +
                LifeHoursEngine.formatHourlyRateLabel(rate)
        } else {
            null
        }
        _uiState.value = ExpenseEditorUiState(
            amountDigits = _amountDigits.value,
            selectedCategoryId = _selectedCategoryId.value,
            note = _note.value,
            categories = _categories.value,
            loading = _loading.value,
            isEdit = expenseId != null,
            suggestedSlug = _suggestedSlug.value,
            spendCheck = _spendCheck.value,
            askConfirmation = _askConfirmation.value,
            errorMessage = _error.value,
            saving = _saving.value,
            partnerEnabled = _partnerEnabled.value,
            partnerName = _partnerName.value,
            partnerDefaultSplit = _partnerDefaultSplit.value,
            sharedSplitPercent = _sharedSplitPercent.value,
            lifeHoursLine = lifeLine,
            askCrushChoice = _askCrushChoice.value,
        )
    }

    fun dismissConfirmation() {
        _askConfirmation.value = false
        emitState()
    }

    fun dismissCrushChoice() {
        _askCrushChoice.value = false
        emitState()
    }

    /** El usuario eligió guardar ya (sin heladera). */
    fun saveNowAfterCrushChoice(onDone: () -> Unit) {
        _askCrushChoice.value = false
        skipCrushOnce = true
        save(onDone)
    }

    /** Mandar a pendiente 24 h y salir del editor. */
    fun deferCrush24h(onDone: () -> Unit) {
        val pesos = _amountDigits.value.toLongOrNull()
        if (pesos == null || pesos <= 0L) {
            _error.value = "Cargá un monto antes de posponer."
            emitState()
            return
        }
        val cat = _selectedCategoryId.value
        if (cat == null) {
            _error.value = "Elegí una categoría."
            emitState()
            return
        }
        _saving.value = true
        emitState()
        viewModelScope.launch {
            try {
                val id = repository.insertPendingCrush(
                    pesos,
                    cat,
                    _note.value.takeIf { it.isNotBlank() },
                )
                CrushScheduler.schedule(app, id)
                _askCrushChoice.value = false
                skipCrushOnce = false
                mainViewModel.postSnack("Te aviso en 24 h. Resistite un toque.")
                onDone()
            } catch (t: Throwable) {
                _error.value = "No pude guardar el pendiente: ${t.message ?: "error"}"
            } finally {
                _saving.value = false
                emitState()
            }
        }
    }

    private fun qualifiesCrush(): Boolean {
        if (expenseId != null) return false
        val s = _settings.value
        val pesos = _amountDigits.value.toLongOrNull() ?: return false
        val slug = _categories.value.firstOrNull { it.id == _selectedCategoryId.value }?.slug ?: return false
        return CrushCooldownRules.qualifies(
            categorySlug = slug,
            amountPesos = pesos,
            minPesos = s.crushMinPesos,
            featureEnabled = s.crushCooldownEnabled,
            isNewExpense = true,
        )
    }

    fun save(onDone: () -> Unit) {
        if (_saving.value) return
        val pesos = _amountDigits.value.toLongOrNull()
        if (pesos == null || pesos <= 0L) {
            _error.value = "Cargá un monto en pesos antes de guardar."
            emitState()
            return
        }
        if (pesos > MAX_AMOUNT_PESOS) {
            _error.value = "El monto es demasiado grande. ¿Sobra un cero?"
            emitState()
            return
        }
        if (_selectedCategoryId.value == null) {
            _error.value = "Elegí una categoría antes de guardar."
            emitState()
            return
        }

        if (!skipCrushOnce && qualifiesCrush()) {
            _askCrushChoice.value = true
            emitState()
            return
        }
        skipCrushOnce = false

        // Si la última evaluación marca que se está zarpando muy feo, mostramos
        // el dialog en lugar de guardar directo. El usuario puede confirmar
        // con `confirmAndSave`.
        val check = _spendCheck.value
        if (check != null &&
            (check.verdict == SpendingBudgetEngine.Permission.RED ||
                check.verdict == SpendingBudgetEngine.Permission.FORBIDDEN)
        ) {
            _askConfirmation.value = true
            emitState()
            return
        }

        persist(onDone)
    }

    /** Forzar guardar saltando la confirmación de "estás zarpado". */
    fun confirmAndSave(onDone: () -> Unit) {
        _askConfirmation.value = false
        emitState()
        persist(onDone)
    }

    private fun persist(onDone: () -> Unit) {
        val pesos = _amountDigits.value.toLongOrNull() ?: return
        if (pesos <= 0L || pesos > MAX_AMOUNT_PESOS) return
        val cat = _selectedCategoryId.value ?: return
        _saving.value = true
        emitState()
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val isInsert = expenseId == null
                val split = _sharedSplitPercent.value
                if (isInsert) {
                    repository.insertExpense(
                        pesos,
                        cat,
                        _note.value.takeIf { it.isNotBlank() },
                        now,
                        sharedSplitPercent = split,
                    )
                } else {
                    repository.updateExpense(
                        expenseId!!,
                        pesos,
                        cat,
                        _note.value.takeIf { it.isNotBlank() },
                        now,
                        sharedSplitPercent = split,
                    )
                }
                if (isInsert) {
                    val sound = app.settingsRepository.getSnapshot().soundEnabled
                    AppFeedback.expenseSaved(app, sound)
                    if (Random.nextDouble() < 0.005) {
                        mainViewModel.showLegendaryDrop()
                        AppFeedback.legendaryDrop(app, sound)
                    }
                    fireExpenseNotificationDetached()
                    runCatching { checkPactAndAlerts(pesos, cat) }
                }
                onDone()
            } catch (t: Throwable) {
                _error.value = "No pude guardar: ${t.message ?: "error inesperado"}"
            } finally {
                _saving.value = false
                emitState()
            }
        }
    }

    private suspend fun checkPactAndAlerts(pesos: Long, categoryId: Long) {
        val cat = _categories.value.firstOrNull { it.id == categoryId } ?: return
        val broken = app.pactRepository.onExpenseInserted(cat.slug, System.currentTimeMillis())
        if (broken.isNotEmpty()) {
            Notifier.postFail(
                app,
                "❌ Rompiste un pacto",
                "Cargaste un gasto en ${cat.name} estando en pacto. Mañana arrancás de cero.",
            )
        }

        val ym = YearMonth.now()
        val home = repository.observeHome(ym).first()
        val pct = home.globalProgress?.toDouble() ?: 0.0
        if (pct in 0.8..0.999) {
            Notifier.postReminder(
                app,
                "⚠️ Cuidado con la guita",
                "Ya consumiste ${(pct * 100).toInt()}% de la meta del mes.",
            )
        }
    }

    private fun fireExpenseNotificationDetached() {
        app.applicationScope.launch {
            runCatching {
                val state = repository.observeHome(YearMonth.now()).first()
                Notifier.postExpenseSaved(
                    context = app,
                    title = "Gasto cargado",
                    body = state.personalityMessage,
                )
            }
        }
    }

    companion object {
        /**
         * Tope superior de monto: 100.000.000.000 pesos. Más que esto huele
         * a tipeo descuidado y se previene para evitar overflows en sumas
         * (`Long.MAX_VALUE` es ~9.2 trillones, pero el UX se rompe mucho antes).
         */
        const val MAX_AMOUNT_PESOS: Long = 100_000_000_000L

        fun factory(
            app: AntiGastosApplication,
            expenseId: Long?,
            main: MainViewModel,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExpenseEditorViewModel(app, expenseId, main) as T
                }
            }
    }
}
