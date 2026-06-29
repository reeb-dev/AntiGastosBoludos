package com.antigastos.boludos.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.TestUiHelper
import com.antigastos.boludos.TestUiHelper.confirmExpenseIfNeeded
import com.antigastos.boludos.TestUiHelper.dismissOverlays
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
        composeRule.onAllNodesWithTag("fab_add_expense", useUnmergedTree = true)[0].performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("expense_amount", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onAllNodesWithTag("expense_amount", useUnmergedTree = true)[0]
            .performClick()
            .performTextInput("500")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("expense_save", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onAllNodesWithTag("expense_save", useUnmergedTree = true)[0].performClick()
        composeRule.confirmExpenseIfNeeded()

        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("fab_add_expense", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onAllNodesWithText("Lista", useUnmergedTree = true)[0].performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("500", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
