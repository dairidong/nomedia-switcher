package com.nomedia.switcher.ui.albums

import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.UiMessage

data class AlbumRowState(
    val id: AlbumId,
    val displayName: String,
    val directorySummary: String,
    val state: AlbumState,
    val coverUri: String? = null,
    val coverMediaKind: String? = null,
    val isChecked: Boolean,
    val isToggleEnabled: Boolean,
    val nextAction: ToggleAction?,
    val statusMessage: UiMessage? = null,
    val showsInlineProgress: Boolean = false,
    val isHighlighted: Boolean = false,
)
