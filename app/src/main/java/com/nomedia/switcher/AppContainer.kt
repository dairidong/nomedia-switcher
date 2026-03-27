package com.nomedia.switcher

import android.app.Application
import androidx.work.WorkManager
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.local.AppDatabase
import com.nomedia.switcher.data.toggle.NoopMediaRefreshCoordinator
import com.nomedia.switcher.data.toggle.NomediaDocumentGateway
import com.nomedia.switcher.data.toggle.SafNomediaDirectoryAccess
import com.nomedia.switcher.domain.usecase.EnqueueToggleAlbumUseCase
import com.nomedia.switcher.worker.DefaultWorkerNotificationFactory
import com.nomedia.switcher.worker.ToggleWorkerFactory

class AppContainer(
    application: Application,
) {
    val database: AppDatabase = AppDatabase.create(application)
    val directoryGrantRepository = DirectoryGrantRepository(database.directoryGrantDao())
    val nomediaDocumentGateway = NomediaDocumentGateway(
        directoryAccess = SafNomediaDirectoryAccess(application),
    )
    val mediaRefreshCoordinator = NoopMediaRefreshCoordinator()
    val workerNotificationFactory = DefaultWorkerNotificationFactory(application)
    val toggleWorkerFactory = ToggleWorkerFactory(
        directoryGrantRepository = directoryGrantRepository,
        nomediaDocumentGateway = nomediaDocumentGateway,
        mediaRefreshCoordinator = mediaRefreshCoordinator,
        notificationFactory = workerNotificationFactory,
    )
    val enqueueToggleAlbumUseCase: EnqueueToggleAlbumUseCase by lazy {
        EnqueueToggleAlbumUseCase(
            workManager = WorkManager.getInstance(application),
        )
    }
}
