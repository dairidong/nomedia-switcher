package com.nomedia.switcher.data.media

import android.content.ContentResolver
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
    fun load_returns_switchable_albums_from_media_rows() = runTest {
        val loader = MediaStoreAlbumLoader(
            scanner = MediaStoreAlbumScanner(),
            queryRows = { _, _ ->
                listOf(
                    MediaStoreAlbumRow(
                        mediaId = 1L,
                        bucketId = "1",
                        bucketName = "Camera",
                        relativePath = "DCIM/Camera/",
                        dataPath = null,
                        volumeName = "external_primary",
                    ),
                    MediaStoreAlbumRow(
                        mediaId = 2L,
                        bucketId = "1",
                        bucketName = "Camera",
                        relativePath = "DCIM/Camera/",
                        dataPath = null,
                        volumeName = "external_primary",
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
                    bucketName = "Camera",
                    directoryKey = "DCIM/Camera",
                    volumeName = "external_primary",
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
}
