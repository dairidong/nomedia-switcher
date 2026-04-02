package com.nomedia.switcher.ui.albums

import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

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
    val statusText: String? = null,
    val showsInlineProgress: Boolean = false,
    val isHighlighted: Boolean = false,
)
