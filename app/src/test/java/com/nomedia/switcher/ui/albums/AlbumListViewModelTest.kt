package com.nomedia.switcher.ui.albums

import com.nomedia.switcher.data.local.settings.UserSettings
import com.nomedia.switcher.data.cover.ResolvedAlbumCover
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun toggle_click_sets_processing_and_opens_sheet() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "DCIM/Camera",
                    displayName = "Camera",
                    state = AlbumState.Shown,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val toggleRequests = mutableListOf<Triple<String, String, ToggleAction>>()
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { directoryKey, albumName, action ->
                toggleRequests += Triple(directoryKey, albumName, action)
            },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()
        val row = viewModel.uiState.value.albums.single()

        viewModel.onToggleClick(row)
        advanceUntilIdle()

        assertEquals("Camera", viewModel.uiState.value.progressSheet?.albumName)
        assertEquals(UiMessage.HideInProgress, viewModel.uiState.value.progressSheet?.message)
        assertEquals(AlbumState.Processing, viewModel.uiState.value.albums.single().state)
        assertEquals(UiMessage.HideInProgress, viewModel.uiState.value.albums.single().statusMessage)
        assertTrue(viewModel.uiState.value.albums.single().showsInlineProgress)
        assertEquals(
            listOf(Triple("DCIM/Camera", "Camera", ToggleAction.Hide)),
            toggleRequests,
        )
    }

    @Test
    fun row_state_exposes_cover_uri_from_album_entry() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Travel",
                    displayName = "Travel",
                    state = AlbumState.Hidden,
                    coverUri = "content://media/external/images/media/303",
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(
            "content://media/external/images/media/303",
            viewModel.uiState.value.albums.single().coverUri,
        )
        assertFalse(viewModel.uiState.value.albums.single().showsInlineProgress)
    }

    @Test
    fun hidden_album_row_uses_hidden_message_key() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Travel",
                    displayName = "Travel",
                    state = AlbumState.Hidden,
                    coverUri = "content://media/external/images/media/303",
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(UiMessage.AlbumHidden, viewModel.uiState.value.albums.single().statusMessage)
    }

    @Test
    fun uiState_uses_fallback_cover_when_live_scan_cover_is_missing() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Movies/Trips",
                    displayName = "Trips",
                    state = AlbumState.HiddenMissingFromScan,
                    coverUri = null,
                    coverRelativeFilePath = "Clips/VID_0007.mp4",
                    coverMediaKind = "video",
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
            resolveFallbackCover = { treeUri, relativeFilePath, mediaKind ->
                if (treeUri == null || relativeFilePath == null || mediaKind == null) {
                    null
                } else {
                    ResolvedAlbumCover(
                        uri = "content://documents/trips/video",
                        mediaKind = mediaKind,
                    )
                }
            },
        )

        advanceUntilIdle()

        val row = viewModel.uiState.value.albums.single()
        assertEquals("content://documents/trips/video", row.coverUri)
        assertEquals("video", row.coverMediaKind)
    }

    @Test
    fun completed_toggle_clears_pending_processing_state() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Cyberpunk 2077",
                    displayName = "Cyberpunk 2077",
                    state = AlbumState.Shown,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()
        val row = viewModel.uiState.value.albums.single()

        viewModel.onToggleClick(row)
        advanceUntilIdle()
        albums.value = listOf(
            album(
                directoryKey = "Pictures/Cyberpunk 2077",
                displayName = "Cyberpunk 2077",
                state = AlbumState.Failed,
                lastAction = ToggleAction.Hide,
                lastFailure = ToggleFailureReason.MissingDirectoryGrant.persistedKey,
            ),
        )
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.progressSheet)
        assertEquals(AlbumState.Failed, viewModel.uiState.value.albums.single().state)
        assertEquals(
            UiMessage.DirectoryGrantMissing,
            viewModel.uiState.value.albums.single().statusMessage,
        )
    }

    @Test
    fun failed_album_without_failure_shows_fallback_message() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Cyberpunk 2077",
                    displayName = "Cyberpunk 2077",
                    state = AlbumState.Failed,
                    lastAction = ToggleAction.Hide,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(UiMessage.LastActionFailed, viewModel.uiState.value.albums.single().statusMessage)
    }

    @Test
    fun failed_album_with_known_failure_key_uses_localized_message_key() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Travel",
                    displayName = "Travel",
                    state = AlbumState.Failed,
                    lastAction = ToggleAction.Hide,
                    lastFailure = ToggleFailureReason.RestrictedRoot.persistedKey,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(
            UiMessage.DirectoryCannotBeGranted,
            viewModel.uiState.value.albums.single().statusMessage,
        )
    }

    @Test
    fun failed_album_with_nomedia_create_failure_key_uses_localized_message_key() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Travel",
                    displayName = "Travel",
                    state = AlbumState.Failed,
                    lastAction = ToggleAction.Hide,
                    lastFailure = ToggleFailureReason.UnableToCreateNomedia.persistedKey,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(
            UiMessage.UnableToCreateNomedia,
            viewModel.uiState.value.albums.single().statusMessage,
        )
    }

    @Test
    fun failed_album_with_nomedia_remove_failure_key_uses_localized_message_key() = runTest {
        val albums = MutableStateFlow(
            listOf(
                album(
                    directoryKey = "Pictures/Travel",
                    displayName = "Travel",
                    state = AlbumState.Failed,
                    lastAction = ToggleAction.Show,
                    lastFailure = ToggleFailureReason.UnableToRemoveNomedia.persistedKey,
                ),
            ),
        )
        val settings = MutableStateFlow(UserSettings())
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = {},
        )

        advanceUntilIdle()

        assertEquals(
            UiMessage.UnableToRemoveNomedia,
            viewModel.uiState.value.albums.single().statusMessage,
        )
    }

    @Test
    fun settings_toggle_updates_sorting_preference() = runTest {
        val albums = MutableStateFlow(emptyList<AlbumEntry>())
        val settings = MutableStateFlow(UserSettings())
        val updates = mutableListOf<Boolean>()
        val viewModel = AlbumListViewModel(
            albums = albums,
            settings = settings,
            enqueueToggle = { _, _, _ -> },
            setPinHiddenAlbums = { enabled ->
                updates += enabled
                settings.value = settings.value.copy(pinHiddenAlbumsToTop = enabled)
            },
        )

        viewModel.onPinHiddenChanged(false)
        advanceUntilIdle()

        assertEquals(listOf(false), updates)
        assertEquals(false, viewModel.uiState.value.pinHiddenAlbumsToTop)
    }

    private fun album(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
        coverUri: String? = null,
        coverRelativeFilePath: String? = null,
        coverMediaKind: String? = null,
        lastAction: ToggleAction? = null,
        lastFailure: String? = null,
    ): AlbumEntry {
        return AlbumEntry(
            id = AlbumId(directoryKey),
            displayName = displayName,
            state = state,
            coverUri = coverUri,
            coverRelativeFilePath = coverRelativeFilePath,
            coverMediaKind = coverMediaKind,
            treeUri = "content://tree/${directoryKey.replace('/', '_')}",
            lastAction = lastAction,
            lastFailure = lastFailure,
            seenInLastScan = true,
            updatedAtEpochMs = 1L,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
