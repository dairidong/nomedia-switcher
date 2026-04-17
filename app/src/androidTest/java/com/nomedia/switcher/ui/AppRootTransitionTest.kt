package com.nomedia.switcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test

class AppRootTransitionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animated_root_screen_keeps_both_screens_in_tree_during_transition() {
        var showSettings by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            NoMediaTheme {
                AnimatedRootScreen(
                    showSettings = showSettings,
                    albumListContent = {
                        androidx.compose.material3.Text(text = "HOME")
                    },
                    settingsContent = {
                        androidx.compose.material3.Text(text = "SETTINGS")
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("HOME").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            showSettings = true
        }
        composeTestRule.mainClock.advanceTimeBy(100L)

        composeTestRule.onAllNodesWithText("HOME").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("SETTINGS").assertCountEquals(1)
    }
}
