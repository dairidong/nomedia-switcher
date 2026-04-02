package com.nomedia.switcher.data.cover

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.nomedia.switcher.data.toggle.buildChildDocumentId
import com.nomedia.switcher.data.toggle.buildChildDocumentUri
import java.io.FileNotFoundException

data class ResolvedAlbumCover(
    val uri: String,
    val mediaKind: String,
)

data class ResolvedTreeDocument(
    val uri: String,
    val mimeType: String,
)

interface TreeDocumentLookup {
    suspend fun findDocument(
        treeUri: String,
        relativeFilePath: String,
    ): ResolvedTreeDocument?
}

class AlbumCoverFallbackResolver(
    private val documentLookup: TreeDocumentLookup,
) {
    suspend fun resolve(
        treeUri: String,
        coverRelativeFilePath: String?,
        coverMediaKind: String?,
    ): ResolvedAlbumCover? {
        val relativeFilePath = coverRelativeFilePath?.takeIf { it.isNotBlank() } ?: return null
        val mediaKind = coverMediaKind?.takeIf { it.isNotBlank() } ?: return null
        val resolvedDocument = documentLookup.findDocument(treeUri, relativeFilePath) ?: return null
        if (!mimeTypeMatches(mediaKind, resolvedDocument.mimeType)) {
            return null
        }
        return ResolvedAlbumCover(
            uri = resolvedDocument.uri,
            mediaKind = mediaKind,
        )
    }

    private fun mimeTypeMatches(
        mediaKind: String,
        mimeType: String,
    ): Boolean {
        return when (mediaKind) {
            "image" -> mimeType.startsWith("image/")
            "video" -> mimeType.startsWith("video/")
            else -> false
        }
    }
}

class SafTreeDocumentLookup(
    private val context: Context,
) : TreeDocumentLookup {
    override suspend fun findDocument(
        treeUri: String,
        relativeFilePath: String,
    ): ResolvedTreeDocument? {
        val treeDocumentUri = Uri.parse(treeUri)
        val documentUri = buildChildDocumentUri(treeDocumentUri, relativeFilePath)
        val expectedDocumentId = buildChildDocumentId(treeDocumentUri, relativeFilePath)
        return try {
            context.contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                if (documentIdIndex == -1 || mimeTypeIndex == -1) {
                    return@use null
                }
                val actualDocumentId = cursor.getString(documentIdIndex)
                val mimeType = cursor.getString(mimeTypeIndex) ?: return@use null
                if (actualDocumentId != expectedDocumentId || mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return@use null
                }
                ResolvedTreeDocument(
                    uri = documentUri.toString(),
                    mimeType = mimeType,
                )
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
