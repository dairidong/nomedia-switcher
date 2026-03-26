package com.nomedia.switcher.ui.access

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DirectoryGrantFlowTest {
    @Test
    fun createIntent_includesPersistableGrantFlags() {
        val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera")

        val intent = DirectoryGrantLauncher.createIntent(initialUri)

        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, intent.action)
        assertEquals(initialUri, intent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
    }
}
