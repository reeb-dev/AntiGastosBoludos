package com.antigastos.boludos.ads

import com.antigastos.boludos.BuildConfig

/**
 * IDs de AdMob alineados con los nombres de bloques en la consola.
 * En debug se usan IDs de prueba de Google; en release, los de producción.
 */
object AdMobIds {

  /** Consola: Anti-Gastos-Boludos */
  const val APPLICATION_ID = "ca-app-pub-9783550633423906~4815321949"

  // —— Producción (pegar cada ID al crear el bloque en AdMob) ——
  /** AR — Banner — Lista gastos — pie (≤5 ítems) */
  const val BANNER_LIST_PIE = "ca-app-pub-9783550633423906/7764918913"

  /** AR — Banner — Stats — pie de pantalla */
  val bannerStatsPieProd: String = "ca-app-pub-9783550633423906/4688163583"

  /** AR — Nativo — Lista gastos — tras 5º ítem */
  val nativeListAfter5Prod: String = "ca-app-pub-9783550633423906/7196960169"

  /** AR — Rewarded — Home — escudo de racha gratis */
  val rewardedStreakShieldProd: String = "ca-app-pub-9783550633423906/5210907769"

  /** AR — Rewarded — Lotería — duplicar premio diario */
  val rewardedLoteriaDoubleProd: String = "ca-app-pub-9783550633423906/6659720807"

  /** AR — Rewarded — Ruleta — giro extra del día */
  val rewardedRuletaExtraProd: String = "ca-app-pub-9783550633423906/1407394124"

  // —— Test (Google) ——
  private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
  private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
  private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

  val bannerListPie: String
    get() = if (BuildConfig.DEBUG) TEST_BANNER else BANNER_LIST_PIE

  /** Stats: bloque propio si existe; si no, reutiliza el banner de lista. */
  val bannerStatsPie: String
    get() = if (BuildConfig.DEBUG) {
      TEST_BANNER
    } else {
      bannerStatsPieProd.ifBlank { BANNER_LIST_PIE }
    }

  val nativeListAfter5: String
    get() = if (BuildConfig.DEBUG) TEST_NATIVE else nativeListAfter5Prod.ifBlank { TEST_NATIVE }

  val rewardedStreakShield: String
    get() = if (BuildConfig.DEBUG) TEST_REWARDED else rewardedStreakShieldProd.ifBlank { TEST_REWARDED }

  val rewardedLoteriaDouble: String
    get() = if (BuildConfig.DEBUG) TEST_REWARDED else rewardedLoteriaDoubleProd.ifBlank { TEST_REWARDED }

  val rewardedRuletaExtra: String
    get() = if (BuildConfig.DEBUG) TEST_REWARDED else rewardedRuletaExtraProd.ifBlank { TEST_REWARDED }
}
