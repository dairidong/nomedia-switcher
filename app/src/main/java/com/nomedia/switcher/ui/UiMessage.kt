package com.nomedia.switcher.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nomedia.switcher.R
import com.nomedia.switcher.data.cover.AlbumCoverWarningCode
import com.nomedia.switcher.domain.model.ToggleFailureReason

sealed interface UiMessage {
    data object AlbumHidden : UiMessage
    data object LastActionFailed : UiMessage
    data object HideInProgress : UiMessage
    data object ShowInProgress : UiMessage
    data object DirectoryCannotBeGranted : UiMessage
    data object DirectoryAccessNotGranted : UiMessage
    data object WrongFolderSelected : UiMessage
    data object PersistAccessDenied : UiMessage
    data object PreviousTaskInterrupted : UiMessage
    data object DirectoryGrantMissing : UiMessage
    data object UnableToCreateNomedia : UiMessage
    data object UnableToRemoveNomedia : UiMessage
    data object VideoThumbnailFailed : UiMessage
    data object VideoFrameFailed : UiMessage
    data object VideoThumbnailAndFrameFailed : UiMessage
    data object CoverCacheWriteFailed : UiMessage
    data object CoverCacheFailed : UiMessage
    data class Raw(val value: String) : UiMessage
}

fun UiMessage.resolve(context: Context): String = when (this) {
    UiMessage.AlbumHidden -> context.getString(R.string.album_status_hidden)
    UiMessage.LastActionFailed -> context.getString(R.string.album_status_last_action_failed)
    UiMessage.HideInProgress -> context.getString(R.string.album_status_hide_in_progress)
    UiMessage.ShowInProgress -> context.getString(R.string.album_status_show_in_progress)
    UiMessage.DirectoryCannotBeGranted -> context.getString(R.string.album_failure_restricted_root)
    UiMessage.DirectoryAccessNotGranted -> context.getString(R.string.album_failure_grant_denied)
    UiMessage.WrongFolderSelected -> context.getString(R.string.album_failure_wrong_directory_selected)
    UiMessage.PersistAccessDenied -> context.getString(R.string.album_failure_persist_permission_denied)
    UiMessage.PreviousTaskInterrupted -> context.getString(R.string.album_failure_interrupted)
    UiMessage.DirectoryGrantMissing -> context.getString(R.string.album_failure_missing_directory_grant)
    UiMessage.UnableToCreateNomedia -> context.getString(R.string.album_failure_unable_to_create_nomedia)
    UiMessage.UnableToRemoveNomedia -> context.getString(R.string.album_failure_unable_to_remove_nomedia)
    UiMessage.VideoThumbnailFailed -> context.getString(R.string.album_warning_video_thumbnail_failed)
    UiMessage.VideoFrameFailed -> context.getString(R.string.album_warning_video_frame_failed)
    UiMessage.VideoThumbnailAndFrameFailed -> context.getString(R.string.album_warning_video_thumbnail_and_frame_failed)
    UiMessage.CoverCacheWriteFailed -> context.getString(R.string.album_warning_cover_cache_write_failed)
    UiMessage.CoverCacheFailed -> context.getString(R.string.album_warning_cover_cache_failed)
    is UiMessage.Raw -> value
}

@Composable
fun UiMessage.resolve(): String = resolve(LocalContext.current)

internal fun ToggleFailureReason.toUiMessage(): UiMessage = when (this) {
    ToggleFailureReason.RestrictedRoot -> UiMessage.DirectoryCannotBeGranted
    ToggleFailureReason.GrantDenied -> UiMessage.DirectoryAccessNotGranted
    ToggleFailureReason.WrongDirectorySelected -> UiMessage.WrongFolderSelected
    ToggleFailureReason.PersistPermissionDenied -> UiMessage.PersistAccessDenied
    ToggleFailureReason.Interrupted -> UiMessage.PreviousTaskInterrupted
    ToggleFailureReason.MissingDirectoryGrant -> UiMessage.DirectoryGrantMissing
    ToggleFailureReason.UnableToCreateNomedia -> UiMessage.UnableToCreateNomedia
    ToggleFailureReason.UnableToRemoveNomedia -> UiMessage.UnableToRemoveNomedia
}

internal fun AlbumCoverWarningCode.toUiMessage(): UiMessage = when (this) {
    AlbumCoverWarningCode.VideoThumbnailFailed -> UiMessage.VideoThumbnailFailed
    AlbumCoverWarningCode.VideoFrameFailed -> UiMessage.VideoFrameFailed
    AlbumCoverWarningCode.VideoThumbnailAndFrameFailed -> UiMessage.VideoThumbnailAndFrameFailed
    AlbumCoverWarningCode.CacheWriteFailed -> UiMessage.CoverCacheWriteFailed
    AlbumCoverWarningCode.Generic -> UiMessage.CoverCacheFailed
}
