package com.antigastos.boludos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.TestUiHelper
import com.antigastos.boludos.TestUiHelper.dismissOverlays
import com.antigastos.boludos.TestUiHelper.fillAmountAndTapSave
import com.antigastos.boludos.TestUiHelper.openAddExpenseEditor
import com.antigastos.boludos.TestUiHelper.waitForExpenseInList
import com.antigastos.boludos.TestUiHelper.waitForHome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddExpenseUiTest {

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
    fun addExpense_appearsInList() {
        composeRule.openAddExpenseEditor()
        composeRule.fillAmountAndTapSave("500")

        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("fab_add_expense", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.waitForExpenseInList("500")
    }

    @Test
    fun addHighExpense_showsOverBudgetDialog_thenSaves() {
        composeRule.openAddExpenseEditor()
        composeRule.fillAmountAndTapSave("5000")

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Cargarlo igual", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithText("¿Seguro que querés cargarlo?", substring = true, useUnmergedTree = true)[0]
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Cargarlo igual", useUnmergedTree = true)[0]
            .performClick()

        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("fab_add_expense", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.waitForExpenseInList("5.000")
    }
}
