package com.nomedia.switcher.domain.usecase

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.worker.ToggleAlbumWorker

class EnqueueToggleAlbumUseCase(
    private val workManager: WorkManager,
    private val albumStateWriter: AlbumStateWriter,
) {
    suspend operator fun invoke(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
    ) {
        albumStateWriter.updateAlbum(
            directoryKey = directoryKey,
            displayName = albumName,
            state = AlbumState.Processing,
            lastAction = action,
            lastFailure = null,
            treeUri = null,
        )
        val request = OneTimeWorkRequestBuilder<ToggleAlbumWorker>()
            .addTag(ToggleAlbumWorker.WORK_TAG)
            .addTag(ToggleAlbumWorker.directoryTag(directoryKey))
            .setInputData(
                Data.Builder()
                    .putString(ToggleAlbumWorker.KEY_DIRECTORY_KEY, directoryKey)
                    .putString(ToggleAlbumWorker.KEY_ALBUM_NAME, albumName)
                    .putString(ToggleAlbumWorker.KEY_ACTION, action.name)
                    .build(),
            )
            .build()
        workManager.enqueue(request)
    }
}
