package com.nomedia.switcher.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumCoverSourceSpecTest {
    @Test
    fun video_cover_source_prefers_platform_thumbnail_loading() {
        assertEquals(
            AlbumCoverSourceSpec.PlatformThumbnail(
                uri = "content://media/external/video/media/5",
                targetSizePx = 128,
            ),
            buildAlbumCoverSourceSpec(
                coverUri = "content://media/external/video/media/5",
                coverMediaKind = "video",
                targetSizePx = 128,
            ),
        )
    }

    @Test
    fun image_cover_source_keeps_coil_request_loading() {
        assertEquals(
            AlbumCoverSourceSpec.CoilRequest(
                request = AlbumCoverRequestSpec(
                    data = "content://media/external/images/media/9",
                    useVideoFrame = false,
                    targetSizePx = 128,
                    videoFrameMillis = 0L,
                ),
            ),
            buildAlbumCoverSourceSpec(
                coverUri = "content://media/external/images/media/9",
                coverMediaKind = "image",
                targetSizePx = 128,
            ),
        )
    }

    @Test
    fun document_video_cover_source_uses_asset_file_descriptor_video_frame_loading() {
        assertEquals(
            AlbumCoverSourceSpec.VideoFrameAssetFileDescriptor(
                uri = "content://com.android.externalstorage.documents/document/primary%3AMovies%2FTrips%2FVID_0007.mp4",
                targetSizePx = 128,
            ),
            buildAlbumCoverSourceSpec(
                coverUri = "content://com.android.externalstorage.documents/document/primary%3AMovies%2FTrips%2FVID_0007.mp4",
                coverMediaKind = "video",
                targetSizePx = 128,
            ),
        )
    }
}
