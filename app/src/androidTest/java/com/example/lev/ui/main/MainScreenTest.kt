package com.example.lev.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lev.ui.screens.MainScreen
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomNav_showsDashboardTab() {
        composeTestRule.setContent { MainScreen() }
        composeTestRule.onNodeWithText("Início").assertIsDisplayed()
    }
}
