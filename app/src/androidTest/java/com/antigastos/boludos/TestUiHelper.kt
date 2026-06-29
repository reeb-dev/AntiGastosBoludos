package com.antigastos.boludos

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import java.time.YearMonth
import kotlinx.coroutines.runBlocking

object TestUiHelper {

    fun prepareForUiTests(app: AntiGastosApplication) {
        runBlocking {
            app.database.expenseDao().clearAll()
            app.settingsRepository.ensureRow()
            app.settingsRepository.update {
                it.copy(
                    onboardingDone = true,
                    personaQuizDone = true,
                    personaWelcomeShown = true,
                    homeTipsShown = true,
                    biometricLockEnabled = false,
                    aiPhrasesEnabled = false,
                    userDisplayName = "Tester",
                    personaKey = "termo",
                    crushCooldownEnabled = false,
                    lastRecapPromptMonth = YearMonth.now().toString(),
                )
            }
            app.repository.ensureCategoriesSeeded()
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForHome(
        timeoutMillis: Long = 20_000L,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithTag("fab_add_expense", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                onAllNodesWithText("Agregar gasto", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.dismissOverlays() {
        listOf("Después", "Entendido", "Cerrar").forEach { label ->
            val nodes = onAllNodesWithText(label, useUnmergedTree = true)
                .fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                runCatching { onAllNodesWithText(label, useUnmergedTree = true)[0].performClick() }
            }
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.scrollToText(
        text: String,
        substring: Boolean = true,
    ) {
        onAllNodesWithText(text, substring = substring, useUnmergedTree = true)[0]
            .performScrollTo()
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.openSettings() {
        onAllNodesWithContentDescription("Ajustes", useUnmergedTree = true)[0].performClick()
        waitUntil(10_000) {
            onAllNodesWithText("Ajustes", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        scrollToText("Frases con IA")
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.confirmExpenseIfNeeded() {
        listOf("Cargarlo igual", "Guardar ya").forEach { label ->
            val nodes = onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                runCatching { onAllNodesWithText(label, useUnmergedTree = true)[0].performClick() }
            }
        }
    }
}
