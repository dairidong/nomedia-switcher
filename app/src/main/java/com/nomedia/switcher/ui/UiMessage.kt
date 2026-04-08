package com.nomedia.switcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nomedia.switcher.R

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
    data class Raw(val value: String) : UiMessage
}

@Composable
fun UiMessage.resolve(): String = when (this) {
    UiMessage.AlbumHidden -> stringResource(id = R.string.album_status_hidden)
    UiMessage.LastActionFailed -> stringResource(id = R.string.album_status_last_action_failed)
    UiMessage.HideInProgress -> stringResource(id = R.string.album_status_hide_in_progress)
    UiMessage.ShowInProgress -> stringResource(id = R.string.album_status_show_in_progress)
    UiMessage.DirectoryCannotBeGranted -> stringResource(id = R.string.album_failure_restricted_root)
    UiMessage.DirectoryAccessNotGranted -> stringResource(id = R.string.album_failure_grant_denied)
    UiMessage.WrongFolderSelected -> stringResource(id = R.string.album_failure_wrong_directory_selected)
    UiMessage.PersistAccessDenied -> stringResource(id = R.string.album_failure_persist_permission_denied)
    UiMessage.PreviousTaskInterrupted -> stringResource(id = R.string.album_failure_interrupted)
    UiMessage.DirectoryGrantMissing -> stringResource(id = R.string.album_failure_missing_directory_grant)
    UiMessage.UnableToCreateNomedia -> stringResource(id = R.string.album_failure_unable_to_create_nomedia)
    UiMessage.UnableToRemoveNomedia -> stringResource(id = R.string.album_failure_unable_to_remove_nomedia)
    is UiMessage.Raw -> value
}
