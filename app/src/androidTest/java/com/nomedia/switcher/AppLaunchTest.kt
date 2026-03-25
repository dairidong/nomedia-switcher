package com.nomedia.switcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsAlbumScreenTitle() {
        composeTestRule.onNodeWithText("Albums").assertIsDisplayed()
    }
}
