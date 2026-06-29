package com.antigastos.boludos.domain

import android.content.Context
import com.antigastos.boludos.BuildConfig
import com.antigastos.boludos.R

/**
 * Alias/CBU para donaciones: BuildConfig (local.properties) o fallback en [donation_config.xml].
 */
object DonationBankConfig {

    fun alias(context: Context): String =
        BuildConfig.DONATION_ALIAS.trim().ifBlank { context.getString(R.string.donation_alias).trim() }

    fun cbu(context: Context): String {
        val raw = BuildConfig.DONATION_CBU.trim().ifBlank { context.getString(R.string.donation_cbu) }
        return raw.filter { it.isDigit() }
    }

    fun holderName(context: Context): String =
        BuildConfig.DONATION_HOLDER.trim().ifBlank { context.getString(R.string.donation_holder).trim() }

    fun isConfigured(context: Context): Boolean = alias(context).isNotBlank()

    fun formatCbuDisplay(context: Context): String {
        val cbu = cbu(context)
        if (cbu.length != 22) return cbu
        return "${cbu.take(8)} ${cbu.drop(8).take(8)} ${cbu.takeLast(6)}"
    }
}
