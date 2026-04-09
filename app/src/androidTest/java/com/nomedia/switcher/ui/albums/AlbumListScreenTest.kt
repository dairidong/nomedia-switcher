package com.nomedia.switcher.ui.albums

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.UiMessage
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class AlbumListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun hidden_missing_album_shows_recoverable_status() {
        composeTestRule.setContent {
            NoMediaTheme {
                AlbumListScreen(
                    state = AlbumListUiState(
                        albums = listOf(
                            AlbumRowState(
                                id = AlbumId("Pictures/Secret"),
                                displayName = "Secret",
                                directorySummary = "Pictures/Secret",
                                state = AlbumState.HiddenMissingFromScan,
                                isChecked = true,
                                isToggleEnabled = true,
                                nextAction = ToggleAction.Show,
                                statusMessage = UiMessage.AlbumHidden,
                            ),
                        ),
                    ),
                    onToggleClick = {},
                    onOpenSettings = {},
                    highlightedAlbumId = null,
                )
            }
        }

        composeTestRule
            .onNodeWithText("Hidden")
            .assertIsDisplayed()
    }

    @Test
    fun highlighted_album_shows_notification_badge() {
        composeTestRule.setContent {
            WithZhCnLocale {
                NoMediaTheme {
                    AlbumListScreen(
                        state = AlbumListUiState(
                            albums = listOf(
                                AlbumRowState(
                                    id = AlbumId("Pictures/Secret"),
                                    displayName = "Secret",
                                    directorySummary = "Pictures/Secret",
                                    state = AlbumState.Hidden,
                                    isChecked = true,
                                    isToggleEnabled = true,
                                    nextAction = ToggleAction.Show,
                                    statusMessage = UiMessage.AlbumHidden,
                                ),
                            ),
                        ),
                        onToggleClick = {},
                        onOpenSettings = {},
                        highlightedAlbumId = AlbumId("Pictures/Secret"),
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("从通知中打开")
            .assertIsDisplayed()
    }

    @Test
    fun album_list_hides_title_text_and_keeps_settings_entry() {
        composeTestRule.setContent {
            WithZhCnLocale {
                NoMediaTheme {
                    AlbumListScreen(
                        state = AlbumListUiState(),
                        onToggleClick = {},
                        onOpenSettings = {},
                        highlightedAlbumId = null,
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText("图集")
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText("设置")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("暂无可切换的图集")
            .assertIsDisplayed()
    }

    @Test
    fun processing_album_shows_spinner_next_to_switch() {
        composeTestRule.setContent {
            NoMediaTheme {
                AlbumListScreen(
                    state = AlbumListUiState(
                        albums = listOf(
                            AlbumRowState(
                                id = AlbumId("Pictures/Edited"),
                                displayName = "Edited",
                                directorySummary = "Pictures/Edited",
                                state = AlbumState.Processing,
                                isChecked = true,
                                isToggleEnabled = false,
                                nextAction = null,
                                statusMessage = UiMessage.HideInProgress,
                                showsInlineProgress = true,
                            ),
                        ),
                    ),
                    onToggleClick = {},
                    onOpenSettings = {},
                    highlightedAlbumId = null,
                )
            }
        }

        composeTestRule
            .onNodeWithTag("album-inline-progress-Pictures/Edited")
            .assertIsDisplayed()
    }

    @Test
    fun album_without_cover_shows_placeholder() {
        composeTestRule.setContent {
            NoMediaTheme {
                AlbumListScreen(
                    state = AlbumListUiState(
                        albums = listOf(
                            AlbumRowState(
                                id = AlbumId("Pictures/Edited"),
                                displayName = "Edited",
                                directorySummary = "Pictures/Edited",
                                state = AlbumState.Shown,
                                coverUri = null,
                                isChecked = false,
                                isToggleEnabled = true,
                                nextAction = ToggleAction.Hide,
                            ),
                        ),
                    ),
                    onToggleClick = {},
                    onOpenSettings = {},
                    highlightedAlbumId = null,
                )
            }
        }

        composeTestRule
            .onNodeWithTag("album-cover-placeholder-Pictures/Edited")
            .assertIsDisplayed()
    }

    @Test
    fun album_with_broken_video_cover_falls_back_to_placeholder() {
        composeTestRule.setContent {
            NoMediaTheme {
                AlbumListScreen(
                    state = AlbumListUiState(
                        albums = listOf(
                            AlbumRowState(
                                id = AlbumId("Movies/Broken"),
                                displayName = "Broken",
                                directorySummary = "Movies/Broken",
                                state = AlbumState.Shown,
                                coverUri = "content://media/external/video/media/does_not_exist",
                                coverMediaKind = "video",
                                isChecked = false,
                                isToggleEnabled = true,
                                nextAction = ToggleAction.Hide,
                            ),
                        ),
                    ),
                    onToggleClick = {},
                    onOpenSettings = {},
                    highlightedAlbumId = null,
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("album-cover-placeholder-Movies/Broken")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag("album-cover-placeholder-Movies/Broken")
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
