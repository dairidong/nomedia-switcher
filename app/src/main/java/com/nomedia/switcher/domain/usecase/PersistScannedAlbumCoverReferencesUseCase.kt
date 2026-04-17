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
        if (scan.isEmpty()) {
            return
        }
        val now = currentTimeProvider()
        val existingByDirectory = albumRecordDao.findByDirectoryKeys(
            scan.map { it.directoryKey }.distinct(),
        ).associateBy { it.directoryKey }
        val records = scan.map { album ->
            val existing = existingByDirectory[album.directoryKey]
            val hasCompleteCoverReference =
                album.coverRelativeFilePath != null && album.coverMediaKind != null
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
                latestMediaTimestampEpochMs = album.latestMediaTimestampEpochMs
                    ?: existing?.latestMediaTimestampEpochMs,
                coverRelativeFilePath = album.coverRelativeFilePath.takeIf { hasCompleteCoverReference },
                coverDisplayName = album.coverDisplayName.takeIf { hasCompleteCoverReference },
                coverMediaKind = album.coverMediaKind.takeIf { hasCompleteCoverReference },
                coverUpdatedAtEpochMs = now.takeIf { hasCompleteCoverReference },
                cachedCoverPath = existing?.cachedCoverPath,
                cachedCoverMediaKind = existing?.cachedCoverMediaKind,
                cachedCoverUpdatedAtEpochMs = existing?.cachedCoverUpdatedAtEpochMs,
            )
        }
        albumRecordDao.upsertAll(records)
    }
}
