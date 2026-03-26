package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ObserveAlbumsUseCaseTest {
    private val useCase = ObserveAlbumsUseCase()

    @Test
    fun local_hidden_album_is_visible_before_scan_returns() = runTest {
        val records = listOf(
            localAlbum(
                directoryKey = "Pictures/Secret",
                displayName = "Secret",
                state = AlbumState.HiddenMissingFromScan,
            ),
        )

        val merged = useCase.merge(records = records, scan = emptyList(), pinHidden = true)

        assertEquals("Secret", merged.first().displayName)
        assertEquals(AlbumState.HiddenMissingFromScan, merged.first().state)
    }

    @Test
    fun processing_album_is_sorted_first() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "DCIM/Camera",
                    displayName = "Camera",
                    state = AlbumState.Processing,
                ),
                localAlbum(
                    directoryKey = "Pictures/Shown",
                    displayName = "Shown",
                    state = AlbumState.Shown,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
        )

        assertEquals(AlbumState.Processing, merged.first().state)
    }

    @Test
    fun hidden_albums_do_not_jump_when_pin_setting_disabled() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Zeta",
                    displayName = "Zeta Hidden",
                    state = AlbumState.Hidden,
                ),
                localAlbum(
                    directoryKey = "Pictures/Alpha",
                    displayName = "Alpha Shown",
                    state = AlbumState.Shown,
                ),
            ),
            scan = emptyList(),
            pinHidden = false,
        )

        assertNotEquals(AlbumState.Hidden, merged.first().state)
        assertEquals(listOf("Alpha Shown", "Zeta Hidden"), merged.map { it.displayName })
    }

    @Test
    fun scanned_album_merges_with_local_state_by_directory_key() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "DCIM/Camera",
                    displayName = "Old Camera",
                    state = AlbumState.Hidden,
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "DCIM/Camera",
                    bucketName = "Camera",
                ),
            ),
            pinHidden = true,
        )

        assertEquals(
            listOf(
                Triple(AlbumId("DCIM/Camera"), "Camera", AlbumState.Hidden),
            ),
            merged.map { Triple(it.id, it.displayName, it.state) },
        )
    }

    @Test
    fun hidden_album_stays_hidden_before_scan_has_results() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Secret",
                    displayName = "Secret",
                    state = AlbumState.Hidden,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = false,
        )

        assertEquals(AlbumState.Hidden, merged.single().state)
    }

    @Test
    fun hidden_missing_album_returns_to_hidden_when_scan_finds_it_again() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Secret",
                    displayName = "Secret",
                    state = AlbumState.HiddenMissingFromScan,
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "Pictures/Secret",
                    bucketName = "Secret",
                ),
            ),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(AlbumState.Hidden, merged.single().state)
    }

    @Test
    fun hidden_album_becomes_missing_after_completed_scan_omits_it() = runTest {
        val merged = useCase.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Secret",
                    displayName = "Secret",
                    state = AlbumState.Hidden,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(AlbumState.HiddenMissingFromScan, merged.single().state)
    }

    private fun localAlbum(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
    ): AlbumRecordEntity {
        return AlbumRecordEntity(
            directoryKey = directoryKey,
            displayName = displayName,
            state = state,
            treeUri = "content://tree/${directoryKey.replace('/', '_')}",
            lastAction = ToggleAction.Hide,
            lastFailure = null,
            seenInLastScan = state != AlbumState.HiddenMissingFromScan,
            updatedAtEpochMs = 1L,
        )
    }

    private fun scannedAlbum(
        directoryKey: String,
        bucketName: String,
    ): AlbumCandidate {
        return AlbumCandidate(
            bucketId = directoryKey.hashCode().toString(),
            bucketName = bucketName,
            directoryKey = directoryKey,
            volumeName = "external_primary",
        )
    }
}
