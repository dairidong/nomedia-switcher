package com.nomedia.switcher.data.local.album

import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.usecase.AlbumStateWriter

class RoomAlbumStateWriter(
    private val albumRecordDao: AlbumRecordDao,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
) : AlbumStateWriter {
    override suspend fun updateAlbum(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
        lastAction: ToggleAction,
        lastFailure: String?,
        treeUri: String?,
    ) {
        val existing = albumRecordDao.findByDirectoryKey(directoryKey)
        albumRecordDao.upsert(
            AlbumRecordEntity(
                directoryKey = directoryKey,
                displayName = displayName.ifBlank {
                    existing?.displayName ?: directoryKey.substringAfterLast('/')
                },
                state = state,
                treeUri = treeUri ?: existing?.treeUri,
                lastAction = lastAction,
                lastFailure = lastFailure,
                seenInLastScan = existing?.seenInLastScan ?: true,
                updatedAtEpochMs = currentTimeProvider(),
                coverRelativeFilePath = existing?.coverRelativeFilePath,
                coverDisplayName = existing?.coverDisplayName,
                coverMediaKind = existing?.coverMediaKind,
                coverUpdatedAtEpochMs = existing?.coverUpdatedAtEpochMs,
                cachedCoverPath = existing?.cachedCoverPath,
                cachedCoverMediaKind = existing?.cachedCoverMediaKind,
                cachedCoverUpdatedAtEpochMs = existing?.cachedCoverUpdatedAtEpochMs,
            ),
        )
    }

    override suspend fun updateCachedCover(
        directoryKey: String,
        cachedCoverPath: String,
        cachedCoverMediaKind: String,
        cachedCoverUpdatedAtEpochMs: Long,
    ) {
        val existing = albumRecordDao.findByDirectoryKey(directoryKey) ?: return
        albumRecordDao.upsert(
            existing.copy(
                updatedAtEpochMs = currentTimeProvider(),
                cachedCoverPath = cachedCoverPath,
                cachedCoverMediaKind = cachedCoverMediaKind,
                cachedCoverUpdatedAtEpochMs = cachedCoverUpdatedAtEpochMs,
            ),
        )
    }
}
