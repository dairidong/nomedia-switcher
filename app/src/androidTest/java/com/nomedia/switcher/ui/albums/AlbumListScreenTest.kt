package com.nomedia.switcher.ui.albums

import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.test.platform.app.InstrumentationRegistry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
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
                                statusText = "Hidden",
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
                                    statusText = "Hidden",
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
    fun album_list_uses_resource_text_for_static_labels() {
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
            .onNodeWithText("图集")
            .assertIsDisplayed()
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
                                statusText = "Hiding from media library",
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
    fun album_with_video_cover_renders_cover_node_instead_of_placeholder() {
        composeTestRule.setContent {
            NoMediaTheme {
                AlbumListScreen(
                    state = AlbumListUiState(
                        albums = listOf(
                            AlbumRowState(
                                id = AlbumId("Movies/Trips"),
                                displayName = "Trips",
                                directorySummary = "Movies/Trips",
                                state = AlbumState.HiddenMissingFromScan,
                                coverUri = "content://documents/trips/video",
                                coverMediaKind = "video",
                                isChecked = true,
                                isToggleEnabled = true,
                                nextAction = ToggleAction.Show,
                                statusText = "Hidden",
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
            .onNodeWithTag("album-cover-image-Movies/Trips")
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
