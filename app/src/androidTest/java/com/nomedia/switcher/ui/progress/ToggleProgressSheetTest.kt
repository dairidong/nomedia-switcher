package com.nomedia.switcher.ui.progress

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.test.platform.app.InstrumentationRegistry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

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
            WithZhCnLocale {
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
        }

        composeTestRule
            .onNodeWithText("大图集可能需要一些时间。你可以隐藏此面板，让任务继续。")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("隐藏面板")
            .assertIsDisplayed()
    }

    @Composable
    private fun WithZhCnLocale(content: @Composable () -> Unit) {
        val zhConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("zh-CN"))
        }
        val zhContext = context.createConfigurationContext(zhConfiguration)
        CompositionLocalProvider(
            LocalContext provides zhContext,
            LocalConfiguration provides zhConfiguration,
        ) {
            content()
        }
    }
}
