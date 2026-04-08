package com.nomedia.switcher.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nomedia.switcher.data.cover.ResolvedAlbumCover
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.UiMessage
import com.nomedia.switcher.ui.progress.ToggleProgressSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumListUiState(
    val albums: List<AlbumRowState> = emptyList(),
    val pinHiddenAlbumsToTop: Boolean = true,
    val progressSheet: ToggleProgressSheetState? = null,
)

class AlbumListViewModel(
    albums: Flow<List<AlbumEntry>>,
    settings: Flow<com.nomedia.switcher.data.local.settings.UserSettings>,
    private val enqueueToggle: suspend (String, String, ToggleAction) -> Unit,
    private val setPinHiddenAlbums: suspend (Boolean) -> Unit,
    private val resolveFallbackCover: suspend (String, String?, String?) -> ResolvedAlbumCover? = { _, _, _ -> null },
) : ViewModel() {
    private val albumEntries = albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
    private val pendingActions = MutableStateFlow<Map<AlbumId, ToggleAction>>(emptyMap())
    private val progressSheetState = MutableStateFlow<ToggleProgressSheetState?>(null)
    private val fallbackCovers = MutableStateFlow<Map<AlbumId, ResolvedAlbumCover>>(emptyMap())

    init {
        viewModelScope.launch {
            albumEntries.collect { entries ->
                val completedIds = pendingActions.value.keys.filter { id ->
                    val entry = entries.firstOrNull { it.id == id }
                    entry == null || entry.state != AlbumState.Processing
                }
                if (completedIds.isEmpty()) {
                    return@collect
                }
                pendingActions.update { current ->
                    current - completedIds.toSet()
                }
                if (progressSheetState.value?.albumId in completedIds) {
                    progressSheetState.value = null
                }
            }
        }
        viewModelScope.launch {
            albumEntries.collect { entries ->
                val resolvedFallbacks = withContext(Dispatchers.IO) {
                    entries.mapNotNull { entry ->
                        val treeUri = entry.treeUri ?: return@mapNotNull null
                        if (entry.coverUri != null) {
                            return@mapNotNull null
                        }
                        val resolved = resolveFallbackCover(
                            treeUri,
                            entry.coverRelativeFilePath,
                            entry.coverMediaKind,
                        ) ?: return@mapNotNull null
                        entry.id to resolved
                    }.toMap()
                }
                fallbackCovers.value = resolvedFallbacks
            }
        }
    }

    val uiState: StateFlow<AlbumListUiState> = combine(
        albumEntries,
        settings,
        pendingActions,
        progressSheetState,
        fallbackCovers,
    ) { albumEntries, userSettings, pending, sheet, resolvedFallbacks ->
        AlbumListUiState(
            albums = albumEntries.map { entry ->
                entry.toRowState(
                    pendingAction = pending[entry.id],
                    fallbackCover = resolvedFallbacks[entry.id],
                )
            },
            pinHiddenAlbumsToTop = userSettings.pinHiddenAlbumsToTop,
            progressSheet = sheet,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumListUiState(),
    )

    fun onToggleClick(album: AlbumRowState) {
        val action = album.nextAction ?: return
        pendingActions.update { current ->
            current + (album.id to action)
        }
        progressSheetState.value = ToggleProgressSheetState(
            albumId = album.id,
            albumName = album.displayName,
            action = action,
            message = progressMessage(action),
        )
        viewModelScope.launch {
            enqueueToggle(album.id.directoryKey, album.displayName, action)
        }
    }

    fun dismissProgressSheet() {
        progressSheetState.value = null
    }

    fun onPinHiddenChanged(enabled: Boolean) {
        viewModelScope.launch {
            setPinHiddenAlbums(enabled)
        }
    }

    companion object {
        fun factory(
            albums: Flow<List<AlbumEntry>>,
            settings: Flow<com.nomedia.switcher.data.local.settings.UserSettings>,
            enqueueToggle: suspend (String, String, ToggleAction) -> Unit,
            setPinHiddenAlbums: suspend (Boolean) -> Unit,
            resolveFallbackCover: suspend (String, String?, String?) -> ResolvedAlbumCover? = { _, _, _ -> null },
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlbumListViewModel(
                        albums = albums,
                        settings = settings,
                        enqueueToggle = enqueueToggle,
                        setPinHiddenAlbums = setPinHiddenAlbums,
                        resolveFallbackCover = resolveFallbackCover,
                    ) as T
                }
            }
        }
    }
}

private fun AlbumEntry.toRowState(
    pendingAction: ToggleAction?,
    fallbackCover: ResolvedAlbumCover?,
): AlbumRowState {
    val resolvedCoverUri = coverUri ?: fallbackCover?.uri
    val resolvedCoverMediaKind = coverMediaKind ?: fallbackCover?.mediaKind

    if (pendingAction != null) {
        return AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = AlbumState.Processing,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = pendingAction == ToggleAction.Hide,
            isToggleEnabled = false,
            nextAction = null,
            statusMessage = progressMessage(pendingAction),
            showsInlineProgress = true,
        )
    }

    return when (state) {
        AlbumState.Shown -> AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = state,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = false,
            isToggleEnabled = true,
            nextAction = ToggleAction.Hide,
        )
        AlbumState.Hidden -> AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = state,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = true,
            isToggleEnabled = true,
            nextAction = ToggleAction.Show,
            statusMessage = UiMessage.AlbumHidden,
        )
        AlbumState.Processing -> AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = state,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = lastAction != ToggleAction.Show,
            isToggleEnabled = false,
            nextAction = null,
            statusMessage = progressMessage(lastAction ?: ToggleAction.Hide),
            showsInlineProgress = true,
        )
        AlbumState.Failed -> AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = state,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = lastAction == ToggleAction.Show,
            isToggleEnabled = true,
            nextAction = lastAction ?: ToggleAction.Hide,
            statusMessage = lastFailure?.toFailureUiMessage() ?: UiMessage.LastActionFailed,
        )
        AlbumState.HiddenMissingFromScan -> AlbumRowState(
            id = id,
            displayName = displayName,
            directorySummary = id.directoryKey,
            state = state,
            coverUri = resolvedCoverUri,
            coverMediaKind = resolvedCoverMediaKind,
            isChecked = true,
            isToggleEnabled = true,
            nextAction = ToggleAction.Show,
            statusMessage = UiMessage.AlbumHidden,
        )
    }
}

private fun String.toFailureUiMessage(): UiMessage {
    return when (ToggleFailureReason.fromPersistedKey(this)) {
        ToggleFailureReason.RestrictedRoot -> UiMessage.DirectoryCannotBeGranted
        ToggleFailureReason.GrantDenied -> UiMessage.DirectoryAccessNotGranted
        ToggleFailureReason.WrongDirectorySelected -> UiMessage.WrongFolderSelected
        ToggleFailureReason.PersistPermissionDenied -> UiMessage.PersistAccessDenied
        ToggleFailureReason.Interrupted -> UiMessage.PreviousTaskInterrupted
        ToggleFailureReason.MissingDirectoryGrant -> UiMessage.DirectoryGrantMissing
        ToggleFailureReason.UnableToCreateNomedia -> UiMessage.UnableToCreateNomedia
        ToggleFailureReason.UnableToRemoveNomedia -> UiMessage.UnableToRemoveNomedia
        null -> UiMessage.Raw(this)
    }
}

private fun progressMessage(action: ToggleAction): UiMessage = when (action) {
    ToggleAction.Hide -> UiMessage.HideInProgress
    ToggleAction.Show -> UiMessage.ShowInProgress
}
