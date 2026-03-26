package com.nomedia.switcher.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

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
            listOf(candidate(bucketId = "1", bucketName = "Camera", directoryKey = "DCIM/Camera")),
            scanner.fromRows(rows),
        )
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
    fun row_without_resolvable_directory_isIgnored() {
        assertEquals(
            emptyList<AlbumCandidate>(),
            scanner.fromRows(listOf(row(relativePath = null, dataPath = null))),
        )
    }

    private fun row(
        bucketId: String = "1",
        bucketName: String = "Camera",
        relativePath: String? = "DCIM/Camera/",
        dataPath: String? = null,
        volumeName: String = "external_primary",
    ): MediaStoreAlbumRow {
        return MediaStoreAlbumRow(
            mediaId = 1L,
            bucketId = bucketId,
            bucketName = bucketName,
            relativePath = relativePath,
            dataPath = dataPath,
            volumeName = volumeName,
        )
    }

    private fun candidate(
        bucketId: String,
        bucketName: String,
        directoryKey: String,
        volumeName: String = "external_primary",
    ): AlbumCandidate {
        return AlbumCandidate(
            bucketId = bucketId,
            bucketName = bucketName,
            directoryKey = directoryKey,
            volumeName = volumeName,
        )
    }
}
