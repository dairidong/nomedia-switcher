package com.nomedia.switcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.usecase.AlbumStateWriter

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

    fun notifyCompletion(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
        result: ToggleResult,
    )
}

open class ToggleAlbumWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val directoryGrantRepository: DirectoryGrantLookup,
    private val nomediaDocumentGateway: NomediaToggleExecutor,
    private val mediaRefreshCoordinator: MediaRefreshCoordinator,
    private val notificationFactory: WorkerNotificationFactory,
    private val albumStateWriter: AlbumStateWriter,
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
            ?: return fail(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                reason = ToggleFailureReason.MissingDirectoryGrant.persistedKey,
                notifyCompletion = true,
                result = Result.failure(),
            )

        val gatewayResult = when (action) {
            ToggleAction.Hide -> nomediaDocumentGateway.hide(treeUri, directoryKey)
            ToggleAction.Show -> nomediaDocumentGateway.show(treeUri, directoryKey)
        }

        val finalResult = when (gatewayResult) {
            is ToggleResult.Success -> mediaRefreshCoordinator.refresh(directoryKey)
            is ToggleResult.RetryableFailure -> gatewayResult
            is ToggleResult.PermanentFailure -> gatewayResult
        }

        return when (finalResult) {
            is ToggleResult.Success -> {
                albumStateWriter.updateAlbum(
                    directoryKey = directoryKey,
                    displayName = albumName,
                    state = if (action == ToggleAction.Hide) AlbumState.Hidden else AlbumState.Shown,
                    lastAction = action,
                    lastFailure = null,
                    treeUri = treeUri,
                )
                notificationFactory.notifyCompletion(directoryKey, albumName, action, finalResult)
                Result.success()
            }
            is ToggleResult.RetryableFailure -> fail(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                reason = finalResult.reason,
                notifyCompletion = false,
                treeUri = treeUri,
                result = Result.retry(),
            )
            is ToggleResult.PermanentFailure -> fail(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                reason = finalResult.reason,
                notifyCompletion = true,
                treeUri = treeUri,
                result = Result.failure(),
            )
        }
    }

    protected open suspend fun updateForeground(
        albumName: String,
        action: ToggleAction,
    ) {
        setForeground(notificationFactory.buildProgressInfo(albumName, action))
    }

    private suspend fun fail(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
        reason: String,
        notifyCompletion: Boolean,
        treeUri: String? = null,
        result: Result,
    ): Result {
        val failure = ToggleResult.PermanentFailure(reason)
        albumStateWriter.updateAlbum(
            directoryKey = directoryKey,
            displayName = albumName,
            state = AlbumState.Failed,
            lastAction = action,
            lastFailure = reason,
            treeUri = treeUri,
        )
        if (notifyCompletion) {
            notificationFactory.notifyCompletion(directoryKey, albumName, action, failure)
        }
        return result
    }

    companion object {
        const val WORK_TAG = "toggle_album"
        const val KEY_DIRECTORY_KEY = "directory_key"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_ACTION = "action"

        fun directoryTag(directoryKey: String): String = "toggle_album:$directoryKey"
    }
}
