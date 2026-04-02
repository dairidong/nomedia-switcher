package com.nomedia.switcher.data.local.album

import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomAlbumStateWriterTest {
    @Test
    fun updateAlbum_preserves_existing_cover_reference_fields() = runTest {
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
        val writer = RoomAlbumStateWriter(
            albumRecordDao = dao,
            currentTimeProvider = { 9L },
        )

        writer.updateAlbum(
            directoryKey = "Pictures/Travel",
            displayName = "Travel",
            state = AlbumState.Processing,
            lastAction = ToggleAction.Show,
            lastFailure = "retrying",
            treeUri = null,
        )

        assertEquals(
            AlbumRecordEntity(
                directoryKey = "Pictures/Travel",
                displayName = "Travel",
                state = AlbumState.Processing,
                treeUri = "content://tree/travel",
                lastAction = ToggleAction.Show,
                lastFailure = "retrying",
                seenInLastScan = true,
                updatedAtEpochMs = 9L,
                coverRelativeFilePath = "IMG_0042.jpg",
                coverDisplayName = "IMG_0042.jpg",
                coverMediaKind = "image",
                coverUpdatedAtEpochMs = 2L,
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
