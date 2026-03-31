package com.nomedia.switcher.data.toggle

import com.nomedia.switcher.domain.model.ToggleResult
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

    private class FakeNomediaDirectoryAccess : NomediaDirectoryAccess {
        val presentFiles = linkedSetOf<String>()
        val createdFiles = mutableListOf<String>()
        val deletedFiles = mutableListOf<String>()
        var existsResult: Boolean? = null

        override suspend fun exists(
            treeUri: String,
            fileName: String,
        ): Boolean = existsResult ?: (fileName in presentFiles)

        override suspend fun createFile(
            treeUri: String,
            fileName: String,
        ): Boolean {
            createdFiles += fileName
            presentFiles += fileName
            return true
        }

        override suspend fun deleteFile(
            treeUri: String,
            fileName: String,
        ): Boolean {
            deletedFiles += fileName
            return presentFiles.remove(fileName) || existsResult == false
        }
    }
}
