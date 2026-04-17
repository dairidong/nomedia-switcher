package com.nomedia.switcher.data.media

data class MediaStoreAlbumRow(
    val mediaId: Long,
    val bucketId: String?,
    val bucketName: String?,
    val relativePath: String?,
    val dataPath: String?,
    val volumeName: String?,
    val displayName: String?,
    val albumRelativeFilePath: String?,
    val mediaKind: String,
    val dateModifiedEpochMs: Long? = null,
)
