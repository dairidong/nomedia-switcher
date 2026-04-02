package com.nomedia.switcher.data.cover

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
            treeUri = "content://documents/tree/travel",
            coverRelativeFilePath = "video.mp4",
            coverMediaKind = "image",
        )

        assertNull(resolved)
    }

    private class FakeTreeDocumentLookup(
        private val document: ResolvedTreeDocument?,
    ) : TreeDocumentLookup {
        override suspend fun findDocument(
            treeUri: String,
            relativeFilePath: String,
        ): ResolvedTreeDocument? = document
    }
}
