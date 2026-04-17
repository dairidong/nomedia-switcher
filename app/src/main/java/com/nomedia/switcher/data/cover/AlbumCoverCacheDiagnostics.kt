package com.nomedia.switcher.data.cover

import android.util.Log

const val COVER_CACHE_STAGE_DECODE = "decode_cover_bitmap"
const val COVER_CACHE_STAGE_LOAD_CONTENT_THUMBNAIL = "load_content_thumbnail"
const val COVER_CACHE_STAGE_LOAD_IMAGE_BITMAP = "load_image_bitmap"
const val COVER_CACHE_STAGE_LOAD_VIDEO_FRAME = "load_video_frame"
const val COVER_CACHE_STAGE_WRITE_CACHE_FILE = "write_cache_file"

data class AlbumCoverCacheAttemptFailure(
    val stage: String,
    val detail: String,
)

data class AlbumCoverCacheFailureEvent(
    val directoryKey: String,
    val sourceUri: String,
    val sourceMediaKind: String,
    val terminalStage: String,
    val attemptFailures: List<AlbumCoverCacheAttemptFailure>,
)

enum class AlbumCoverWarningCode(
    val persistedKey: String,
) {
    VideoThumbnailFailed("video_thumbnail_failed"),
    VideoFrameFailed("video_frame_failed"),
    VideoThumbnailAndFrameFailed("video_thumbnail_and_frame_failed"),
    CacheWriteFailed("cache_write_failed"),
    Generic("generic"),
    ;

    companion object {
        fun fromPersistedKey(value: String): AlbumCoverWarningCode? {
            return entries.firstOrNull { it.persistedKey == value }
        }
    }
}

data class AlbumCoverCacheStoreResult(
    val cachedCoverRef: CachedAlbumCoverRef?,
    val warningCode: AlbumCoverWarningCode? = null,
){
    companion object {
        fun succeeded(
            cachedCoverRef: CachedAlbumCoverRef?,
        ): AlbumCoverCacheStoreResult = AlbumCoverCacheStoreResult(
            cachedCoverRef = cachedCoverRef,
            warningCode = null,
        )

        fun failed(
            warningCode: AlbumCoverWarningCode,
        ): AlbumCoverCacheStoreResult = AlbumCoverCacheStoreResult(
            cachedCoverRef = null,
            warningCode = warningCode,
        )
    }
}

interface AlbumCoverCacheDiagnosticReporter {
    fun onFailure(event: AlbumCoverCacheFailureEvent)
}

object NoopAlbumCoverCacheDiagnosticReporter : AlbumCoverCacheDiagnosticReporter {
    override fun onFailure(event: AlbumCoverCacheFailureEvent) {
    }
}

class AndroidLogAlbumCoverCacheDiagnosticReporter : AlbumCoverCacheDiagnosticReporter {
    override fun onFailure(event: AlbumCoverCacheFailureEvent) {
        val attempts = event.attemptFailures.joinToString(separator = " | ") { failure ->
            "${failure.stage}: ${failure.detail}"
        }
        Log.w(
            "AlbumCoverCache",
            "Failed to cache cover for ${event.directoryKey} " +
                "(mediaKind=${event.sourceMediaKind}, terminalStage=${event.terminalStage}, " +
                "sourceUri=${event.sourceUri}, attempts=$attempts)",
        )
    }
}

fun AlbumCoverCacheFailureEvent.toWarningCode(): AlbumCoverWarningCode {
    if (terminalStage == COVER_CACHE_STAGE_WRITE_CACHE_FILE) {
        return AlbumCoverWarningCode.CacheWriteFailed
    }
    if (sourceMediaKind == "video") {
        val stages = attemptFailures.map { it.stage }.toSet()
        val thumbnailFailed = COVER_CACHE_STAGE_LOAD_CONTENT_THUMBNAIL in stages
        val frameFailed = COVER_CACHE_STAGE_LOAD_VIDEO_FRAME in stages
        return when {
            thumbnailFailed && frameFailed -> AlbumCoverWarningCode.VideoThumbnailAndFrameFailed
            frameFailed -> AlbumCoverWarningCode.VideoFrameFailed
            thumbnailFailed -> AlbumCoverWarningCode.VideoThumbnailFailed
            else -> AlbumCoverWarningCode.Generic
        }
    }
    return AlbumCoverWarningCode.Generic
}
