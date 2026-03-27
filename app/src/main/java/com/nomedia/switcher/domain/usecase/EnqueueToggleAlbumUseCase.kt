package com.nomedia.switcher.domain.usecase

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.worker.ToggleAlbumWorker

class EnqueueToggleAlbumUseCase(
    private val workManager: WorkManager,
) {
    operator fun invoke(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
    ) {
        val request = OneTimeWorkRequestBuilder<ToggleAlbumWorker>()
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
