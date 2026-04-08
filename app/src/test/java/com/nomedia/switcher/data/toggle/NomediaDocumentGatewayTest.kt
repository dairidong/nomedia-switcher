package com.nomedia.switcher.data.toggle

import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.model.ToggleFailureReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NomediaDocumentGatewayTest {
    private val fakeDirectory = FakeNomediaDirectoryAccess()
    private val gateway = NomediaDocumentGateway(fakeDirectory)

    @Test
    fun hide_creates_nomedia_file_when_missing() = runTest {
        val result = gateway.hide(
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
            directoryKey = "DCIM/Camera",
        )

        assertEquals(listOf(".nomedia"), fakeDirectory.createdFiles)
        assertEquals(ToggleResult.Success, result)
    }

    @Test
    fun hide_fails_when_provider_reports_create_success_but_file_still_missing() = runTest {
        fakeDirectory.keepCreatedFilesInvisible = true

        val result = gateway.hide(
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
            directoryKey = "DCIM/Camera",
        )

        assertEquals(
            ToggleResult.PermanentFailure(ToggleFailureReason.UnableToCreateNomedia.persistedKey),
            result,
        )
    }

    @Test
    fun show_deletes_nomedia_file_when_present() = runTest {
        fakeDirectory.presentFiles += ".nomedia"

        val result = gateway.show(
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
            directoryKey = "DCIM/Camera",
        )

        assertEquals(listOf(".nomedia"), fakeDirectory.deletedFiles)
        assertEquals(ToggleResult.Success, result)
    }

    @Test
    fun show_still_attempts_delete_when_dotfile_is_not_listable() = runTest {
        fakeDirectory.existsResult = false

        val result = gateway.show(
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
            directoryKey = "DCIM/Camera",
        )

        assertEquals(listOf(".nomedia"), fakeDirectory.deletedFiles)
        assertEquals(ToggleResult.Success, result)
    }

    @Test
    fun show_returns_stable_failure_key_when_delete_fails() = runTest {
        fakeDirectory.deleteResultOverride = false

        val result = gateway.show(
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
            directoryKey = "DCIM/Camera",
        )

        assertEquals(
            ToggleResult.PermanentFailure(ToggleFailureReason.UnableToRemoveNomedia.persistedKey),
            result,
        )
    }

    private class FakeNomediaDirectoryAccess : NomediaDirectoryAccess {
        val presentFiles = linkedSetOf<String>()
        val createdFiles = mutableListOf<String>()
        val deletedFiles = mutableListOf<String>()
        var existsResult: Boolean? = null
        var keepCreatedFilesInvisible: Boolean = false
        var deleteResultOverride: Boolean? = null

        override suspend fun exists(
            treeUri: String,
            fileName: String,
        ): Boolean = existsResult ?: (fileName in presentFiles)

        override suspend fun createFile(
            treeUri: String,
            fileName: String,
        ): Boolean {
            createdFiles += fileName
            if (!keepCreatedFilesInvisible) {
                presentFiles += fileName
            }
            return true
        }

        override suspend fun deleteFile(
            treeUri: String,
            fileName: String,
        ): Boolean {
            deletedFiles += fileName
            deleteResultOverride?.let { return it }
            return presentFiles.remove(fileName) || existsResult == false
        }
    }
}
