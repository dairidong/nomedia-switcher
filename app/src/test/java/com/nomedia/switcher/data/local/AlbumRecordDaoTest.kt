package com.nomedia.switcher.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class AlbumRecordDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun hidden_album_record_roundTrips() = runTest {
        val record = AlbumRecordEntity(
            directoryKey = "DCIM/Camera",
            displayName = "Camera",
            state = AlbumState.Hidden,
            treeUri = "content://tree/camera",
            lastAction = ToggleAction.Hide,
            lastFailure = "none",
            seenInLastScan = true,
            updatedAtEpochMs = 1234L,
        )

        database.albumRecordDao().upsert(record)

        val records = database.albumRecordDao().observeAll().first()

        assertEquals(listOf(record), records)
    }
}
