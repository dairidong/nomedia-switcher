package com.nomedia.switcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nomedia.switcher.R

sealed interface UiMessage {
    data object AlbumHidden : UiMessage
    data object LastActionFailed : UiMessage
    data object HideInProgress : UiMessage
    data object ShowInProgress : UiMessage
    data class Raw(val value: String) : UiMessage
}

@Composable
fun UiMessage.resolve(): String = when (this) {
    UiMessage.AlbumHidden -> stringResource(id = R.string.album_status_hidden)
    UiMessage.LastActionFailed -> stringResource(id = R.string.album_status_last_action_failed)
    UiMessage.HideInProgress -> stringResource(id = R.string.album_status_hide_in_progress)
    UiMessage.ShowInProgress -> stringResource(id = R.string.album_status_show_in_progress)
    is UiMessage.Raw -> value
}
