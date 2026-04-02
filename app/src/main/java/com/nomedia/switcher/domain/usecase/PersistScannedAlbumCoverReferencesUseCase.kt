package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordDao
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumState

class PersistScannedAlbumCoverReferencesUseCase(
    private val albumRecordDao: AlbumRecordDao,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun persist(scan: List<AlbumCandidate>) {
        scan.forEach { album ->
            val now = currentTimeProvider()
            val existing = albumRecordDao.findByDirectoryKey(album.directoryKey)
            val hasCompleteCoverReference =
                album.coverRelativeFilePath != null && album.coverMediaKind != null
            albumRecordDao.upsert(
                AlbumRecordEntity(
                    directoryKey = album.directoryKey,
                    displayName = album.bucketName.ifBlank {
                        existing?.displayName ?: album.directoryKey.substringAfterLast('/')
                    },
                    state = existing?.state ?: AlbumState.Shown,
                    treeUri = existing?.treeUri,
                    lastAction = existing?.lastAction,
                    lastFailure = existing?.lastFailure,
                    seenInLastScan = true,
                    updatedAtEpochMs = existing?.updatedAtEpochMs ?: now,
                    coverRelativeFilePath = album.coverRelativeFilePath.takeIf { hasCompleteCoverReference },
                    coverDisplayName = album.coverDisplayName.takeIf { hasCompleteCoverReference },
                    coverMediaKind = album.coverMediaKind.takeIf { hasCompleteCoverReference },
                    coverUpdatedAtEpochMs = now.takeIf { hasCompleteCoverReference },
                ),
            )
        }
    }
}
