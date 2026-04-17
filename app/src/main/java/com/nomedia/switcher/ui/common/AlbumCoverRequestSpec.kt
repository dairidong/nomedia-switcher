package com.nomedia.switcher.ui.common

private const val VIDEO_COVER_FRAME_MILLIS = 100L

sealed interface AlbumCoverSourceSpec {
    data class CoilRequest(
        val request: AlbumCoverRequestSpec,
    ) : AlbumCoverSourceSpec

    data class PlatformThumbnail(
        val uri: String,
        val targetSizePx: Int,
    ) : AlbumCoverSourceSpec

    data class VideoFrameAssetFileDescriptor(
        val uri: String,
        val targetSizePx: Int,
    ) : AlbumCoverSourceSpec
}

data class AlbumCoverRequestSpec(
    val data: String,
    val useVideoFrame: Boolean,
    val targetSizePx: Int,
    val videoFrameMillis: Long,
)

fun buildAlbumCoverSourceSpec(
    coverUri: String,
    coverMediaKind: String?,
    targetSizePx: Int,
): AlbumCoverSourceSpec {
    return if (coverMediaKind == "video" && coverUri.startsWith("content://media/")) {
        AlbumCoverSourceSpec.PlatformThumbnail(
            uri = coverUri,
            targetSizePx = targetSizePx,
        )
    } else if (coverMediaKind == "video") {
        AlbumCoverSourceSpec.VideoFrameAssetFileDescriptor(
            uri = coverUri,
            targetSizePx = targetSizePx,
        )
    } else {
        AlbumCoverSourceSpec.CoilRequest(
            request = buildAlbumCoverRequestSpec(
                coverUri = coverUri,
                coverMediaKind = coverMediaKind,
                targetSizePx = targetSizePx,
            ),
        )
    }
}

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
