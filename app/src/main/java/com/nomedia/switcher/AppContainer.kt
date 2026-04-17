package com.nomedia.switcher

import android.app.Application
import androidx.work.WorkManager
import com.nomedia.switcher.data.cover.AndroidLogAlbumCoverCacheDiagnosticReporter
import com.nomedia.switcher.data.cover.AlbumCoverCacheStore
import com.nomedia.switcher.data.cover.AlbumCoverFallbackResolver
import com.nomedia.switcher.data.cover.SafTreeDocumentLookup
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.local.AppDatabase
import com.nomedia.switcher.data.local.album.RoomAlbumStateWriter
import com.nomedia.switcher.data.media.MediaStoreAlbumLoader
import com.nomedia.switcher.data.local.settings.UserSettingsRepository
import com.nomedia.switcher.data.toggle.NoopMediaRefreshCoordinator
import com.nomedia.switcher.data.toggle.NomediaDocumentGateway
import com.nomedia.switcher.data.toggle.SafNomediaDirectoryAccess
import com.nomedia.switcher.domain.usecase.EnqueueToggleAlbumUseCase
import com.nomedia.switcher.domain.usecase.PersistScannedAlbumCoverReferencesUseCase
import com.nomedia.switcher.domain.usecase.RecoverInterruptedAlbumTogglesUseCase
import com.nomedia.switcher.domain.usecase.SetAlbumSortModeUseCase
import com.nomedia.switcher.domain.usecase.SetHiddenAlbumsPinnedUseCase
import com.nomedia.switcher.worker.DefaultWorkerNotificationFactory
import com.nomedia.switcher.worker.ToggleWorkerFactory
import com.nomedia.switcher.worker.ToggleAlbumWorker

class AppContainer(
    application: Application,
) {
    val workManager: WorkManager by lazy { WorkManager.getInstance(application) }
    val database: AppDatabase = AppDatabase.create(application)
    val albumStateWriter = RoomAlbumStateWriter(database.albumRecordDao())
    val mediaStoreAlbumLoader = MediaStoreAlbumLoader()
    val userSettingsRepository = UserSettingsRepository.create(application)
    val directoryGrantRepository = DirectoryGrantRepository(database.directoryGrantDao())
    val albumCoverCacheStore = AlbumCoverCacheStore(
        context = application,
        diagnosticReporter = AndroidLogAlbumCoverCacheDiagnosticReporter(),
    )
    val albumCoverFallbackResolver = AlbumCoverFallbackResolver(
        documentLookup = SafTreeDocumentLookup(application),
        coverCacheStore = albumCoverCacheStore,
    )
    val persistScannedAlbumCoverReferencesUseCase = PersistScannedAlbumCoverReferencesUseCase(
        database.albumRecordDao(),
    )
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
        albumStateWriter = albumStateWriter,
        coverCacheStore = albumCoverCacheStore,
    )
    val enqueueToggleAlbumUseCase: EnqueueToggleAlbumUseCase by lazy {
        EnqueueToggleAlbumUseCase(
            workManager = workManager,
            albumStateWriter = albumStateWriter,
        )
    }
    val recoverInterruptedAlbumTogglesUseCase = RecoverInterruptedAlbumTogglesUseCase(
        findProcessingAlbums = {
            database.albumRecordDao().findByState(com.nomedia.switcher.domain.model.AlbumState.Processing)
        },
        unfinishedDirectoryKeys = {
            workManager.getWorkInfosByTag(ToggleAlbumWorker.WORK_TAG)
                .get()
                .asSequence()
                .filter { !it.state.isFinished }
                .flatMap { it.tags.asSequence() }
                .filter { it.startsWith("${ToggleAlbumWorker.WORK_TAG}:") }
                .map { it.removePrefix("${ToggleAlbumWorker.WORK_TAG}:") }
                .toSet()
        },
        findGrant = directoryGrantRepository::findGrant,
        directoryAccess = SafNomediaDirectoryAccess(application),
        albumStateWriter = albumStateWriter,
    )
    val setHiddenAlbumsPinnedUseCase = SetHiddenAlbumsPinnedUseCase(userSettingsRepository)
    val setAlbumSortModeUseCase = SetAlbumSortModeUseCase(userSettingsRepository)
}
