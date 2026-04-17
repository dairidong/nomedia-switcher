package com.nomedia.switcher.ui

import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.R
import com.nomedia.switcher.data.cover.AlbumCoverWarningCode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiMessageResolverTest {
    @Test
    fun resolve_with_context_returns_localized_failure_text() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        assertEquals(
            context.getString(R.string.album_failure_unable_to_create_nomedia),
            UiMessage.UnableToCreateNomedia.resolve(context),
        )
    }

    @Test
    fun resolve_with_context_returns_raw_value_for_raw_message() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        assertEquals(
            "custom failure",
            UiMessage.Raw("custom failure").resolve(context),
        )
    }

    @Test
    fun cover_cache_warning_code_resolves_to_localized_message() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        assertEquals(
            context.getString(R.string.album_warning_video_thumbnail_and_frame_failed),
            AlbumCoverWarningCode.VideoThumbnailAndFrameFailed.toUiMessage().resolve(context),
        )
    }
}
