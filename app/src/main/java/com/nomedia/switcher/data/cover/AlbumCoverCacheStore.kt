package com.nomedia.switcher.data.cover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CACHE_DIRECTORY_NAME = "album-cover-cache"
private const val CACHE_SIZE_PX = 256
private const val CACHE_QUALITY = 82
private const val VIDEO_FRAME_MICROS = 100_000L

open class AlbumCoverCacheStore(
    private val context: Context? = null,
    private val currentTimeProvider: () -> Long = System::currentTimeMillis,
    private val diagnosticReporter: AlbumCoverCacheDiagnosticReporter = NoopAlbumCoverCacheDiagnosticReporter,
    private val cacheDirectoryFactory: (Context) -> File = { appContext ->
        File(appContext.filesDir, CACHE_DIRECTORY_NAME)
    },
) {
    open suspend fun createOrUpdate(
        directoryKey: String,
        sourceUri: String,
        sourceMediaKind: String,
    ): CachedAlbumCoverRef? {
        return createOrUpdateDetailed(
            directoryKey = directoryKey,
            sourceUri = sourceUri,
            sourceMediaKind = sourceMediaKind,
        ).cachedCoverRef
    }

    open suspend fun createOrUpdateDetailed(
        directoryKey: String,
        sourceUri: String,
        sourceMediaKind: String,
    ): AlbumCoverCacheStoreResult {
        val appContext = context?.applicationContext
            ?: return AlbumCoverCacheStoreResult.succeeded(cachedCoverRef = null)
        return withContext(Dispatchers.IO) {
            val attemptFailures = mutableListOf<AlbumCoverCacheAttemptFailure>()
            val bitmap = loadBitmap(
                directoryKey = directoryKey,
                context = appContext,
                sourceUri = sourceUri,
                sourceMediaKind = sourceMediaKind,
                attemptFailures = attemptFailures,
            ) ?: return@withContext AlbumCoverCacheStoreResult.failed(
                warningCode = AlbumCoverCacheFailureEvent(
                    directoryKey = directoryKey,
                    sourceUri = sourceUri,
                    sourceMediaKind = sourceMediaKind,
                    terminalStage = COVER_CACHE_STAGE_DECODE,
                    attemptFailures = attemptFailures,
                ).toWarningCode(),
            )
            try {
                val cacheDirectory = cacheDirectoryFactory(appContext).apply {
                    mkdirs()
                }
                val cacheFile = File(cacheDirectory, buildCacheFileName(directoryKey))
                val writeSucceeded = runCatching {
                    FileOutputStream(cacheFile).use { output ->
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, CACHE_QUALITY, output)
                    }
                }.getOrElse { throwable ->
                    reportFailure(
                        directoryKey = directoryKey,
                        sourceUri = sourceUri,
                        sourceMediaKind = sourceMediaKind,
                        terminalStage = COVER_CACHE_STAGE_WRITE_CACHE_FILE,
                        attemptFailures = attemptFailures + throwable.toAttemptFailure(COVER_CACHE_STAGE_WRITE_CACHE_FILE),
                    )
                    return@withContext AlbumCoverCacheStoreResult.failed(
                        warningCode = AlbumCoverWarningCode.CacheWriteFailed,
                    )
                }
                if (!writeSucceeded) {
                    reportFailure(
                        directoryKey = directoryKey,
                        sourceUri = sourceUri,
                        sourceMediaKind = sourceMediaKind,
                        terminalStage = COVER_CACHE_STAGE_WRITE_CACHE_FILE,
                        attemptFailures = attemptFailures + AlbumCoverCacheAttemptFailure(
                            stage = COVER_CACHE_STAGE_WRITE_CACHE_FILE,
                            detail = "Bitmap.compress returned false",
                        ),
                    )
                    return@withContext AlbumCoverCacheStoreResult.failed(
                        warningCode = AlbumCoverWarningCode.CacheWriteFailed,
                    )
                }
                AlbumCoverCacheStoreResult.succeeded(
                    cachedCoverRef = CachedAlbumCoverRef(
                        absolutePath = cacheFile.absolutePath,
                        mediaKind = "image",
                        updatedAtEpochMs = currentTimeProvider(),
                    ),
                )
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun loadBitmap(
        directoryKey: String,
        context: Context,
        sourceUri: String,
        sourceMediaKind: String,
        attemptFailures: MutableList<AlbumCoverCacheAttemptFailure>,
    ): Bitmap? {
        val uri = Uri.parse(sourceUri)
        val bitmap = when (sourceMediaKind) {
            "video" -> {
                loadContentThumbnail(context, uri, attemptFailures) ?:
                    loadVideoFrame(context, uri, attemptFailures)
            }
            "image" -> {
                loadContentThumbnail(context, uri, attemptFailures) ?:
                    loadImageBitmap(context, uri, attemptFailures)
            }
            else -> null
        }?.toSquareThumbnail()

        if (bitmap == null) {
            reportFailure(
                directoryKey = directoryKey,
                sourceUri = sourceUri,
                sourceMediaKind = sourceMediaKind,
                terminalStage = COVER_CACHE_STAGE_DECODE,
                attemptFailures = attemptFailures,
            )
        }
        return bitmap
    }

    private fun loadContentThumbnail(
        context: Context,
        uri: Uri,
        attemptFailures: MutableList<AlbumCoverCacheAttemptFailure>,
    ): Bitmap? {
        if (uri.scheme != "content") {
            return null
        }
        return runCatching {
            context.contentResolver.loadThumbnail(
                uri,
                Size(CACHE_SIZE_PX, CACHE_SIZE_PX),
                null,
            )
        }.getOrElse { throwable ->
            attemptFailures += throwable.toAttemptFailure(COVER_CACHE_STAGE_LOAD_CONTENT_THUMBNAIL)
            null
        }
    }

    private fun loadImageBitmap(
        context: Context,
        uri: Uri,
        attemptFailures: MutableList<AlbumCoverCacheAttemptFailure>,
    ): Bitmap? {
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrElse { throwable ->
            attemptFailures += throwable.toAttemptFailure(COVER_CACHE_STAGE_LOAD_IMAGE_BITMAP)
            null
        }
        if (bitmap == null) {
            attemptFailures += AlbumCoverCacheAttemptFailure(
                stage = COVER_CACHE_STAGE_LOAD_IMAGE_BITMAP,
                detail = "Returned null bitmap",
            )
        }
        return bitmap
    }

    private fun loadVideoFrame(
        context: Context,
        uri: Uri,
        attemptFailures: MutableList<AlbumCoverCacheAttemptFailure>,
    ): Bitmap? {
        val bitmap = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.getScaledFrameAtTime(
                    VIDEO_FRAME_MICROS,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    CACHE_SIZE_PX,
                    CACHE_SIZE_PX,
                )
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrElse { throwable ->
            attemptFailures += throwable.toAttemptFailure(COVER_CACHE_STAGE_LOAD_VIDEO_FRAME)
            null
        }
        if (bitmap == null) {
            attemptFailures += AlbumCoverCacheAttemptFailure(
                stage = COVER_CACHE_STAGE_LOAD_VIDEO_FRAME,
                detail = "Returned null bitmap",
            )
        }
        return bitmap
    }

    private fun Bitmap.toSquareThumbnail(): Bitmap {
        val thumbnail = ThumbnailUtils.extractThumbnail(this, CACHE_SIZE_PX, CACHE_SIZE_PX)
        if (thumbnail !== this) {
            recycle()
        }
        return thumbnail
    }

    private fun buildCacheFileName(directoryKey: String): String {
        val safeKey = directoryKey.map { character ->
            if (character.isLetterOrDigit()) character.lowercaseChar() else '_'
        }.joinToString("")
        return "$safeKey.webp"
    }

    private fun reportFailure(
        directoryKey: String,
        sourceUri: String,
        sourceMediaKind: String,
        terminalStage: String,
        attemptFailures: List<AlbumCoverCacheAttemptFailure>,
    ) {
        diagnosticReporter.onFailure(
            AlbumCoverCacheFailureEvent(
                directoryKey = directoryKey,
                sourceUri = sourceUri,
                sourceMediaKind = sourceMediaKind,
                terminalStage = terminalStage,
                attemptFailures = attemptFailures,
            ),
        )
    }
}

private fun Throwable.toAttemptFailure(stage: String): AlbumCoverCacheAttemptFailure {
    val detail = buildString {
        append(this@toAttemptFailure::class.java.simpleName)
        this@toAttemptFailure.message?.takeIf { it.isNotBlank() }?.let { message ->
            append(": ")
            append(message)
        }
    }
    return AlbumCoverCacheAttemptFailure(
        stage = stage,
        detail = detail,
    )
}
