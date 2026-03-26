package com.nomedia.switcher.data

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.repository.AlbumRepository

class AlbumRepositoryImpl : AlbumRepository {
    override fun merge(
        records: List<AlbumRecordEntity>,
        scan: List<AlbumCandidate>,
        pinHidden: Boolean,
        scanCompleted: Boolean,
    ): List<AlbumEntry> {
        val localByDirectory = records.associateBy { it.directoryKey }
        val scannedByDirectory = scan.associateBy { it.directoryKey }

        return (localByDirectory.keys + scannedByDirectory.keys)
            .map { directoryKey ->
                val local = localByDirectory[directoryKey]
                val scanned = scannedByDirectory[directoryKey]
                val seenInScan = scanned != null

                AlbumEntry(
                    id = AlbumId(directoryKey),
                    displayName = scanned?.bucketName?.ifBlank { null }
                        ?: local?.displayName
                        ?: directoryKey.substringAfterLast('/'),
                    state = mergedState(local, seenInScan, scanCompleted),
                    treeUri = local?.treeUri,
                    lastAction = local?.lastAction,
                    lastFailure = local?.lastFailure,
                    seenInLastScan = seenInScan,
                    updatedAtEpochMs = local?.updatedAtEpochMs ?: 0L,
                )
            }
            .sortedWith(
                compareBy<AlbumEntry> { stateRank(it.state, pinHidden) }
                    .thenBy { it.displayName.lowercase() }
                    .thenBy { it.id.directoryKey },
            )
    }

    private fun mergedState(
        local: AlbumRecordEntity?,
        seenInScan: Boolean,
        scanCompleted: Boolean,
    ): AlbumState {
        if (local == null) {
            return AlbumState.Shown
        }

        return when {
            seenInScan && local.state == AlbumState.HiddenMissingFromScan -> AlbumState.Hidden
            scanCompleted && !seenInScan && local.state == AlbumState.Hidden -> AlbumState.HiddenMissingFromScan
            scanCompleted && !seenInScan && local.state == AlbumState.HiddenMissingFromScan -> AlbumState.HiddenMissingFromScan
            else -> local.state
        }
    }

    private fun stateRank(state: AlbumState, pinHidden: Boolean): Int = when (state) {
        AlbumState.Processing -> 0
        AlbumState.Hidden, AlbumState.HiddenMissingFromScan -> if (pinHidden) 1 else 3
        AlbumState.Failed -> 2
        AlbumState.Shown -> 3
    }
}
