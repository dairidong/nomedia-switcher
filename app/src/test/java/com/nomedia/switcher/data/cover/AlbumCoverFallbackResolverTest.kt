package com.nomedia.switcher.data.cover

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumCoverFallbackResolverTest {
    @Test
    fun resolve_returns_document_uri_for_nested_relative_file_path() = runTest {
        val resolver = AlbumCoverFallbackResolver(
            documentLookup = FakeTreeDocumentLookup(
                document = ResolvedTreeDocument(
                    uri = "content://com.android.externalstorage.documents/tree/primary%3APictures%2FTravel/document/primary%3APictures%2FTravel%2FShots%2FIMG_0042.jpg",
                    mimeType = "image/jpeg",
                ),
            ),
        )

        val resolved = resolver.resolve(
            directoryKey = "Pictures/Travel",
            treeUri = "content://com.android.externalstorage.documents/tree/primary%3APictures%2FTravel",
            coverRelativeFilePath = "Shots/IMG_0042.jpg",
            coverMediaKind = "image",
        )

        assertEquals(
            ResolvedAlbumCover(
                uri = "content://com.android.externalstorage.documents/tree/primary%3APictures%2FTravel/document/primary%3APictures%2FTravel%2FShots%2FIMG_0042.jpg",
                mediaKind = "image",
            ),
            resolved,
        )
    }

    @Test
    fun resolve_returns_null_when_document_mime_type_does_not_match_cover_kind() = runTest {
        val resolver = AlbumCoverFallbackResolver(
            documentLookup = FakeTreeDocumentLookup(
                document = ResolvedTreeDocument(
                    uri = "content://documents/travel/video.mp4",
                    mimeType = "video/mp4",
                ),
            ),
        )

        val resolved = resolver.resolve(
            directoryKey = "Pictures/Travel",
            treeUri = "content://documents/tree/travel",
            coverRelativeFilePath = "video.mp4",
            coverMediaKind = "image",
        )

        assertNull(resolved)
    }

    @Test
    fun resolve_returns_cached_image_cover_for_video_document_when_cache_store_succeeds() = runTest {
        val cacheStore = FakeAlbumCoverCacheStore(
            cached = CachedAlbumCoverRef(
                absolutePath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-trips.webp",
                mediaKind = "image",
                updatedAtEpochMs = 123L,
            ),
        )
        val resolver = AlbumCoverFallbackResolver(
            documentLookup = FakeTreeDocumentLookup(
                document = ResolvedTreeDocument(
                    uri = "content://documents/trips/video.mp4",
                    mimeType = "video/mp4",
                ),
            ),
            coverCacheStore = cacheStore,
        )

        val resolved = resolver.resolve(
            directoryKey = "Movies/Trips",
            treeUri = "content://documents/tree/trips",
            coverRelativeFilePath = "Clips/video.mp4",
            coverMediaKind = "video",
        )

        assertEquals(
            ResolvedAlbumCover(
                uri = File("/data/user/0/com.nomedia.switcher/files/album-cover-cache/movies-trips.webp").toURI().toString(),
                mediaKind = "image",
            ),
            resolved,
        )
        assertEquals(
            listOf(Triple("Movies/Trips", "content://documents/trips/video.mp4", "video")),
            cacheStore.requests,
        )
    }

    private class FakeTreeDocumentLookup(
        private val document: ResolvedTreeDocument?,
    ) : TreeDocumentLookup {
        override suspend fun findDocument(
            treeUri: String,
            relativeFilePath: String,
        ): ResolvedTreeDocument? = document
    }

    private class FakeAlbumCoverCacheStore(
        private val cached: CachedAlbumCoverRef?,
    ) : AlbumCoverCacheStore() {
        val requests = mutableListOf<Triple<String, String, String>>()

        override suspend fun createOrUpdate(
            directoryKey: String,
            sourceUri: String,
            sourceMediaKind: String,
        ): CachedAlbumCoverRef? {
            requests += Triple(directoryKey, sourceUri, sourceMediaKind)
            return cached
        }
    }
}
