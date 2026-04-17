package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordDao
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistScannedAlbumCoverReferencesUseCaseTest {
    @Test
    fun persist_refreshes_cover_reference_without_overwriting_existing_state_fields() = runTest {
        val dao = FakeAlbumRecordDao(
            existing = AlbumRecordEntity(
                directoryKey = "Pictures/Travel",
                displayName = "Travel",
                state = AlbumState.Hidden,
                treeUri = "content://tree/travel",
                lastAction = ToggleAction.Hide,
                lastFailure = "old failure",
                seenInLastScan = true,
                updatedAtEpochMs = 1L,
                latestMediaTimestampEpochMs = 900L,
            ),
        )
        val useCase = PersistScannedAlbumCoverReferencesUseCase(
            albumRecordDao = dao,
            currentTimeProvider = { 1L },
        )

        useCase.persist(
            listOf(
                AlbumCandidate(
                    bucketId = "1",
                    bucketName = "Travel",
                    directoryKey = "Pictures/Travel",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/42",
                    latestMediaTimestampEpochMs = 4_200L,
                    coverRelativeFilePath = "IMG_0042.jpg",
                    coverDisplayName = "IMG_0042.jpg",
                    coverMediaKind = "image",
                ),
            ),
        )

        assertEquals(
            AlbumRecordEntity(
                directoryKey = "Pictures/Travel",
                displayName = "Travel",
                state = AlbumState.Hidden,
                treeUri = "content://tree/travel",
                lastAction = ToggleAction.Hide,
                lastFailure = "old failure",
                seenInLastScan = true,
                updatedAtEpochMs = 1L,
                latestMediaTimestampEpochMs = 4_200L,
                coverRelativeFilePath = "IMG_0042.jpg",
                coverDisplayName = "IMG_0042.jpg",
                coverMediaKind = "image",
                coverUpdatedAtEpochMs = 1L,
            ),
            dao.lastUpsert,
        )
    }

    @Test
    fun persist_creates_shown_record_for_new_album_with_cover_reference() = runTest {
        val dao = FakeAlbumRecordDao(existing = null)
        val useCase = PersistScannedAlbumCoverReferencesUseCase(
            albumRecordDao = dao,
            currentTimeProvider = { 5L },
        )

        useCase.persist(
            listOf(
                AlbumCandidate(
                    bucketId = "2",
                    bucketName = "Family",
                    directoryKey = "Pictures/Family",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/9",
                    latestMediaTimestampEpochMs = 900L,
                    coverRelativeFilePath = "IMG_0009.jpg",
                    coverDisplayName = "IMG_0009.jpg",
                    coverMediaKind = "image",
                ),
            ),
        )

        assertEquals(
            AlbumRecordEntity(
                directoryKey = "Pictures/Family",
                displayName = "Family",
                state = AlbumState.Shown,
                treeUri = null,
                lastAction = null,
                lastFailure = null,
                seenInLastScan = true,
                updatedAtEpochMs = 5L,
                latestMediaTimestampEpochMs = 900L,
                coverRelativeFilePath = "IMG_0009.jpg",
                coverDisplayName = "IMG_0009.jpg",
                coverMediaKind = "image",
                coverUpdatedAtEpochMs = 5L,
            ),
            dao.lastUpsert,
        )
    }

    @Test
    fun persist_clears_stale_cover_reference_when_latest_scan_has_no_persistable_cover() = runTest {
        val dao = FakeAlbumRecordDao(
            existing = AlbumRecordEntity(
                directoryKey = "Pictures/Travel",
                displayName = "Travel",
                state = AlbumState.Hidden,
                treeUri = "content://tree/travel",
                lastAction = ToggleAction.Hide,
                lastFailure = null,
                seenInLastScan = true,
                updatedAtEpochMs = 1L,
                latestMediaTimestampEpochMs = 4_200L,
                coverRelativeFilePath = "IMG_0042.jpg",
                coverDisplayName = "IMG_0042.jpg",
                coverMediaKind = "image",
                coverUpdatedAtEpochMs = 2L,
            ),
        )
        val useCase = PersistScannedAlbumCoverReferencesUseCase(
            albumRecordDao = dao,
            currentTimeProvider = { 7L },
        )

        useCase.persist(
            listOf(
                AlbumCandidate(
                    bucketId = "1",
                    bucketName = "Travel",
                    directoryKey = "Pictures/Travel",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/42",
                    latestMediaTimestampEpochMs = 7_700L,
                    coverRelativeFilePath = null,
                    coverDisplayName = null,
                    coverMediaKind = null,
                ),
            ),
        )

        assertEquals(
            AlbumRecordEntity(
                directoryKey = "Pictures/Travel",
                displayName = "Travel",
                state = AlbumState.Hidden,
                treeUri = "content://tree/travel",
                lastAction = ToggleAction.Hide,
                lastFailure = null,
                seenInLastScan = true,
                updatedAtEpochMs = 1L,
                latestMediaTimestampEpochMs = 7_700L,
                coverRelativeFilePath = null,
                coverDisplayName = null,
                coverMediaKind = null,
                coverUpdatedAtEpochMs = null,
            ),
            dao.lastUpsert,
        )
    }

    @Test
    fun persist_batches_multiple_album_updates_into_single_upsert_all() = runTest {
        val dao = FakeAlbumRecordDao(existing = null)
        val useCase = PersistScannedAlbumCoverReferencesUseCase(
            albumRecordDao = dao,
            currentTimeProvider = { 9L },
        )

        useCase.persist(
            listOf(
                AlbumCandidate(
                    bucketId = "1",
                    bucketName = "Travel",
                    directoryKey = "Pictures/Travel",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/42",
                    latestMediaTimestampEpochMs = 420L,
                    coverRelativeFilePath = "IMG_0042.jpg",
                    coverDisplayName = "IMG_0042.jpg",
                    coverMediaKind = "image",
                ),
                AlbumCandidate(
                    bucketId = "2",
                    bucketName = "Family",
                    directoryKey = "Pictures/Family",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/9",
                    latestMediaTimestampEpochMs = 900L,
                    coverRelativeFilePath = "IMG_0009.jpg",
                    coverDisplayName = "IMG_0009.jpg",
                    coverMediaKind = "image",
                ),
            ),
        )

        assertEquals(1, dao.upsertAllCalls)
        assertEquals(2, dao.lastUpsertAll.size)
    }

    @Test
    fun persist_doesNotOverwrite_lastKnownTimestamp_for_album_missing_from_current_scan() = runTest {
        val missingRecord = AlbumRecordEntity(
            directoryKey = "Pictures/Archive",
            displayName = "Archive",
            state = AlbumState.HiddenMissingFromScan,
            treeUri = "content://tree/archive",
            lastAction = ToggleAction.Hide,
            lastFailure = null,
            seenInLastScan = false,
            updatedAtEpochMs = 3L,
            latestMediaTimestampEpochMs = 8_800L,
        )
        val dao = FakeAlbumRecordDao(existing = missingRecord)
        val useCase = PersistScannedAlbumCoverReferencesUseCase(
            albumRecordDao = dao,
            currentTimeProvider = { 9L },
        )

        useCase.persist(
            listOf(
                AlbumCandidate(
                    bucketId = "1",
                    bucketName = "Travel",
                    directoryKey = "Pictures/Travel",
                    volumeName = "external_primary",
                    coverUri = "content://media/external_primary/images/media/42",
                    latestMediaTimestampEpochMs = 420L,
                    coverRelativeFilePath = "IMG_0042.jpg",
                    coverDisplayName = "IMG_0042.jpg",
                    coverMediaKind = "image",
                ),
            ),
        )

        assertEquals(8_800L, dao.findByDirectoryKey("Pictures/Archive")?.latestMediaTimestampEpochMs)
    }

    private class FakeAlbumRecordDao(
        existing: AlbumRecordEntity?,
    ) : AlbumRecordDao {
        private val records = existing?.let { mutableMapOf(it.directoryKey to it) } ?: mutableMapOf()
        var lastUpsert: AlbumRecordEntity? = null
        var lastUpsertAll: List<AlbumRecordEntity> = emptyList()
        var upsertAllCalls: Int = 0

        override fun observeAll(): Flow<List<AlbumRecordEntity>> = flowOf(records.values.toList())

        override suspend fun findByDirectoryKey(directoryKey: String): AlbumRecordEntity? = records[directoryKey]

        override suspend fun findByState(state: AlbumState): List<AlbumRecordEntity> {
            return records.values.filter { it.state == state }
        }

        override suspend fun upsert(record: AlbumRecordEntity) {
            records[record.directoryKey] = record
            lastUpsert = record
        }

        override suspend fun findByDirectoryKeys(directoryKeys: List<String>): List<AlbumRecordEntity> {
            return directoryKeys.mapNotNull(records::get)
        }

        override suspend fun upsertAll(records: List<AlbumRecordEntity>) {
            records.forEach { record ->
                this.records[record.directoryKey] = record
            }
            lastUpsertAll = records
            upsertAllCalls += 1
            lastUpsert = records.lastOrNull()
        }
    }
}
