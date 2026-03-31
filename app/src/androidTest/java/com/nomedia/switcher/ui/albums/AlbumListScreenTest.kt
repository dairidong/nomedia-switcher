package com.nomedia.switcher.ui.albums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
}
