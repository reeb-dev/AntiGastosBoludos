package com.antigastos.boludos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.TestUiHelper
import com.antigastos.boludos.TestUiHelper.dismissOverlays
import com.antigastos.boludos.TestUiHelper.openSettings
import com.antigastos.boludos.TestUiHelper.scrollToText
import com.antigastos.boludos.TestUiHelper.waitForHome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPrivacyUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val app = composeRule.activity.application as AntiGastosApplication
        TestUiHelper.prepareForUiTests(app)
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForHome()
        composeRule.dismissOverlays()
    }

    @Test
    fun settings_privacyPolicyScreen_opens() {
        composeRule.openSettings()
        composeRule.scrollToText("Ver política de privacidad", substring = false)
        composeRule.onAllNodesWithText("Ver política de privacidad", useUnmergedTree = true)[0]
            .performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Abrir versión web", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithText("Política de privacidad", useUnmergedTree = true)[0]
            .assertIsDisplayed()
    }

    @Test
    fun settings_toggleAiPhrases_visible() {
        composeRule.openSettings()
        composeRule.scrollToText("Frases con IA")
        composeRule.onAllNodesWithText("Frases con IA", substring = true, useUnmergedTree = true)[0]
            .assertIsDisplayed()
    }
}
