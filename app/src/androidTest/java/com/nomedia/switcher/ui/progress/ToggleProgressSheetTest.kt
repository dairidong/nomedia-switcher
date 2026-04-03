package com.nomedia.switcher.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.nomedia.switcher.R
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test

class ToggleProgressSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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

    @Test
    fun progress_sheet_uses_resource_text_for_helper_copy() {
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
            .onNodeWithText(context.getString(R.string.toggle_progress_helper))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.toggle_progress_hide_panel))
            .assertIsDisplayed()
    }
}
