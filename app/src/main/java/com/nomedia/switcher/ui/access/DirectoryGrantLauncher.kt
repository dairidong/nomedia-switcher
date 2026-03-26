package com.nomedia.switcher.ui.access

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object DirectoryGrantLauncher {
    fun createIntent(initialUri: Uri) = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
    }
}
