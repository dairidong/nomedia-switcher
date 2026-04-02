package com.nomedia.switcher.domain.model

data class AlbumEntry(
    val id: AlbumId,
    val displayName: String,
    val state: AlbumState,
    val treeUri: String? = null,
    val coverUri: String? = null,
    val coverRelativeFilePath: String? = null,
    val coverMediaKind: String? = null,
    val lastAction: ToggleAction? = null,
    val lastFailure: String? = null,
    val seenInLastScan: Boolean = true,
    val updatedAtEpochMs: Long = 0L,
)
