package com.nomedia.switcher.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.local.AppDatabase
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ResolveToggleRequestUseCaseTest {
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
    fun missing_grant_requests_directory_access() = runTest {
        val useCase = ResolveToggleRequestUseCase(repository)

        val result = useCase(
            directoryKey = "Pictures/Cyberpunk 2077",
            albumName = "Cyberpunk 2077",
            action = ToggleAction.Hide,
        )

        assertTrue(result is ToggleRequestResolution.RequestGrant)
        result as ToggleRequestResolution.RequestGrant
        assertEquals("Pictures/Cyberpunk 2077", result.directoryKey)
        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3APictures%2FCyberpunk%202077",
            result.initialUri.toString(),
        )
    }

    @Test
    fun known_grant_is_ready_to_enqueue() = runTest {
        repository.saveGrant(
            directoryKey = "Pictures/Cyberpunk 2077",
            treeUri = "content://com.android.externalstorage.documents/tree/primary%3APictures%2FCyberpunk%202077",
        )
        val useCase = ResolveToggleRequestUseCase(repository)

        val result = useCase(
            directoryKey = "Pictures/Cyberpunk 2077",
            albumName = "Cyberpunk 2077",
            action = ToggleAction.Hide,
        )

        assertEquals(
            ToggleRequestResolution.Enqueue(
                directoryKey = "Pictures/Cyberpunk 2077",
                albumName = "Cyberpunk 2077",
                action = ToggleAction.Hide,
            ),
            result,
        )
    }
}
