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
                coverRelativeFilePath = null,
                coverDisplayName = null,
                coverMediaKind = null,
                coverUpdatedAtEpochMs = null,
            ),
            dao.lastUpsert,
        )
    }

    private class FakeAlbumRecordDao(
        existing: AlbumRecordEntity?,
    ) : AlbumRecordDao {
        private val records = existing?.let { mutableMapOf(it.directoryKey to it) } ?: mutableMapOf()
        var lastUpsert: AlbumRecordEntity? = null

        override fun observeAll(): Flow<List<AlbumRecordEntity>> = flowOf(records.values.toList())

        override suspend fun findByDirectoryKey(directoryKey: String): AlbumRecordEntity? = records[directoryKey]

        override suspend fun findByState(state: AlbumState): List<AlbumRecordEntity> {
            return records.values.filter { it.state == state }
        }

        override suspend fun upsert(record: AlbumRecordEntity) {
            records[record.directoryKey] = record
            lastUpsert = record
        }
    }
}
