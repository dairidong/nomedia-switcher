package com.nomedia.switcher.data.toggle

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.worker.NomediaToggleExecutor
import java.io.FileNotFoundException

interface NomediaDirectoryAccess {
    suspend fun exists(treeUri: String, fileName: String): Boolean
    suspend fun createFile(treeUri: String, fileName: String): Boolean
    suspend fun deleteFile(treeUri: String, fileName: String): Boolean
}

class NomediaDocumentGateway(
    private val directoryAccess: NomediaDirectoryAccess,
) : NomediaToggleExecutor {
    override suspend fun hide(
        treeUri: String,
        directoryKey: String,
    ): ToggleResult {
        if (directoryAccess.exists(treeUri, NOMEDIA_FILE)) {
            return ToggleResult.Success
        }

        val created = directoryAccess.createFile(treeUri, NOMEDIA_FILE)
        return if (created && directoryAccess.exists(treeUri, NOMEDIA_FILE)) {
            ToggleResult.Success
        } else {
            ToggleResult.PermanentFailure("Unable to create .nomedia for $directoryKey")
        }
    }

    override suspend fun show(
        treeUri: String,
        directoryKey: String,
    ): ToggleResult {
        return if (directoryAccess.deleteFile(treeUri, NOMEDIA_FILE)) {
            ToggleResult.Success
        } else {
            ToggleResult.PermanentFailure("Unable to remove .nomedia for $directoryKey")
        }
    }

    companion object {
        private const val NOMEDIA_FILE = ".nomedia"
    }
}

class SafNomediaDirectoryAccess(
    private val context: Context,
) : NomediaDirectoryAccess {
    override suspend fun exists(
        treeUri: String,
        fileName: String,
    ): Boolean {
        val treeDocumentUri = Uri.parse(treeUri)
        val childDocumentId = buildChildDocumentId(treeDocumentUri, fileName)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeDocumentUri, childDocumentId)
        return try {
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use false
                }
                val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                if (documentIdIndex == -1) {
                    return@use false
                }
                cursor.getString(documentIdIndex) == childDocumentId
            } == true
        } catch (_: FileNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    override suspend fun createFile(
        treeUri: String,
        fileName: String,
    ): Boolean {
        val directory = resolveTree(treeUri) ?: return false
        val existing = directory.findFile(fileName)
        if (existing != null) {
            return true
        }

        val created = directory.createFile("application/octet-stream", fileName)
        if (created == null) {
            return false
        }
        return true
    }

    override suspend fun deleteFile(
        treeUri: String,
        fileName: String,
    ): Boolean {
        val treeDocumentUri = Uri.parse(treeUri)
        val documentUri = buildChildDocumentUri(treeDocumentUri, fileName)
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, documentUri)
        } catch (_: FileNotFoundException) {
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            true
        }
    }

    private fun resolveTree(treeUri: String): DocumentFile? {
        return DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
    }
}

internal fun buildChildDocumentUri(
    treeUri: Uri,
    relativePath: String,
): Uri {
    return DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        buildChildDocumentId(treeUri, relativePath),
    )
}

internal fun buildChildDocumentId(
    treeUri: Uri,
    relativePath: String,
): String {
    val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val normalizedRelativePath = relativePath.trim('/')
    return if (normalizedRelativePath.isEmpty()) {
        treeDocumentId
    } else {
        "$treeDocumentId/$normalizedRelativePath"
    }
}
