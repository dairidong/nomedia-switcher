package com.nomedia.switcher.data

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumRepositoryImplTest {
    private val repository = AlbumRepositoryImpl()

    @Test
    fun merge_filters_out_system_reserved_directories_from_local_state() {
        val albums = repository.merge(
            records = listOf(
                AlbumRecordEntity(
                    directoryKey = "Pictures/Screenshots",
                    displayName = "Screenshots",
                    state = AlbumState.Failed,
                    treeUri = "content://example/screenshots",
                    lastAction = ToggleAction.Hide,
                    lastFailure = "Unable to create .nomedia",
                    seenInLastScan = true,
                    updatedAtEpochMs = 1L,
                ),
                AlbumRecordEntity(
                    directoryKey = "Pictures/Vacation",
                    displayName = "Vacation",
                    state = AlbumState.Shown,
                    treeUri = null,
                    lastAction = null,
                    lastFailure = null,
                    seenInLastScan = true,
                    updatedAtEpochMs = 2L,
                ),
            ),
            scan = emptyList(),
            pinHidden = false,
            scanCompleted = true,
        )

        assertEquals(listOf("Pictures/Vacation"), albums.map { it.id.directoryKey })
    }

    @Test
    fun merge_keeps_child_directories_of_reserved_directories_from_local_state() {
        val albums = repository.merge(
            records = listOf(
                AlbumRecordEntity(
                    directoryKey = "Pictures/Screenshots/Edited",
                    displayName = "Edited",
                    state = AlbumState.Shown,
                    treeUri = null,
                    lastAction = null,
                    lastFailure = null,
                    seenInLastScan = true,
                    updatedAtEpochMs = 1L,
                ),
                AlbumRecordEntity(
                    directoryKey = "DCIM/Camera/Burst",
                    displayName = "Burst",
                    state = AlbumState.Hidden,
                    treeUri = "content://example/burst",
                    lastAction = ToggleAction.Hide,
                    lastFailure = null,
                    seenInLastScan = false,
                    updatedAtEpochMs = 2L,
                ),
            ),
            scan = emptyList(),
            pinHidden = false,
            scanCompleted = true,
        )

        assertEquals(
            listOf("DCIM/Camera/Burst", "Pictures/Screenshots/Edited"),
            albums.map { it.id.directoryKey },
        )
    }

    @Test
    fun merge_prefers_cover_uri_from_current_scan_result() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Travel",
                    displayName = "Old Travel",
                    state = AlbumState.Hidden,
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "Pictures/Travel",
                    bucketName = "Travel",
                    coverUri = "content://media/external/images/media/101",
                ),
            ),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(
            "content://media/external/images/media/101",
            albums.single().coverUri,
        )
    }

    @Test
    fun merge_sets_cover_uri_null_for_local_only_album() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Archive",
                    displayName = "Archive",
                    state = AlbumState.Hidden,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = true,
        )

        assertNull(albums.single().coverUri)
    }

    @Test
    fun merge_exposes_persisted_cover_reference_for_local_only_album() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Archive",
                    displayName = "Archive",
                    state = AlbumState.Hidden,
                    coverRelativeFilePath = "Shots/IMG_0042.jpg",
                    coverMediaKind = "image",
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals("Shots/IMG_0042.jpg", albums.single().coverRelativeFilePath)
        assertEquals("image", albums.single().coverMediaKind)
    }

    private fun localAlbum(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
        coverRelativeFilePath: String? = null,
        coverMediaKind: String? = null,
    ): AlbumRecordEntity {
        return AlbumRecordEntity(
            directoryKey = directoryKey,
            displayName = displayName,
            state = state,
            treeUri = "content://tree/${directoryKey.replace('/', '_')}",
            lastAction = ToggleAction.Hide,
            lastFailure = null,
            seenInLastScan = true,
            updatedAtEpochMs = 1L,
            coverRelativeFilePath = coverRelativeFilePath,
            coverMediaKind = coverMediaKind,
        )
    }

    private fun scannedAlbum(
        directoryKey: String,
        bucketName: String,
        coverUri: String? = null,
    ): AlbumCandidate {
        return AlbumCandidate(
            bucketId = directoryKey.hashCode().toString(),
            bucketName = bucketName,
            directoryKey = directoryKey,
            volumeName = "external_primary",
            coverUri = coverUri,
        )
    }
}
