package com.antigastos.boludos.ui.donate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.DonationBankConfig
import com.antigastos.boludos.domain.DonationTier
import com.antigastos.boludos.domain.MercadoPagoLauncher
import com.antigastos.boludos.domain.MonetizationEstimator
import com.antigastos.boludos.domain.MonetizationSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DonateUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val donationConfigured: Boolean = false,
    val mercadoPagoInstalled: Boolean = false,
    val monetization: MonetizationSnapshot = MonetizationEstimator.snapshot(SettingsEntity()),
    val message: String? = null,
)

class DonateViewModel(
    private val app: AntiGastosApplication,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _mpInstalled = MutableStateFlow(false)
    private val _donationConfigured = MutableStateFlow(false)

    val uiState: StateFlow<DonateUiState> = combine(
        app.settingsRepository.flow,
        _message,
        _mpInstalled,
        _donationConfigured,
    ) { settings, msg, mpInstalled, configured ->
        DonateUiState(
            settings = settings,
            donationConfigured = configured,
            mercadoPagoInstalled = mpInstalled,
            monetization = MonetizationEstimator.snapshot(settings),
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DonateUiState())

    init {
        viewModelScope.launch {
            app.settingsRepository.ensureDonationReferenceCode()
        }
    }

    fun refreshMercadoPagoInstalled(context: Context) {
        _donationConfigured.value = DonationBankConfig.isConfigured(context)
        _mpInstalled.value = runCatching {
            context.packageManager.getPackageInfo("com.mercadopago.wallet", 0)
            true
        }.getOrDefault(false)
    }

    fun openMercadoPago(context: Context, tier: DonationTier?) {
        _message.value = MercadoPagoLauncher.openTransfer(context, tier?.amountPesos)
    }

    fun openMercadoPagoFreeAmount(context: Context) {
        _message.value = MercadoPagoLauncher.openTransfer(context, amountPesos = null)
    }

    fun copyAlias(context: Context) {
        _message.value = MercadoPagoLauncher.copyAlias(context)
    }

    fun copyCbu(context: Context) {
        _message.value = MercadoPagoLauncher.copyCbu(context)
    }

    fun thankYou(tier: DonationTier?) {
        _message.value = buildString {
            append("¡Gracias por el cafecito! ☕")
            tier?.let { append(" ${it.emoji}") }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun factory(app: AntiGastosApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DonateViewModel(app) as T
            }
    }
}
