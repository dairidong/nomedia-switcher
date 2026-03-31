package com.nomedia.switcher.ui.access

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object DirectoryGrantLauncher {
    fun createInitialUri(directoryKey: String): Uri {
        return DocumentsContract.buildDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            "$PRIMARY_VOLUME:$directoryKey",
        )
    }

    fun createIntent(initialUri: Uri) = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
    }

    fun matchesDirectory(treeUri: Uri, directoryKey: String): Boolean {
        return DocumentsContract.getTreeDocumentId(treeUri) == "$PRIMARY_VOLUME:$directoryKey"
    }

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY_VOLUME = "primary"
}
