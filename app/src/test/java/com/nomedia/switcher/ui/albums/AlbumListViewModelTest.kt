package com.nomedia.switcher.ui.albums
import com.nomedia.switcher.data.local.settings.UserSettings
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
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
        assertEquals(AlbumState.Processing, viewModel.uiState.value.albums.single().state)
        assertEquals(
            listOf(Triple("DCIM/Camera", "Camera", ToggleAction.Hide)),
            toggleRequests,
        )
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
                lastFailure = "Missing directory grant",
            ),
        )
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.progressSheet)
        assertEquals(AlbumState.Failed, viewModel.uiState.value.albums.single().state)
        assertEquals(
            "Missing directory grant",
            viewModel.uiState.value.albums.single().statusText,
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
        lastAction: ToggleAction? = null,
        lastFailure: String? = null,
    ): AlbumEntry {
        return AlbumEntry(
            id = AlbumId(directoryKey),
            displayName = displayName,
            state = state,
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
