package com.nomedia.switcher.ui.common

private const val VIDEO_COVER_FRAME_MILLIS = 100L

data class AlbumCoverRequestSpec(
    val data: String,
    val useVideoFrame: Boolean,
    val targetSizePx: Int,
    val videoFrameMillis: Long,
)

fun buildAlbumCoverRequestSpec(
    coverUri: String,
    coverMediaKind: String?,
    targetSizePx: Int,
): AlbumCoverRequestSpec {
    val useVideoFrame = coverMediaKind == "video"
    return AlbumCoverRequestSpec(
        data = coverUri,
        useVideoFrame = useVideoFrame,
        targetSizePx = targetSizePx,
        videoFrameMillis = if (useVideoFrame) VIDEO_COVER_FRAME_MILLIS else 0L,
    )
}
