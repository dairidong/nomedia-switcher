package com.nomedia.switcher.data.local.settings

import androidx.datastore.core.DataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
    }
}
