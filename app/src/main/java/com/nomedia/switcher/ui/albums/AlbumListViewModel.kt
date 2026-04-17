package com.nomedia.switcher.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nomedia.switcher.data.cover.ResolvedAlbumCover
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.shouldPersistAlbumFailureState
import com.nomedia.switcher.ui.UiMessage
import com.nomedia.switcher.ui.toUiMessage
import com.nomedia.switcher.ui.progress.ToggleProgressSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumListUiState(
    val albums: List<AlbumRowState> = emptyList(),
    val pinHiddenAlbumsToTop: Boolean = true,
    val albumSortMode: AlbumSortMode = AlbumSortMode.ByName,
    val progressSheet: ToggleProgressSheetState? = null,
)

class AlbumListViewModel(
    albums: Flow<List<AlbumEntry>>,
    settings: Flow<com.nomedia.switcher.data.local.settings.UserSettings>,
    private val enqueueToggle: suspend (String, String, ToggleAction, String?, String?) -> Unit,
    private val setPinHiddenAlbums: suspend (Boolean) -> Unit,
    private val setAlbumSortMode: suspend (AlbumSortMode) -> Unit = {},
    private val resolveFallbackCover: suspend (String, String, String?, String?) -> ResolvedAlbumCover? = { _, _, _, _ -> null },
) : ViewModel() {
    private val albumEntries = albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
    private val pendingActions = MutableStateFlow<Map<AlbumId, ToggleAction>>(emptyMap())
    private val progressSheetState = MutableStateFlow<ToggleProgressSheetState?>(null)
    private val fallbackCovers = MutableStateFlow<Map<AlbumId, ResolvedAlbumCover>>(emptyMap())
    private val fallbackCoverRefs = MutableStateFlow<Map<AlbumId, FallbackCoverRef>>(emptyMap())
    private val transientMessageFlow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    private var rowStateCache: Map<AlbumId, AlbumRowState> = emptyMap()
    val transientMessages: SharedFlow<UiMessage> = transientMessageFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            albumEntries.collect { entries ->
                val completedEntries = pendingActions.value.keys.mapNotNull { id ->
                    val entry = entries.firstOrNull { it.id == id }
                    if (entry == null || entry.state != AlbumState.Processing) {
                        id to entry
                    } else {
                        null
                    }
                }
                val completedIds = completedEntries.map { it.first }
                if (completedIds.isEmpty()) {
                    return@collect
                }
                pendingActions.update { current ->
                    current - completedIds.toSet()
                }
                completedEntries.forEach { (_, entry) ->
                    if (entry?.state == AlbumState.Failed) {
                        transientMessageFlow.tryEmit(
                            entry.lastFailure?.toFailureUiMessage() ?: UiMessage.LastActionFailed,
                        )
                    }
                }
                if (progressSheetState.value?.albumId in completedIds) {
                    progressSheetState.value = null
                }
            }
        }
        viewModelScope.launch {
            albumEntries.collect { entries ->
                val cachedCovers = fallbackCovers.value
                val cachedRefs = fallbackCoverRefs.value
                val resolvedFallbacks = withContext(Dispatchers.IO) {
                    entries.mapNotNull { entry ->
                        val fallbackRef = entry.fallbackCoverRef() ?: return@mapNotNull null
                        val cachedCover = cachedCovers[entry.id]
                        if (cachedRefs[entry.id] == fallbackRef && cachedCover != null) {
                            return@mapNotNull entry.id to cachedCover
                        }
                        resolveFallbackCover(
                            entry.id.directoryKey,
                            fallbackRef.treeUri,
                            fallbackRef.relativeFilePath,
                            fallbackRef.mediaKind,
                        )?.let { entry.id to it }
                    }.toMap()
                }
                fallbackCovers.value = resolvedFallbacks
                fallbackCoverRefs.value = entries.mapNotNull { entry ->
                    entry.fallbackCoverRef()?.let { entry.id to it }
                }.toMap()
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
        val reusedRows = linkedMapOf<AlbumId, AlbumRowState>()
        val rows = albumEntries.map { entry ->
            val nextRow = entry.toRowState(
                pendingAction = pending[entry.id],
                fallbackCover = resolvedFallbacks[entry.id],
            )
            val reusedRow = rowStateCache[entry.id]?.takeIf { it == nextRow } ?: nextRow
            reusedRows[entry.id] = reusedRow
            reusedRow
        }
        rowStateCache = reusedRows
        AlbumListUiState(
            albums = rows,
            pinHiddenAlbumsToTop = userSettings.pinHiddenAlbumsToTop,
            albumSortMode = userSettings.albumSortMode,
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
            enqueueToggle(
                album.id.directoryKey,
                album.displayName,
                action,
                album.coverUri,
                album.coverMediaKind,
            )
        }
    }

    fun dismissProgressSheet() {
        progressSheetState.value = null
    }

    fun onForegroundFailure(message: UiMessage) {
        transientMessageFlow.tryEmit(message)
    }

    fun onPinHiddenChanged(enabled: Boolean) {
        viewModelScope.launch {
            setPinHiddenAlbums(enabled)
        }
    }

    fun onAlbumSortModeChanged(mode: AlbumSortMode) {
        viewModelScope.launch {
            setAlbumSortMode(mode)
        }
    }

    companion object {
        fun factory(
            albums: Flow<List<AlbumEntry>>,
            settings: Flow<com.nomedia.switcher.data.local.settings.UserSettings>,
            enqueueToggle: suspend (String, String, ToggleAction, String?, String?) -> Unit,
            setPinHiddenAlbums: suspend (Boolean) -> Unit,
            setAlbumSortMode: suspend (AlbumSortMode) -> Unit,
            resolveFallbackCover: suspend (String, String, String?, String?) -> ResolvedAlbumCover? = { _, _, _, _ -> null },
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlbumListViewModel(
                        albums = albums,
                        settings = settings,
                        enqueueToggle = enqueueToggle,
                        setPinHiddenAlbums = setPinHiddenAlbums,
                        setAlbumSortMode = setAlbumSortMode,
                        resolveFallbackCover = resolveFallbackCover,
                    ) as T
                }
            }
        }
    }
}

