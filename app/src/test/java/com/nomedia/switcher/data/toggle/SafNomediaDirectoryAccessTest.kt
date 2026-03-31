package com.nomedia.switcher.data.toggle

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SafNomediaDirectoryAccessTest {
    @Test
    fun exists_returns_false_when_provider_throws_illegal_argument_for_missing_dotfile() = kotlinx.coroutines.test.runTest {
        ShadowContentResolver.registerProviderInternal(
            AUTHORITY,
            ThrowingQueryProvider(),
        )
        val access = SafNomediaDirectoryAccess(ApplicationProvider.getApplicationContext())

        val exists = access.exists(
            treeUri = "content://$AUTHORITY/tree/primary%3APictures%2FScreenshots",
            fileName = ".nomedia",
        )

        assertEquals(false, exists)
    }

    private class ThrowingQueryProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            throw IllegalArgumentException(
                "Failed to determine if primary:Pictures/Screenshots/.nomedia is child of primary:Pictures/Screenshots",
            )
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        const val AUTHORITY = "com.android.externalstorage.documents"
    }
}
