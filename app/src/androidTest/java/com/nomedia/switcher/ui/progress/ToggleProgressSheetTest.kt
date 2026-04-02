package com.nomedia.switcher.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test

class ToggleProgressSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sheet_shows_loading_indicator_while_task_is_running() {
        composeTestRule.setContent {
            NoMediaTheme {
                ToggleProgressSheet(
                    state = ToggleProgressSheetState(
                        albumId = AlbumId("Pictures/Edited"),
                        albumName = "Edited",
                        action = ToggleAction.Hide,
                        message = "Hiding from media library",
                    ),
                    onHide = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("toggle-progress-loading")
            .assertIsDisplayed()
    }
}
