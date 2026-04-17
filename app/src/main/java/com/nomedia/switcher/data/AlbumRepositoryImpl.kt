package com.nomedia.switcher.data
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.data.media.SystemReservedDirectoryPolicy
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.shouldPersistAlbumFailureState
import com.nomedia.switcher.domain.repository.AlbumRepository
import java.io.File

class AlbumRepositoryImpl(
    private val reservedDirectoryPolicy: SystemReservedDirectoryPolicy = SystemReservedDirectoryPolicy(),
) : AlbumRepository {
    override fun merge(
        records: List<AlbumRecordEntity>,
        scan: List<AlbumCandidate>,
        pinHidden: Boolean,
        sortMode: AlbumSortMode,
        scanCompleted: Boolean,
    ): List<AlbumEntry> {
        val localByDirectory = records.associateBy { it.directoryKey }
        val scannedByDirectory = scan.associateBy { it.directoryKey }

        return (localByDirectory.keys + scannedByDirectory.keys)
            .filter { directoryKey ->
                reservedDirectoryPolicy.isSwitchable(directoryKey)
            }
            .map { directoryKey ->
                val local = localByDirectory[directoryKey]
                val scanned = scannedByDirectory[directoryKey]
                val seenInScan = scanned != null
                val mergedState = mergedState(local, seenInScan, scanCompleted)
                val cachedCoverUri = local?.cachedCoverPath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { File(it).toURI().toString() }
                val shouldPreferCachedShownVideoCover =
                    mergedState == AlbumState.Shown &&
                        cachedCoverUri != null &&
                        scanned?.coverMediaKind == "video"
                val preferredCoverUri = when (mergedState) {
                    AlbumState.Hidden,
                    AlbumState.HiddenMissingFromScan,
                    -> cachedCoverUri ?: scanned?.coverUri
                    else -> if (shouldPreferCachedShownVideoCover) {
                        cachedCoverUri
                    } else {
                        scanned?.coverUri ?: cachedCoverUri
                    }
                }
                val preferredCoverMediaKind = when (mergedState) {
                    AlbumState.Hidden,
                    AlbumState.HiddenMissingFromScan,
                    -> local?.cachedCoverMediaKind ?: scanned?.coverMediaKind ?: local?.coverMediaKind
                    else -> if (shouldPreferCachedShownVideoCover) {
                        local?.cachedCoverMediaKind ?: scanned?.coverMediaKind ?: local?.coverMediaKind
                    } else {
                        scanned?.coverMediaKind ?: local?.cachedCoverMediaKind ?: local?.coverMediaKind
                    }
                }

                AlbumEntry(
                    id = AlbumId(directoryKey),
                    displayName = scanned?.bucketName?.ifBlank { null }
                        ?: local?.displayName
                        ?: directoryKey.substringAfterLast('/'),
                    state = mergedState,
                    treeUri = local?.treeUri,
                    coverUri = preferredCoverUri,
                    coverRelativeFilePath = local?.coverRelativeFilePath,
                    coverMediaKind = preferredCoverMediaKind,
                    lastAction = local?.lastAction,
                    lastFailure = local?.lastFailure,
                    seenInLastScan = seenInScan,
                    updatedAtEpochMs = local?.updatedAtEpochMs ?: 0L,
                    latestMediaTimestampEpochMs = scanned?.latestMediaTimestampEpochMs
                        ?: local?.latestMediaTimestampEpochMs,
                )
            }
            .sortedWith(albumComparator(pinHidden = pinHidden, sortMode = sortMode))
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
            local.state == AlbumState.Failed -> normalizeFailureState(local)
            else -> local.state
        }
    }

    private fun normalizeFailureState(local: AlbumRecordEntity): AlbumState {
        val reason = local.lastFailure?.let(ToggleFailureReason::fromPersistedKey)
        if (reason != null && !reason.shouldPersistAlbumFailureState()) {
            return when (local.lastAction) {
                ToggleAction.Show -> AlbumState.Hidden
                ToggleAction.Hide, null -> AlbumState.Shown
            }
        }
        return local.state
    }

    private fun stateRank(state: AlbumState, pinHidden: Boolean): Int = when (state) {
        AlbumState.Processing -> 0
        AlbumState.Hidden, AlbumState.HiddenMissingFromScan -> if (pinHidden) 1 else 3
        AlbumState.Failed -> 2
        AlbumState.Shown -> 3
    }

    private fun albumComparator(
        pinHidden: Boolean,
        sortMode: AlbumSortMode,
    ): Comparator<AlbumEntry> {
        val stableComparator = compareBy<AlbumEntry> { it.displayName.lowercase() }
            .thenBy { it.id.directoryKey }

        val sortComparator = when (sortMode) {
            AlbumSortMode.ByLatestMedia -> compareByDescending<AlbumEntry> {
                it.latestMediaTimestampEpochMs ?: Long.MIN_VALUE
            }.then(stableComparator)
            AlbumSortMode.ByName -> stableComparator
        }

        return compareBy<AlbumEntry> { stateRank(it.state, pinHidden) }
            .then(sortComparator)
    }
}
