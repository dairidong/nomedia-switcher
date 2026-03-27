package com.nomedia.switcher.data.toggle

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.worker.NomediaToggleExecutor

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
        if (!directoryAccess.exists(treeUri, NOMEDIA_FILE)) {
            return ToggleResult.Success
        }

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
        return resolveTree(treeUri)?.findFile(fileName) != null
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
        val file = resolveTree(treeUri)?.findFile(fileName) ?: return true
        return file.delete()
    }

    private fun resolveTree(treeUri: String): DocumentFile? {
        return DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
    }
}
