package com.antigastos.boludos.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Abre Mercado Pago para transferir al alias configurado.
 */
object MercadoPagoLauncher {

    private const val MP_PACKAGE = "com.mercadopago.wallet"
    private const val MP_PLAY_STORE = "market://details?id=$MP_PACKAGE"
    private const val TRANSFER_BASE = "https://www.mercadopago.com.ar/money-out/transfer/alias"

    fun openTransfer(context: Context, amountPesos: Long?): String {
        if (!DonationBankConfig.isConfigured(context)) {
            return "Las donaciones no están disponibles en esta versión."
        }
        val alias = DonationBankConfig.alias(context)
        val transferUrl = buildTransferWebUrl(alias, amountPesos)
        val candidates = listOf(
            buildUniversalLink(transferUrl),
            transferUrl,
            "mercadopago://webview?url=${encode(transferUrl)}",
            "mercadopago://money-out/transfer/alias?alias=${encode(alias)}",
        )
        for (url in candidates) {
            if (tryViewIntent(context, url)) {
                return buildOpenedMessage(amountPesos)
            }
        }
        if (isAppInstalled(context)) {
            if (tryLaunchApp(context)) {
                return buildOpenedMessage(amountPesos) +
                    " Si no ves el monto, elegilo en la app."
            }
        }
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, MP_PLAY_STORE.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "Instalá Mercado Pago para transferir el cafecito."
        } catch (_: ActivityNotFoundException) {
            "No pude abrir Mercado Pago. Instalalo desde Play Store."
        }
    }

    fun copyAlias(context: Context): String {
        val alias = DonationBankConfig.alias(context)
        if (alias.isBlank()) return "No hay alias configurado."
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("alias", alias))
        return "Alias copiado: $alias"
    }

    fun copyCbu(context: Context): String {
        val cbu = DonationBankConfig.cbu(context)
        if (cbu.length != 22) return "CBU no configurado."
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("cbu", cbu))
        return "CBU copiado"
    }

    private fun buildTransferWebUrl(alias: String, amountPesos: Long?): String {
        val params = buildList {
            add("alias=${encode(alias)}")
            if (amountPesos != null && amountPesos > 0L) {
                add("amount=$amountPesos")
            }
        }
        return "$TRANSFER_BASE?${params.joinToString("&")}"
    }

    private fun buildUniversalLink(targetUrl: String): String {
        val encoded = encode(targetUrl)
        val androidDeep = encode("mercadopago://webview?url=$encoded")
        return "https://www.mercadopago.com.ar/onlinepayments/universal-link" +
            "?fallback=$encoded&android=$androidDeep"
    }

    private fun tryViewIntent(context: Context, url: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (url.startsWith("http")) {
            intent.setPackage(MP_PACKAGE)
            if (intent.resolveActivity(context.packageManager) == null) {
                intent.setPackage(null)
            }
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun tryLaunchApp(context: Context): Boolean = try {
        val launch = context.packageManager.getLaunchIntentForPackage(MP_PACKAGE) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun isAppInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(MP_PACKAGE, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun buildOpenedMessage(amountPesos: Long?): String = if (amountPesos != null && amountPesos > 0L) {
        "Abriendo Mercado Pago con ${MoneyFormat.formatPesos(amountPesos)}…"
    } else {
        "Abriendo Mercado Pago… elegí el monto en la app."
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
