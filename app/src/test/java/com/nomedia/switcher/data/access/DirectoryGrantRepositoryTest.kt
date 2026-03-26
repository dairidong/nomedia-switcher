package com.nomedia.switcher.data.access

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DirectoryGrantRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DirectoryGrantRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        repository = DirectoryGrantRepository(database.directoryGrantDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun matching_tree_uri_isReturnedForKnownDirectory() = runTest {
        repository.saveGrant(
            directoryKey = "DCIM/Camera",
            treeUri = "content://tree/primary%3ADCIM%2FCamera",
        )

        assertEquals(
            "content://tree/primary%3ADCIM%2FCamera",
            repository.findGrant("DCIM/Camera"),
        )
    }

    @Test
    fun missing_grant_requests_user_action() = runTest {
        assertNull(repository.findGrant("Pictures/Secret"))
    }

    @Test
    fun newer_grant_replaces_stale_grant() = runTest {
        repository.saveGrant(
            directoryKey = "DCIM/Camera",
            treeUri = "content://tree/old",
        )
        repository.saveGrant(
            directoryKey = "DCIM/Camera",
            treeUri = "content://tree/new",
        )

        assertEquals("content://tree/new", repository.findGrant("DCIM/Camera"))
    }

    @Test
    fun restricted_root_returns_ungrantable_error() = runTest {
        assertEquals(
            GrantError.RestrictedRoot,
            repository.validateGrantRequest("Download"),
        )
    }
}
