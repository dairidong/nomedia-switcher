package com.nomedia.switcher.domain.usecase

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    sdk = [34],
    application = Application::class,
)
class EnqueueToggleAlbumUseCaseTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private val albumStateWriter = FakeAlbumStateWriter()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            androidx.work.Configuration.Builder()
                .setWorkerFactory(NoopWorkerFactory())
                .build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun invoke_marks_album_processing_before_enqueue() = runTest {
        val useCase = EnqueueToggleAlbumUseCase(
            workManager = workManager,
            albumStateWriter = albumStateWriter,
        )

        useCase(
            directoryKey = "DCIM/Camera",
            albumName = "Camera",
            action = ToggleAction.Hide,
        )

        assertEquals(
            listOf(
                AlbumWrite(
                    directoryKey = "DCIM/Camera",
                    displayName = "Camera",
                    state = AlbumState.Processing,
                    lastAction = ToggleAction.Hide,
                    lastFailure = null,
                    treeUri = null,
                ),
            ),
            albumStateWriter.writes,
        )
    }

    @Test
    fun invoke_tags_work_with_directory_key() = runTest {
        val useCase = EnqueueToggleAlbumUseCase(
            workManager = workManager,
            albumStateWriter = albumStateWriter,
        )

        useCase(
            directoryKey = "DCIM/Camera",
            albumName = "Camera",
            action = ToggleAction.Hide,
        )

        val workInfos = workManager.getWorkInfosByTag(
            com.nomedia.switcher.worker.ToggleAlbumWorker.directoryTag("DCIM/Camera"),
        ).get()

        assertEquals(1, workInfos.size)
        assertTrue(workInfos.single().tags.contains(com.nomedia.switcher.worker.ToggleAlbumWorker.WORK_TAG))
    }

    private class NoopWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? = null
    }

    private class FakeAlbumStateWriter : AlbumStateWriter {
        val writes = mutableListOf<AlbumWrite>()

        override suspend fun updateAlbum(
            directoryKey: String,
            displayName: String,
            state: AlbumState,
            lastAction: ToggleAction,
            lastFailure: String?,
            treeUri: String?,
        ) {
            writes += AlbumWrite(
                directoryKey = directoryKey,
                displayName = displayName,
                state = state,
                lastAction = lastAction,
                lastFailure = lastFailure,
                treeUri = treeUri,
            )
        }

        override suspend fun updateCachedCover(
            directoryKey: String,
            cachedCoverPath: String,
            cachedCoverMediaKind: String,
            cachedCoverUpdatedAtEpochMs: Long,
        ) {
        }
    }
}

private data class AlbumWrite(
    val directoryKey: String,
    val displayName: String,
    val state: AlbumState,
    val lastAction: ToggleAction,
    val lastFailure: String?,
    val treeUri: String?,
)
