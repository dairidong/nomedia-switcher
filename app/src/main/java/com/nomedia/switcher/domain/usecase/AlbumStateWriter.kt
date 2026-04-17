package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

interface AlbumStateWriter {
    suspend fun updateAlbum(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
        lastAction: ToggleAction,
        lastFailure: String?,
        treeUri: String?,
    )

    suspend fun updateCachedCover(
        directoryKey: String,
        cachedCoverPath: String,
        cachedCoverMediaKind: String,
        cachedCoverUpdatedAtEpochMs: Long,
    )
}
