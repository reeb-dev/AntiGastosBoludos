package com.antigastos.boludos.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.TestUiHelper
import com.antigastos.boludos.TestUiHelper.waitForHome
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashBypassUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun splash_skipped_reachesHomeQuickly() {
        val app = composeRule.activity.application as AntiGastosApplication
        TestUiHelper.prepareForUiTests(app)

        val startMs = System.currentTimeMillis()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForHome(timeoutMillis = 4_000L)
        val elapsedMs = System.currentTimeMillis() - startMs

        assertTrue(
            "Home tardó ${elapsedMs}ms; el splash de 1,6s no se omitió en tests",
            elapsedMs < 3_000L,
        )
    }
}
