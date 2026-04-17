package com.nomedia.switcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nomedia.switcher.data.cover.AlbumCoverCacheStore
import com.nomedia.switcher.data.toggle.MediaRefreshCoordinator
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleResult
import com.nomedia.switcher.domain.model.shouldPersistAlbumFailureState
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
    private val coverCacheStore: AlbumCoverCacheStore,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val directoryKey = inputData.getString(KEY_DIRECTORY_KEY)
            ?: return Result.failure()
        val albumName = inputData.getString(KEY_ALBUM_NAME)
            ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION)
            ?.let(ToggleAction::valueOf)
            ?: return Result.failure()
        val coverUri = inputData.getString(KEY_COVER_URI)
        val coverMediaKind = inputData.getString(KEY_COVER_MEDIA_KIND)
        var coverCacheWarningCode: String? = null

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

        if (
            action == ToggleAction.Hide &&
            !coverUri.isNullOrBlank() &&
            !coverMediaKind.isNullOrBlank()
        ) {
            runCatching {
                coverCacheStore.createOrUpdateDetailed(
                    directoryKey = directoryKey,
                    sourceUri = coverUri,
                    sourceMediaKind = coverMediaKind,
                )
            }.getOrNull()?.let { result ->
                result.cachedCoverRef?.let { cached ->
                    albumStateWriter.updateCachedCover(
                        directoryKey = directoryKey,
                        cachedCoverPath = cached.absolutePath,
                        cachedCoverMediaKind = cached.mediaKind,
                        cachedCoverUpdatedAtEpochMs = cached.updatedAtEpochMs,
                    )
                }
                coverCacheWarningCode = result.warningCode?.persistedKey
            }
        }

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
                coverCacheWarningCode?.let { warningCode ->
                    Result.success(workDataOf(KEY_COVER_CACHE_WARNING_CODE to warningCode))
                } ?: Result.success()
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
        val persistedReason = ToggleFailureReason.fromPersistedKey(reason)
        albumStateWriter.updateAlbum(
            directoryKey = directoryKey,
            displayName = albumName,
            state = if (persistedReason?.shouldPersistAlbumFailureState() == false) {
                rollbackState(action)
            } else {
                AlbumState.Failed
            },
            lastAction = action,
            lastFailure = reason.takeIf { persistedReason?.shouldPersistAlbumFailureState() != false },
            treeUri = treeUri,
        )
        if (notifyCompletion) {
            notificationFactory.notifyCompletion(directoryKey, albumName, action, failure)
        }
        return result
    }

    private fun rollbackState(action: ToggleAction): AlbumState {
        return when (action) {
            ToggleAction.Hide -> AlbumState.Shown
            ToggleAction.Show -> AlbumState.Hidden
        }
    }

    companion object {
        const val WORK_TAG = "toggle_album"
        const val KEY_DIRECTORY_KEY = "directory_key"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_ACTION = "action"
        const val KEY_COVER_URI = "cover_uri"
        const val KEY_COVER_MEDIA_KIND = "cover_media_kind"
        const val KEY_COVER_CACHE_WARNING_CODE = "cover_cache_warning_code"

        fun directoryTag(directoryKey: String): String = "toggle_album:$directoryKey"
    }
}
