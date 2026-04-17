package com.nomedia.switcher.data

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleAction
import java.io.File
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

    @Test
    fun merge_uses_cached_cover_file_when_live_cover_is_missing() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Movies/Archive",
                    displayName = "Archive",
                    state = AlbumState.HiddenMissingFromScan,
                    cachedCoverPath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-archive.webp",
                    cachedCoverMediaKind = "image",
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(
            File("/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-archive.webp").toURI().toString(),
            albums.single().coverUri,
        )
        assertEquals("image", albums.single().coverMediaKind)
    }

    @Test
    fun merge_prefers_cached_cover_for_hidden_album_even_when_scan_still_has_live_cover() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Movies/Archive",
                    displayName = "Archive",
                    state = AlbumState.Hidden,
                    cachedCoverPath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-archive.webp",
                    cachedCoverMediaKind = "image",
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "Movies/Archive",
                    bucketName = "Archive",
                    coverUri = "content://media/external/video/media/77",
                ),
            ),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(
            File("/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-archive.webp").toURI().toString(),
            albums.single().coverUri,
        )
    }

    @Test
    fun merge_prefers_live_cover_for_shown_image_album_when_scan_has_latest_cover() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Public",
                    displayName = "Public",
                    state = AlbumState.Shown,
                    cachedCoverPath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/pictures-public.webp",
                    cachedCoverMediaKind = "image",
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "Pictures/Public",
                    bucketName = "Public",
                    coverUri = "content://media/external/images/media/88",
                    coverMediaKind = "image",
                ),
            ),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals("content://media/external/images/media/88", albums.single().coverUri)
    }

    @Test
    fun merge_prefers_cached_cover_for_shown_video_album_when_cached_thumbnail_exists() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Movies/Public",
                    displayName = "Public",
                    state = AlbumState.Shown,
                    cachedCoverPath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-public.webp",
                    cachedCoverMediaKind = "image",
                ),
            ),
            scan = listOf(
                scannedAlbum(
                    directoryKey = "Movies/Public",
                    bucketName = "Public",
                    coverUri = "content://media/external/video/media/88",
                    coverMediaKind = "video",
                ),
            ),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(
            File("/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-public.webp").toURI().toString(),
            albums.single().coverUri,
        )
        assertEquals("image", albums.single().coverMediaKind)
    }

    @Test
    fun merge_normalizes_non_persistent_failures_back_to_regular_visibility_state() {
        val albums = repository.merge(
            records = listOf(
                AlbumRecordEntity(
                    directoryKey = "Pictures/AuthFailure",
                    displayName = "AuthFailure",
                    state = AlbumState.Failed,
                    treeUri = null,
                    lastAction = ToggleAction.Hide,
                    lastFailure = ToggleFailureReason.GrantDenied.persistedKey,
                    seenInLastScan = true,
                    updatedAtEpochMs = 1L,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            scanCompleted = true,
        )

        assertEquals(AlbumState.Shown, albums.single().state)
    }

    @Test
    fun merge_keeps_hidden_group_ahead_of_shown_group_when_pin_enabled_even_for_time_sort() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/OldHidden",
                    displayName = "Old Hidden",
                    state = AlbumState.Hidden,
                    latestMediaTimestampEpochMs = 10L,
                ),
                localAlbum(
                    directoryKey = "Pictures/NewShown",
                    displayName = "New Shown",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = 1_000L,
                ),
            ),
            scan = emptyList(),
            pinHidden = true,
            sortMode = AlbumSortMode.ByLatestMedia,
            scanCompleted = true,
        )

        assertEquals(listOf("Old Hidden", "New Shown"), albums.map { it.displayName })
    }

    @Test
    fun merge_sorts_by_latest_media_time_descending_within_same_group() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Old",
                    displayName = "Old",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = 10L,
                ),
                localAlbum(
                    directoryKey = "Pictures/New",
                    displayName = "New",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = 1_000L,
                ),
            ),
            scan = emptyList(),
            pinHidden = false,
            sortMode = AlbumSortMode.ByLatestMedia,
            scanCompleted = true,
        )

        assertEquals(listOf("New", "Old"), albums.map { it.displayName })
    }

    @Test
    fun merge_falls_back_to_name_then_directory_when_latest_media_time_missing() {
        val albums = repository.merge(
            records = listOf(
                localAlbum(
                    directoryKey = "Pictures/Gamma",
                    displayName = "Same",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = null,
                ),
                localAlbum(
                    directoryKey = "Pictures/Alpha",
                    displayName = "Same",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = null,
                ),
                localAlbum(
                    directoryKey = "Pictures/Beta",
                    displayName = "Another",
                    state = AlbumState.Shown,
                    latestMediaTimestampEpochMs = null,
                ),
            ),
            scan = emptyList(),
            pinHidden = false,
            sortMode = AlbumSortMode.ByLatestMedia,
            scanCompleted = true,
        )

        assertEquals(
            listOf("Pictures/Beta", "Pictures/Alpha", "Pictures/Gamma"),
            albums.map { it.id.directoryKey },
        )
    }

    private fun localAlbum(
        directoryKey: String,
        displayName: String,
        state: AlbumState,
        latestMediaTimestampEpochMs: Long? = null,
        coverRelativeFilePath: String? = null,
        coverMediaKind: String? = null,
        cachedCoverPath: String? = null,
        cachedCoverMediaKind: String? = null,
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
            latestMediaTimestampEpochMs = latestMediaTimestampEpochMs,
            coverRelativeFilePath = coverRelativeFilePath,
            coverMediaKind = coverMediaKind,
            cachedCoverPath = cachedCoverPath,
            cachedCoverMediaKind = cachedCoverMediaKind,
        )
    }

    private fun scannedAlbum(
        directoryKey: String,
        bucketName: String,
        coverUri: String? = null,
        coverMediaKind: String? = null,
    ): AlbumCandidate {
        return AlbumCandidate(
            bucketId = directoryKey.hashCode().toString(),
            bucketName = bucketName,
            directoryKey = directoryKey,
            volumeName = "external_primary",
            coverUri = coverUri,
            coverMediaKind = coverMediaKind,
        )
    }
}
