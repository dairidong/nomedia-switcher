package com.nomedia.switcher.data.local.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class UserSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val settingsFile: File
        get() = temporaryFolder.root.resolve("user-settings.json")

    @Test
    fun settings_default_pinsHiddenAlbums() = runTest {
        val repository = UserSettingsRepository(
            dataStore = DataStoreFactory.create(
                serializer = UserSettingsSerializer,
                scope = backgroundScope,
                produceFile = { settingsFile },
            ),
        )

        assertTrue(repository.settings.first().pinHiddenAlbumsToTop)
        assertEquals(AlbumSortMode.ByName, repository.settings.first().albumSortMode)
    }

    @Test
    fun update_persistsPinHiddenAlbumsChoice() = runTest {
        val repository = UserSettingsRepository(
            dataStore = DataStoreFactory.create(
                serializer = UserSettingsSerializer,
                scope = backgroundScope,
                produceFile = { settingsFile },
            ),
        )

        repository.setPinHiddenAlbumsToTop(false)

        assertEquals(false, repository.settings.first().pinHiddenAlbumsToTop)
    }

    @Test
    fun update_persistsAlbumSortModeChoice() = runTest {
        val repository = UserSettingsRepository(
            dataStore = DataStoreFactory.create(
                serializer = UserSettingsSerializer,
                scope = backgroundScope,
                produceFile = { settingsFile },
            ),
        )

        repository.setAlbumSortMode(AlbumSortMode.ByLatestMedia)

        assertEquals(AlbumSortMode.ByLatestMedia, repository.settings.first().albumSortMode)
    }

    @Test
    fun create_canBeCalledTwiceForSameContext() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val first = UserSettingsRepository.create(context)
        val second = UserSettingsRepository.create(context)

        assertTrue(first.settings.first().pinHiddenAlbumsToTop)
        assertTrue(second.settings.first().pinHiddenAlbumsToTop)
    }

    @Test
    fun serializer_invalidPayload_throwsCorruption() = runTest {
        try {
            UserSettingsSerializer.readFrom("broken".byteInputStream() as InputStream)
            fail("Expected CorruptionException")
        } catch (_: CorruptionException) {
        }
    }

    @Test
    fun serializer_roundTripsPinHiddenAndSortMode() = runTest {
        val output = RecordingOutputStream()

        UserSettingsSerializer.writeTo(
            UserSettings(
                pinHiddenAlbumsToTop = false,
                albumSortMode = AlbumSortMode.ByLatestMedia,
            ),
            output,
        )

        val restored = UserSettingsSerializer.readFrom(
            output.toString().byteInputStream() as InputStream,
        )

        assertEquals(
            UserSettings(
                pinHiddenAlbumsToTop = false,
                albumSortMode = AlbumSortMode.ByLatestMedia,
            ),
            restored,
        )
    }

    private class RecordingOutputStream : OutputStream() {
        private val bytes = mutableListOf<Byte>()

        override fun write(b: Int) {
            bytes += b.toByte()
        }

        override fun toString(): String = bytes.toByteArray().decodeToString()
    }
}
