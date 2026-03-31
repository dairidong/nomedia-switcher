package com.nomedia.switcher.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.testing.TestListenableWorkerBuilder
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.data.toggle.NomediaDocumentGateway
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.usecase.AlbumStateWriter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ToggleAlbumWorkerTest {
    @Test
    fun worker_marks_failure_when_grant_missing() = runTest {
        val albumStateWriter = FakeAlbumStateWriter()
        val notificationFactory = FakeWorkerNotificationFactory()
        val worker = buildWorker(
            grantRepository = FakeDirectoryGrantRepository(),
            gateway = FakeNomediaGateway(),
            albumStateWriter = albumStateWriter,
            notificationFactory = notificationFactory,
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(AlbumState.Failed, albumStateWriter.writes.single().state)
        assertEquals(
            listOf(
                CompletionNotification(
                    directoryKey = "DCIM/Camera",
                    albumName = "Camera",
                    action = ToggleAction.Hide,
                    result = ToggleResult.PermanentFailure("Missing directory grant"),
                ),
            ),
            notificationFactory.completions,
        )
    }

    @Test
    fun worker_retries_when_gateway_requests_retry() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val albumStateWriter = FakeAlbumStateWriter()
        val notificationFactory = FakeWorkerNotificationFactory()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = FakeNomediaGateway(result = ToggleResult.RetryableFailure("refresh pending")),
            albumStateWriter = albumStateWriter,
            notificationFactory = notificationFactory,
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(AlbumState.Failed, albumStateWriter.writes.single().state)
        assertEquals("refresh pending", albumStateWriter.writes.single().lastFailure)
        assertEquals(emptyList<CompletionNotification>(), notificationFactory.completions)
    }

    @Test
    fun worker_succeeds_when_hide_operation_completes() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val gateway = FakeNomediaGateway()
        val albumStateWriter = FakeAlbumStateWriter()
        val notificationFactory = FakeWorkerNotificationFactory()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = gateway,
            albumStateWriter = albumStateWriter,
            notificationFactory = notificationFactory,
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf("hide:DCIM/Camera"), gateway.calls)
        assertEquals(AlbumState.Hidden, albumStateWriter.writes.single().state)
        assertEquals(
            listOf(
                CompletionNotification(
                    directoryKey = "DCIM/Camera",
                    albumName = "Camera",
                    action = ToggleAction.Hide,
                    result = ToggleResult.Success,
                ),
            ),
            notificationFactory.completions,
        )
    }

    private fun buildWorker(
        grantRepository: FakeDirectoryGrantRepository,
        gateway: FakeNomediaGateway,
        albumStateWriter: FakeAlbumStateWriter,
        notificationFactory: FakeWorkerNotificationFactory,
    ): TestableToggleAlbumWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workerFactory = TestToggleWorkerFactory(
            grantRepository = grantRepository,
            gateway = gateway,
            mediaRefreshCoordinator = FakeMediaRefreshCoordinator(),
            notificationFactory = notificationFactory,
            albumStateWriter = albumStateWriter,
        )

        return TestListenableWorkerBuilder<TestableToggleAlbumWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(
                Data.Builder()
                    .putString(ToggleAlbumWorker.KEY_DIRECTORY_KEY, "DCIM/Camera")
                    .putString(ToggleAlbumWorker.KEY_ALBUM_NAME, "Camera")
                    .putString(ToggleAlbumWorker.KEY_ACTION, ToggleAction.Hide.name)
                    .build(),
            )
            .build()
    }

    private class TestableToggleAlbumWorker(
        appContext: Context,
        workerParams: androidx.work.WorkerParameters,
        directoryGrantRepository: DirectoryGrantLookup,
        nomediaDocumentGateway: NomediaToggleExecutor,
        mediaRefreshCoordinator: MediaRefreshCoordinator,
        notificationFactory: WorkerNotificationFactory,
        albumStateWriter: AlbumStateWriter,
    ) : ToggleAlbumWorker(
        appContext = appContext,
        workerParams = workerParams,
        directoryGrantRepository = directoryGrantRepository,
        nomediaDocumentGateway = nomediaDocumentGateway,
        mediaRefreshCoordinator = mediaRefreshCoordinator,
        notificationFactory = notificationFactory,
        albumStateWriter = albumStateWriter,
    ) {
        override suspend fun updateForeground(
            albumName: String,
            action: ToggleAction,
        ) {
        }
    }

    private class TestToggleWorkerFactory(
        private val grantRepository: FakeDirectoryGrantRepository,
        private val gateway: FakeNomediaGateway,
        private val mediaRefreshCoordinator: FakeMediaRefreshCoordinator,
        private val notificationFactory: FakeWorkerNotificationFactory,
        private val albumStateWriter: FakeAlbumStateWriter,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters,
        ): ListenableWorker? {
            if (workerClassName != TestableToggleAlbumWorker::class.java.name) {
                return null
            }
            return TestableToggleAlbumWorker(
                appContext = appContext,
                workerParams = workerParameters,
                directoryGrantRepository = grantRepository,
                nomediaDocumentGateway = gateway,
                mediaRefreshCoordinator = mediaRefreshCoordinator,
                notificationFactory = notificationFactory,
                albumStateWriter = albumStateWriter,
            )
        }
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
    }

    private class FakeDirectoryGrantRepository : DirectoryGrantLookup {
        val grants = mutableMapOf<String, String>()

        override suspend fun findGrant(directoryKey: String): String? = grants[directoryKey]
    }

    private class FakeNomediaGateway(
        private val result: ToggleResult = ToggleResult.Success,
    ) : NomediaToggleExecutor {
        val calls = mutableListOf<String>()

        override suspend fun hide(treeUri: String, directoryKey: String): ToggleResult {
            calls += "hide:$directoryKey"
            return result
        }

        override suspend fun show(treeUri: String, directoryKey: String): ToggleResult {
            calls += "show:$directoryKey"
            return result
        }
    }

    private class FakeMediaRefreshCoordinator : MediaRefreshCoordinator {
        override suspend fun refresh(directoryKey: String): ToggleResult = ToggleResult.Success
    }

    private class FakeWorkerNotificationFactory : WorkerNotificationFactory {
        val completions = mutableListOf<CompletionNotification>()

        override fun buildProgressInfo(
            albumName: String,
            action: ToggleAction,
        ) = ToggleForegroundInfoFactory.create(
            context = ApplicationProvider.getApplicationContext(),
            albumName = albumName,
            action = action,
        )

        override fun notifyCompletion(
            directoryKey: String,
            albumName: String,
            action: ToggleAction,
            result: ToggleResult,
        ) {
            completions += CompletionNotification(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                result = result,
            )
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

private data class CompletionNotification(
    val directoryKey: String,
    val albumName: String,
    val action: ToggleAction,
    val result: ToggleResult,
)
