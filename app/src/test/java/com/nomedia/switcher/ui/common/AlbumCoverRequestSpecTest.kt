package com.nomedia.switcher.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumCoverRequestSpecTest {
    @Test
    fun video_cover_request_uses_non_zero_video_frame_and_small_size() {
        val spec = buildAlbumCoverRequestSpec(
            coverUri = "content://media/external/video/media/5",
            coverMediaKind = "video",
            targetSizePx = 128,
        )

        assertEquals("content://media/external/video/media/5", spec.data)
        assertEquals(true, spec.useVideoFrame)
        assertEquals(128, spec.targetSizePx)
        assertEquals(1_000L, spec.videoFrameMillis)
    }

    @Test
    fun image_cover_request_skips_video_frame_mode() {
        val spec = buildAlbumCoverRequestSpec(
            coverUri = "content://media/external/images/media/9",
            coverMediaKind = "image",
            targetSizePx = 128,
        )

        assertEquals(false, spec.useVideoFrame)
    }
}