private data class FallbackCoverRef(
    val treeUri: String,
    val relativeFilePath: String,
    val mediaKind: String,
)

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
        AlbumState.Failed -> toFailedRowState(
            resolvedCoverUri = resolvedCoverUri,
            resolvedCoverMediaKind = resolvedCoverMediaKind,
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

private fun AlbumEntry.toFailedRowState(
    resolvedCoverUri: String?,
    resolvedCoverMediaKind: String?,
): AlbumRowState {
    val failureReason = lastFailure?.let(ToggleFailureReason::fromPersistedKey)
    if (failureReason != null && !failureReason.shouldPersistAlbumFailureState()) {
        return when (lastAction) {
            ToggleAction.Show -> AlbumRowState(
                id = id,
                displayName = displayName,
                directorySummary = id.directoryKey,
                state = AlbumState.Hidden,
                coverUri = resolvedCoverUri,
                coverMediaKind = resolvedCoverMediaKind,
                isChecked = true,
                isToggleEnabled = true,
                nextAction = ToggleAction.Show,
                statusMessage = UiMessage.AlbumHidden,
            )

            ToggleAction.Hide, null -> AlbumRowState(
                id = id,
                displayName = displayName,
                directorySummary = id.directoryKey,
                state = AlbumState.Shown,
                coverUri = resolvedCoverUri,
                coverMediaKind = resolvedCoverMediaKind,
                isChecked = false,
                isToggleEnabled = true,
                nextAction = ToggleAction.Hide,
            )
        }
    }

    return AlbumRowState(
        id = id,
        displayName = displayName,
        directorySummary = id.directoryKey,
        state = AlbumState.Failed,
        coverUri = resolvedCoverUri,
        coverMediaKind = resolvedCoverMediaKind,
        isChecked = lastAction == ToggleAction.Show,
        isToggleEnabled = true,
        nextAction = lastAction ?: ToggleAction.Hide,
        statusMessage = lastFailure?.toFailureUiMessage() ?: UiMessage.LastActionFailed,
    )
}

private fun AlbumEntry.fallbackCoverRef(): FallbackCoverRef? {
    if (coverUri != null) {
        return null
    }
    val treeUri = treeUri ?: return null
    val relativeFilePath = coverRelativeFilePath?.takeIf { it.isNotBlank() } ?: return null
    val mediaKind = coverMediaKind?.takeIf { it.isNotBlank() } ?: return null
    return FallbackCoverRef(
        treeUri = treeUri,
        relativeFilePath = relativeFilePath,
        mediaKind = mediaKind,
    )
}

private fun String.toFailureUiMessage(): UiMessage {
    return when (ToggleFailureReason.fromPersistedKey(this)) {
        ToggleFailureReason.RestrictedRoot -> ToggleFailureReason.RestrictedRoot.toUiMessage()
        ToggleFailureReason.GrantDenied -> ToggleFailureReason.GrantDenied.toUiMessage()
        ToggleFailureReason.WrongDirectorySelected -> ToggleFailureReason.WrongDirectorySelected.toUiMessage()
        ToggleFailureReason.PersistPermissionDenied -> ToggleFailureReason.PersistPermissionDenied.toUiMessage()
        ToggleFailureReason.Interrupted -> ToggleFailureReason.Interrupted.toUiMessage()
        ToggleFailureReason.MissingDirectoryGrant -> ToggleFailureReason.MissingDirectoryGrant.toUiMessage()
        ToggleFailureReason.UnableToCreateNomedia -> ToggleFailureReason.UnableToCreateNomedia.toUiMessage()
        ToggleFailureReason.UnableToRemoveNomedia -> ToggleFailureReason.UnableToRemoveNomedia.toUiMessage()
        null -> UiMessage.Raw(this)
    }
}

private fun progressMessage(action: ToggleAction): UiMessage = when (action) {
    ToggleAction.Hide -> UiMessage.HideInProgress
    ToggleAction.Show -> UiMessage.ShowInProgress
}
