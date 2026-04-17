package com.nomedia.switcher.data.cover

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    sdk = [34],
    application = Application::class,
)
class AlbumCoverCacheStoreTest {
    @Test
    fun createOrUpdate_writes_thumbnail_file_and_returns_persistable_reference() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = File(context.cacheDir, "album-cover-source.jpg")
        writeBitmap(sourceFile)
        val store = AlbumCoverCacheStore(
            context = context,
            currentTimeProvider = { 123L },
        )

        val cached = store.createOrUpdate(
            directoryKey = "Movies/Trips",
            sourceUri = Uri.fromFile(sourceFile).toString(),
            sourceMediaKind = "image",
        )

        assertNotNull(cached)
        assertEquals("image", cached?.mediaKind)
        assertEquals(123L, cached?.updatedAtEpochMs)
        assertTrue(File(cached!!.absolutePath).exists())
        assertTrue(cached.absolutePath.contains("album-cover-cache"))
    }

    @Test
    fun createOrUpdate_reports_video_decode_attempts_when_all_paths_fail() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reporter = FakeAlbumCoverCacheDiagnosticReporter()
        val store = AlbumCoverCacheStore(
            context = context,
            diagnosticReporter = reporter,
        )

        val cached = store.createOrUpdate(
            directoryKey = "Movies/Trips",
            sourceUri = "content://missing.provider/video/1",
            sourceMediaKind = "video",
        )

        assertNull(cached)
        assertEquals(1, reporter.failures.size)
        assertEquals(COVER_CACHE_STAGE_DECODE, reporter.failures.single().terminalStage)
        assertEquals(
            listOf(
                COVER_CACHE_STAGE_LOAD_CONTENT_THUMBNAIL,
                COVER_CACHE_STAGE_LOAD_VIDEO_FRAME,
            ),
            reporter.failures.single().attemptFailures.map { it.stage }.distinct(),
        )
    }

    private fun writeBitmap(destination: File) {
        val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.CYAN)
        FileOutputStream(destination).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
    }

    private class FakeAlbumCoverCacheDiagnosticReporter : AlbumCoverCacheDiagnosticReporter {
        val failures = mutableListOf<AlbumCoverCacheFailureEvent>()

        override fun onFailure(event: AlbumCoverCacheFailureEvent) {
            failures += event
        }
    }
}
