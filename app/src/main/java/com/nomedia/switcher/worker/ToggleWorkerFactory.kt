package com.nomedia.switcher.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.data.toggle.NomediaDocumentGateway
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.usecase.AlbumStateWriter
import com.nomedia.switcher.notifications.ToggleNotificationFactory

class ToggleWorkerFactory(
    private val directoryGrantRepository: DirectoryGrantRepository,
    private val nomediaDocumentGateway: NomediaDocumentGateway,
    private val mediaRefreshCoordinator: MediaRefreshCoordinator,
    private val notificationFactory: WorkerNotificationFactory,
    private val albumStateWriter: AlbumStateWriter,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        if (workerClassName != ToggleAlbumWorker::class.java.name) {
            return null
        }

        return ToggleAlbumWorker(
            appContext = appContext,
            workerParams = workerParameters,
            directoryGrantRepository = directoryGrantRepository,
            nomediaDocumentGateway = nomediaDocumentGateway,
            mediaRefreshCoordinator = mediaRefreshCoordinator,
            notificationFactory = notificationFactory,
            albumStateWriter = albumStateWriter,
        )
    }
}

class DefaultWorkerNotificationFactory(
    private val context: Context,
) : WorkerNotificationFactory {
    private val notificationFactory = ToggleNotificationFactory(context)

    override fun buildProgressInfo(
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo {
        return notificationFactory.buildProgressInfo(albumName, action)
    }

    override fun notifyCompletion(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
        result: ToggleResult,
    ) {
        notificationFactory.notifyCompletion(directoryKey, albumName, action, result)
    }
}
