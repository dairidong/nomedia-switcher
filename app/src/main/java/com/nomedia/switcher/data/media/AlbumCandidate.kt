package com.nomedia.switcher.data.media

data class AlbumCandidate(
    val bucketId: String,
    val bucketName: String,
    val directoryKey: String,
    val volumeName: String? = null,
)
