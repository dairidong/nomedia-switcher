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

        return if (directoryAccess.createFile(treeUri, NOMEDIA_FILE)) {
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
        val documentUri = buildChildDocumentUri(treeDocumentUri, fileName)
        return try {
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { cursor ->
                cursor.moveToFirst()
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
        return directory.findFile(fileName) != null || directory.createFile("application/octet-stream", fileName) != null
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

    private fun buildChildDocumentUri(
        treeUri: Uri,
        fileName: String,
    ): Uri {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        return DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            "$treeDocumentId/$fileName",
        )
    }
}
