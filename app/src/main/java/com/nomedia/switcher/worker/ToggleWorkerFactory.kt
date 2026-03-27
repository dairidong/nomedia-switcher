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

class ToggleWorkerFactory(
    private val directoryGrantRepository: DirectoryGrantRepository,
    private val nomediaDocumentGateway: NomediaDocumentGateway,
    private val mediaRefreshCoordinator: MediaRefreshCoordinator,
    private val notificationFactory: WorkerNotificationFactory,
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
        )
    }
}

class DefaultWorkerNotificationFactory(
    private val context: Context,
) : WorkerNotificationFactory {
    override fun buildProgressInfo(
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo {
        return ToggleForegroundInfoFactory.create(context, albumName, action)
    }
}
