package com.nomedia.switcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleResult

interface DirectoryGrantLookup {
    suspend fun findGrant(directoryKey: String): String?
}

interface NomediaToggleExecutor {
    suspend fun hide(treeUri: String, directoryKey: String): ToggleResult
    suspend fun show(treeUri: String, directoryKey: String): ToggleResult
}

interface WorkerNotificationFactory {
    fun buildProgressInfo(
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo
}

open class ToggleAlbumWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val directoryGrantRepository: DirectoryGrantLookup,
    private val nomediaDocumentGateway: NomediaToggleExecutor,
    private val mediaRefreshCoordinator: MediaRefreshCoordinator,
    private val notificationFactory: WorkerNotificationFactory,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val directoryKey = inputData.getString(KEY_DIRECTORY_KEY)
            ?: return Result.failure()
        val albumName = inputData.getString(KEY_ALBUM_NAME)
            ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION)
            ?.let(ToggleAction::valueOf)
            ?: return Result.failure()

        updateForeground(albumName, action)

        val treeUri = directoryGrantRepository.findGrant(directoryKey)
            ?: return Result.failure()

        val gatewayResult = when (action) {
            ToggleAction.Hide -> nomediaDocumentGateway.hide(treeUri, directoryKey)
            ToggleAction.Show -> nomediaDocumentGateway.show(treeUri, directoryKey)
        }

        return when (gatewayResult) {
            is ToggleResult.Success -> when (mediaRefreshCoordinator.refresh(directoryKey)) {
                is ToggleResult.Success -> Result.success()
                is ToggleResult.RetryableFailure -> Result.retry()
                is ToggleResult.PermanentFailure -> Result.failure()
            }
            is ToggleResult.RetryableFailure -> Result.retry()
            is ToggleResult.PermanentFailure -> Result.failure()
        }
    }

    protected open suspend fun updateForeground(
        albumName: String,
        action: ToggleAction,
    ) {
        setForeground(notificationFactory.buildProgressInfo(albumName, action))
    }

    companion object {
        const val KEY_DIRECTORY_KEY = "directory_key"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_ACTION = "action"
    }
}
