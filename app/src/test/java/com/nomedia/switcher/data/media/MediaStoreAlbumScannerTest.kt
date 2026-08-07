package com.nomedia.switcher.data.media

import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MediaStoreAlbumScannerTest {
    private val scanner = MediaStoreAlbumScanner(
        directoryNormalizer = DirectoryNormalizer(),
    )

    @Test
    fun bucket_with_single_directory_isEligible() {
        val rows = listOf(
            row(bucketId = "1", bucketName = "Camera", relativePath = "DCIM/Camera/"),
            row(bucketId = "1", bucketName = "Camera", relativePath = "DCIM/Camera/"),
        )

        assertEquals(
            listOf(
                candidate(
                    bucketId = "1",
                    bucketName = "Camera",
                    directoryKey = "DCIM/Camera",
                    coverUri = coverUri(1L),
                    latestMediaTimestampEpochMs = 1_000L,
                    coverRelativeFilePath = "IMG_0001.jpg",
                    coverDisplayName = "IMG_0001.jpg",
                    coverMediaKind = "image",
                ),
            ),
            scanner.fromRows(rows),
        )
    }

    @Test
    fun fromRows_uses_newest_media_item_as_cover_source() {
        val albums = scanner.fromRows(
            listOf(
                row(
                    mediaId = 200L,
                    bucketId = "1",
                    bucketName = "Edited",
                    relativePath = "Pictures/Edited/",
                    dateModifiedEpochMs = 20_000L,
                ),
                row(
                    mediaId = 100L,
                    bucketId = "1",
                    bucketName = "Edited",
                    relativePath = "Pictures/Edited/",
                    dateModifiedEpochMs = 10_000L,
                ),
            )
        )

        assertEquals(coverUri(200L), albums.single().coverUri)
        assertEquals(20_000L, albums.single().latestMediaTimestampEpochMs)
    }

    @Test
    fun fromRows_uses_row_volume_name_when_building_cover_uri() {
        val albums = scanner.fromRows(
            listOf(
                row(
                    mediaId = 200L,
                    bucketId = "1",
                    bucketName = "Edited",
                    relativePath = "Pictures/Edited/",
                    volumeName = "external_secondary",
                ),
            ),
        )

        assertEquals(
            MediaStore.Images.Media.getContentUri("external_secondary", 200L).toString(),
            albums.single().coverUri,
        )
    }

    @Test
    fun fromRows_captures_cover_reference_fields_for_newest_image_row() {
        val album = scanner.fromRows(
            listOf(
                row(
                    mediaId = 42L,
                    bucketId = "1",
                    bucketName = "Travel",
                    relativePath = "Pictures/Travel/",
                    displayName = "IMG_0042.jpg",
                    albumRelativeFilePath = "IMG_0042.jpg",
                    mediaKind = "image",
                ),
            ),
        ).single()

        assertEquals("IMG_0042.jpg", album.coverDisplayName)
        assertEquals("IMG_0042.jpg", album.coverRelativeFilePath)
        assertEquals("image", album.coverMediaKind)
    }

    @Test
    fun bucket_with_multiple_directories_isRejected() {
        val rows = listOf(
            row(bucketId = "2", bucketName = "Travel", relativePath = "Pictures/TripA/"),
            row(bucketId = "2", bucketName = "Travel", relativePath = "Pictures/TripB/"),
        )

        assertEquals(emptyList<AlbumCandidate>(), scanner.fromRows(rows))
    }

    @Test
    fun bucket_with_case_variant_directory_paths_uses_system_bucket_grouping() {
        val rows = listOf(
            row(
                mediaId = 100L,
                bucketId = "-243170037",
                bucketName = "X",
                relativePath = "Download/X/",
                displayName = "newest.jpg",
                albumRelativeFilePath = "newest.jpg",
                dateModifiedEpochMs = 2_000L,
            ),
            row(
                mediaId = 200L,
                bucketId = "-243170037",
                bucketName = "X",
                relativePath = "download/X/",
                displayName = "older.jpg",
                albumRelativeFilePath = "older.jpg",
            ),
        )

        assertEquals(
            listOf(
                candidate(
                    bucketId = "-243170037",
                    bucketName = "X",
                    directoryKey = "Download/X",
                    coverUri = coverUri(100L),
                    latestMediaTimestampEpochMs = 2_000L,
                    coverRelativeFilePath = "newest.jpg",
                    coverDisplayName = "newest.jpg",
                    coverMediaKind = "image",
                ),
            ),
            scanner.fromRows(rows),
        )
    }

    @Test
    fun row_without_resolvable_directory_isIgnored() {
        assertEquals(
            emptyList<AlbumCandidate>(),
            scanner.fromRows(listOf(row(relativePath = null, dataPath = null))),
        )
    }

    private fun row(
        mediaId: Long = 1L,
        bucketId: String = "1",
        bucketName: String = "Camera",
        relativePath: String? = "DCIM/Camera/",
        dataPath: String? = null,
        volumeName: String = "external_primary",
        displayName: String = "IMG_0001.jpg",
        albumRelativeFilePath: String = "IMG_0001.jpg",
        mediaKind: String = "image",
        dateModifiedEpochMs: Long = 1_000L,
    ): MediaStoreAlbumRow {
        return MediaStoreAlbumRow(
            mediaId = mediaId,
            bucketId = bucketId,
            bucketName = bucketName,
            relativePath = relativePath,
            dataPath = dataPath,
            volumeName = volumeName,
            displayName = displayName,
            albumRelativeFilePath = albumRelativeFilePath,
            mediaKind = mediaKind,
            dateModifiedEpochMs = dateModifiedEpochMs,
        )
    }

    private fun coverUri(
        mediaId: Long,
        volumeName: String = "external_primary",
        mediaKind: String = "image",
    ): String {
        return when (mediaKind) {
            "video" -> MediaStore.Video.Media.getContentUri(volumeName, mediaId).toString()
            else -> MediaStore.Images.Media.getContentUri(volumeName, mediaId).toString()
        }
    }

    private fun candidate(
        bucketId: String,
        bucketName: String,
        directoryKey: String,
        volumeName: String = "external_primary",
        coverUri: String? = null,
        latestMediaTimestampEpochMs: Long? = null,
        coverRelativeFilePath: String? = null,
        coverDisplayName: String? = null,
        coverMediaKind: String? = null,
    ): AlbumCandidate {
        return AlbumCandidate(
            bucketId = bucketId,
            bucketName = bucketName,
            directoryKey = directoryKey,
            volumeName = volumeName,
            coverUri = coverUri,
            latestMediaTimestampEpochMs = latestMediaTimestampEpochMs,
            coverRelativeFilePath = coverRelativeFilePath,
            coverDisplayName = coverDisplayName,
            coverMediaKind = coverMediaKind,
        )
    }
}
