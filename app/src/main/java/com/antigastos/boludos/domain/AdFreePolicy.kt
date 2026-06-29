package com.antigastos.boludos.domain

import com.antigastos.boludos.data.local.entity.SettingsEntity

/** La publicidad no se desactiva por donaciones; siempre se muestran ads. */
object AdFreePolicy {

    fun isAdFreeActive(settings: SettingsEntity, nowMillis: Long = System.currentTimeMillis()): Boolean = false

    fun shouldShowAds(settings: SettingsEntity, nowMillis: Long = System.currentTimeMillis()): Boolean = true

    fun statusLabel(settings: SettingsEntity): String = ""
}
