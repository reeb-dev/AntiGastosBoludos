package com.antigastos.boludos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.TestUiHelper
import com.antigastos.boludos.TestUiHelper.dismissOverlays
import com.antigastos.boludos.TestUiHelper.openSettings
import com.antigastos.boludos.TestUiHelper.waitForHome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationUiTest {

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
    fun home_showsFabAndInicioTab() {
        composeRule.onAllNodesWithText("Inicio", useUnmergedTree = true)[0]
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Agregar gasto", useUnmergedTree = true)[0]
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_listTab_showsHistorial() {
        composeRule.onAllNodesWithText("Lista", useUnmergedTree = true)[0].performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Historial", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun bottomNav_chatTab_showsPersonas() {
        composeRule.onAllNodesWithText("Chat", useUnmergedTree = true)[0].performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Chatealos", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun topBar_settings_opensSettingsScreen() {
        composeRule.openSettings()
        composeRule.onAllNodesWithText("Frases con IA", substring = true, useUnmergedTree = true)[0]
            .assertIsDisplayed()
    }
}
