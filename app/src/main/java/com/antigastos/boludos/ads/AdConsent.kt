package com.antigastos.boludos.ads

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Consentimiento de publicidad (UMP) antes de cargar anuncios.
 * Obligatorio para distribución en EEA/Reino Unido.
 */
object AdConsent {

    fun requestAndInit(activity: Activity, onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    onComplete()
                }
            },
            { onComplete() },
        )
    }

    /** Reabrir opciones de privacidad de anuncios (requerido en EEA si el usuario revoca). */
    fun showPrivacyOptions(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismiss() }
    }

    fun isPrivacyOptionsRequired(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
}
