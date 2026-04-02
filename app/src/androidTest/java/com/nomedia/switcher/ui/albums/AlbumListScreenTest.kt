package com.nomedia.switcher.ui.albums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Rule
import org.junit.Test

class AlbumListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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
                                statusText = "Hidden, not currently in media library",
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
            .onNodeWithText("Hidden, not currently in media library")
            .assertIsDisplayed()
    }

    @Test
    fun highlighted_album_shows_notification_badge() {
        composeTestRule.setContent {
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

        composeTestRule.onNodeWithText("Opened from notification").assertIsDisplayed()
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
                                statusText = "Hidden, not currently in media library",
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
}
