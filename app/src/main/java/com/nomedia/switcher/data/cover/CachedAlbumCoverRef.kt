package com.nomedia.switcher.data.cover

data class CachedAlbumCoverRef(
    val absolutePath: String,
    val mediaKind: String,
    val updatedAtEpochMs: Long,
)
