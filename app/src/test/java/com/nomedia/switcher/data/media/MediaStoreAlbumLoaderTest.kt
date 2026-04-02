package com.nomedia.switcher.data.media

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MediaStoreAlbumLoaderTest {
    @Test
    fun load_returns_non_reserved_albums_from_media_rows() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 1L,
                        bucketId = "1",
                        bucketName = "Vacation",
                        relativePath = "Pictures/Vacation/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "IMG_0001.jpg",
                        albumRelativeFilePath = "IMG_0001.jpg",
                        mediaKind = "image",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 2L,
                        bucketId = "1",
                        bucketName = "Vacation",
                        relativePath = "Pictures/Vacation/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "IMG_0002.jpg",
                        albumRelativeFilePath = "IMG_0002.jpg",
                        mediaKind = "image",
                    ),
                )
            },
        )

        val albums = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        )

        assertEquals(
            listOf(
                AlbumCandidate(
                    bucketId = "1",
                    bucketName = "Vacation",
                    directoryKey = "Pictures/Vacation",
                    volumeName = "external_primary",
                    coverUri = coverUri(1L),
                    coverRelativeFilePath = "IMG_0001.jpg",
                    coverDisplayName = "IMG_0001.jpg",
                    coverMediaKind = "image",
                ),
            ),
            albums,
        )
    }

    @Test
    fun load_returns_empty_list_when_query_rows_are_empty() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _: ContentResolver, _: Array<String> -> emptyList() },
        )

        val albums = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        )

        assertEquals(emptyList<AlbumCandidate>(), albums)
    }

    @Test
    fun load_filters_out_system_reserved_directories() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 1L,
                        bucketId = "1",
                        bucketName = "Screenshots",
                        relativePath = "Pictures/Screenshots/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Screenshot_1.png",
                        albumRelativeFilePath = "Screenshot_1.png",
                        mediaKind = "image",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 2L,
                        bucketId = "2",
                        bucketName = "Vacation",
                        relativePath = "Pictures/Vacation/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "IMG_0002.jpg",
                        albumRelativeFilePath = "IMG_0002.jpg",
                        mediaKind = "image",
                    ),
                )
            },
        )

        val albums = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        )

        assertEquals(
            listOf(
                AlbumCandidate(
                    bucketId = "2",
                    bucketName = "Vacation",
                    directoryKey = "Pictures/Vacation",
                    volumeName = "external_primary",
                    coverUri = coverUri(2L),
                    coverRelativeFilePath = "IMG_0002.jpg",
                    coverDisplayName = "IMG_0002.jpg",
                    coverMediaKind = "image",
                ),
            ),
            albums,
        )
    }

    @Test
    fun load_keeps_child_directories_of_reserved_directories_switchable() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 1L,
                        bucketId = "1",
                        bucketName = "Screenshots",
                        relativePath = "Pictures/Screenshots/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Screenshot_1.png",
                        albumRelativeFilePath = "Screenshot_1.png",
                        mediaKind = "image",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 2L,
                        bucketId = "2",
                        bucketName = "Edited",
                        relativePath = "Pictures/Screenshots/Edited/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Edited_2.png",
                        albumRelativeFilePath = "Edited_2.png",
                        mediaKind = "image",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 3L,
                        bucketId = "3",
                        bucketName = "Burst",
                        relativePath = "DCIM/Camera/Burst/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Burst_3.jpg",
                        albumRelativeFilePath = "Burst_3.jpg",
                        mediaKind = "image",
                    ),
                )
            },
        )

        val albums = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        )

        assertEquals(
            listOf(
                AlbumCandidate(
                    bucketId = "2",
                    bucketName = "Edited",
                    directoryKey = "Pictures/Screenshots/Edited",
                    volumeName = "external_primary",
                    coverUri = coverUri(2L),
                    coverRelativeFilePath = "Edited_2.png",
                    coverDisplayName = "Edited_2.png",
                    coverMediaKind = "image",
                ),
                AlbumCandidate(
                    bucketId = "3",
                    bucketName = "Burst",
                    directoryKey = "DCIM/Camera/Burst",
                    volumeName = "external_primary",
                    coverUri = coverUri(3L),
                    coverRelativeFilePath = "Burst_3.jpg",
                    coverDisplayName = "Burst_3.jpg",
                    coverMediaKind = "image",
                ),
            ),
            albums,
        )
    }

    @Test
    fun load_keeps_cover_uri_for_child_directory_of_reserved_directory() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 1L,
                        bucketId = "1",
                        bucketName = "Screenshots",
                        relativePath = "Pictures/Screenshots/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Screenshot_1.png",
                        albumRelativeFilePath = "Screenshot_1.png",
                        mediaKind = "image",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 2L,
                        bucketId = "2",
                        bucketName = "Edited",
                        relativePath = "Pictures/Screenshots/Edited/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "Edited_2.png",
                        albumRelativeFilePath = "Edited_2.png",
                        mediaKind = "image",
                    ),
                )
            },
        )

        val albums = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        )

        assertEquals(
            coverUri(2L),
            albums.single { it.directoryKey == "Pictures/Screenshots/Edited" }.coverUri,
        )
    }

    @Test
    fun load_keeps_video_album_rows_switchable_and_assigns_video_cover_kind() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 5L,
                        bucketId = "5",
                        bucketName = "Trips",
                        relativePath = "Movies/Trips/",
                        dataPath = null,
                        volumeName = "external_primary",
                        displayName = "clip_5.mp4",
                        albumRelativeFilePath = "clip_5.mp4",
                        mediaKind = "video",
                    ),
                )
            },
        )

        val album = loader.load(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
        ).single { it.directoryKey == "Movies/Trips" }

        assertEquals("video", album.coverMediaKind)
        assertEquals("clip_5.mp4", album.coverRelativeFilePath)
    }

    private fun coverUri(
        mediaId: Long,
        volumeName: String = "external_primary",
    ): String {
        return MediaStore.Images.Media.getContentUri(volumeName, mediaId).toString()
    }
}
