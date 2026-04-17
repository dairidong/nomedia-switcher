package com.nomedia.switcher.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.testing.TestListenableWorkerBuilder
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.cover.AlbumCoverCacheStore
import com.nomedia.switcher.data.cover.AlbumCoverCacheStoreResult
import com.nomedia.switcher.data.cover.AlbumCoverWarningCode
import com.nomedia.switcher.data.cover.CachedAlbumCoverRef
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.data.toggle.NomediaDocumentGateway
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.usecase.AlbumStateWriter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class ToggleAlbumWorkerTest {
    @Test
    fun worker_rolls_back_to_shown_state_when_grant_is_missing() = runTest {
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
        assertEquals(AlbumState.Shown, albumStateWriter.writes.single().state)
        assertEquals(null, albumStateWriter.writes.single().lastFailure)
        assertEquals(
            listOf(
                CompletionNotification(
                    directoryKey = "DCIM/Camera",
                    albumName = "Camera",
                    action = ToggleAction.Hide,
                    result = ToggleResult.PermanentFailure(ToggleFailureReason.MissingDirectoryGrant.persistedKey),
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

    @Test
    fun worker_hide_attempts_cover_cache_before_nomedia_write() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val cacheStore = FakeAlbumCoverCacheStore().apply {
            result = CachedAlbumCoverRef(
                absolutePath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/dcim-camera.webp",
                mediaKind = "image",
                updatedAtEpochMs = 88L,
            )
        }
        val albumStateWriter = FakeAlbumStateWriter()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = FakeNomediaGateway(),
            albumStateWriter = albumStateWriter,
            notificationFactory = FakeWorkerNotificationFactory(),
            cacheStore = cacheStore,
            coverUri = "content://media/external/video/media/7",
            coverMediaKind = "video",
        )

        worker.doWork()

        assertEquals(
            listOf(
                CacheRequest(
                    directoryKey = "DCIM/Camera",
                    sourceUri = "content://media/external/video/media/7",
                    sourceMediaKind = "video",
                ),
            ),
            cacheStore.requests,
        )
        assertEquals(
            listOf(
                CachedCoverWrite(
                    directoryKey = "DCIM/Camera",
                    cachedCoverPath = "/data/user/0/com.nomedia.switcher/files/album-cover-cache/dcim-camera.webp",
                    cachedCoverMediaKind = "image",
                    cachedCoverUpdatedAtEpochMs = 88L,
                ),
            ),
            albumStateWriter.cachedCoverWrites,
        )
    }

    @Test
    fun worker_hide_continues_when_cover_cache_creation_fails() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val cacheStore = FakeAlbumCoverCacheStore().apply {
            failure = IllegalStateException("decode failed")
        }
        val albumStateWriter = FakeAlbumStateWriter()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = FakeNomediaGateway(),
            albumStateWriter = albumStateWriter,
            notificationFactory = FakeWorkerNotificationFactory(),
            cacheStore = cacheStore,
            coverUri = "content://media/external/video/media/7",
            coverMediaKind = "video",
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(AlbumState.Hidden, albumStateWriter.writes.single().state)
        assertTrue(albumStateWriter.cachedCoverWrites.isEmpty())
    }

    @Test
    fun worker_hide_returns_warning_output_when_cover_cache_creation_fails() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val cacheStore = FakeAlbumCoverCacheStore().apply {
            detailedResult = AlbumCoverCacheStoreResult.failed(
                warningCode = AlbumCoverWarningCode.VideoThumbnailAndFrameFailed,
            )
        }
        val albumStateWriter = FakeAlbumStateWriter()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = FakeNomediaGateway(),
            albumStateWriter = albumStateWriter,
            notificationFactory = FakeWorkerNotificationFactory(),
            cacheStore = cacheStore,
            coverUri = "content://media/external/video/media/7",
            coverMediaKind = "video",
        )

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.success(
                Data.Builder()
                    .putString(
                        ToggleAlbumWorker.KEY_COVER_CACHE_WARNING_CODE,
                        AlbumCoverWarningCode.VideoThumbnailAndFrameFailed.persistedKey,
                    )
                    .build(),
            ),
            result,
        )
    }

    @Test
    fun worker_show_does_not_attempt_cover_cache_creation() = runTest {
        val grantRepository = FakeDirectoryGrantRepository().apply {
            grants["DCIM/Camera"] = "content://tree/camera"
        }
        val cacheStore = FakeAlbumCoverCacheStore()
        val albumStateWriter = FakeAlbumStateWriter()
        val worker = buildWorker(
            grantRepository = grantRepository,
            gateway = FakeNomediaGateway(),
            albumStateWriter = albumStateWriter,
            notificationFactory = FakeWorkerNotificationFactory(),
            cacheStore = cacheStore,
            action = ToggleAction.Show,
        )

        worker.doWork()

        assertTrue(cacheStore.requests.isEmpty())
    }

    private fun buildWorker(
        grantRepository: FakeDirectoryGrantRepository,
        gateway: FakeNomediaGateway,
        albumStateWriter: FakeAlbumStateWriter,
        notificationFactory: FakeWorkerNotificationFactory,
        cacheStore: FakeAlbumCoverCacheStore = FakeAlbumCoverCacheStore(),
        action: ToggleAction = ToggleAction.Hide,
        coverUri: String? = null,
        coverMediaKind: String? = null,
    ): TestableToggleAlbumWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workerFactory = TestToggleWorkerFactory(
            grantRepository = grantRepository,
            gateway = gateway,
            mediaRefreshCoordinator = FakeMediaRefreshCoordinator(),
            notificationFactory = notificationFactory,
            albumStateWriter = albumStateWriter,
            cacheStore = cacheStore,
        )

        return TestListenableWorkerBuilder<TestableToggleAlbumWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(
                Data.Builder()
                    .putString(ToggleAlbumWorker.KEY_DIRECTORY_KEY, "DCIM/Camera")
                    .putString(ToggleAlbumWorker.KEY_ALBUM_NAME, "Camera")
                    .putString(ToggleAlbumWorker.KEY_ACTION, action.name)
                    .putString(ToggleAlbumWorker.KEY_COVER_URI, coverUri)
                    .putString(ToggleAlbumWorker.KEY_COVER_MEDIA_KIND, coverMediaKind)
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
        coverCacheStore: AlbumCoverCacheStore,
    ) : ToggleAlbumWorker(
        appContext = appContext,
        workerParams = workerParams,
        directoryGrantRepository = directoryGrantRepository,
        nomediaDocumentGateway = nomediaDocumentGateway,
        mediaRefreshCoordinator = mediaRefreshCoordinator,
        notificationFactory = notificationFactory,
        albumStateWriter = albumStateWriter,
        coverCacheStore = coverCacheStore,
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
        private val cacheStore: FakeAlbumCoverCacheStore,
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
                coverCacheStore = cacheStore,
            )
        }
    }

    private class FakeAlbumStateWriter : AlbumStateWriter {
        val writes = mutableListOf<AlbumWrite>()
        val cachedCoverWrites = mutableListOf<CachedCoverWrite>()

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
            cachedCoverWrites += CachedCoverWrite(
                directoryKey = directoryKey,
                cachedCoverPath = cachedCoverPath,
                cachedCoverMediaKind = cachedCoverMediaKind,
                cachedCoverUpdatedAtEpochMs = cachedCoverUpdatedAtEpochMs,
            )
        }
    }

    private class FakeAlbumCoverCacheStore : AlbumCoverCacheStore() {
        val requests = mutableListOf<CacheRequest>()
        var result: CachedAlbumCoverRef? = null
        var detailedResult: AlbumCoverCacheStoreResult? = null
        var failure: Throwable? = null

        override suspend fun createOrUpdateDetailed(
            directoryKey: String,
            sourceUri: String,
            sourceMediaKind: String,
        ): AlbumCoverCacheStoreResult {
            requests += CacheRequest(directoryKey, sourceUri, sourceMediaKind)
            failure?.let { throw it }
            return detailedResult ?: AlbumCoverCacheStoreResult.succeeded(result)
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

private data class CacheRequest(
    val directoryKey: String,
    val sourceUri: String,
    val sourceMediaKind: String,
)

private data class CachedCoverWrite(
    val directoryKey: String,
    val cachedCoverPath: String,
    val cachedCoverMediaKind: String,
    val cachedCoverUpdatedAtEpochMs: Long,
)

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
