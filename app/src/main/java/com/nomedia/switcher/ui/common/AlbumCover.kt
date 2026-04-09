package com.nomedia.switcher.ui.common

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumCover(
    directoryKey: String,
    displayName: String,
    coverUri: String?,
    coverMediaKind: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)
    val coverModifier = modifier
        .size(64.dp)
        .clip(shape)
    val requestSizePx = with(density) { 64.dp.roundToPx() }

    if (coverUri.isNullOrBlank()) {
        AlbumCoverPlaceholder(
            directoryKey = directoryKey,
            displayName = displayName,
            modifier = coverModifier,
        )
        return
    }

    val sourceSpec = remember(context, coverUri, coverMediaKind, requestSizePx) {
        buildAlbumCoverSourceSpec(
            coverUri = coverUri,
            coverMediaKind = coverMediaKind,
            targetSizePx = requestSizePx,
        )
    }

    if (sourceSpec is AlbumCoverSourceSpec.PlatformThumbnail) {
        PlatformThumbnailCover(
            directoryKey = directoryKey,
            displayName = displayName,
            source = sourceSpec,
            modifier = coverModifier,
        )
        return
    }

    val model = remember(sourceSpec, context) {
        val requestSpec = (sourceSpec as AlbumCoverSourceSpec.CoilRequest).request
        ImageRequest.Builder(context)
            .data(requestSpec.data)
            .size(requestSpec.targetSizePx, requestSpec.targetSizePx)
            .apply {
                if (requestSpec.useVideoFrame) {
                    videoFrameMillis(requestSpec.videoFrameMillis)
                }
            }
            .build()
    }
    val painter = rememberAsyncImagePainter(model = model)

    if (painter.state is AsyncImagePainter.State.Error) {
        AlbumCoverPlaceholder(
            directoryKey = directoryKey,
            displayName = displayName,
            modifier = coverModifier,
        )
        return
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = coverModifier.testTag("album-cover-image-$directoryKey"),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PlatformThumbnailCover(
    directoryKey: String,
    displayName: String,
    source: AlbumCoverSourceSpec.PlatformThumbnail,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val thumbnailState by produceState<PlatformThumbnailState>(
        initialValue = PlatformThumbnailState.Loading,
        key1 = context,
        key2 = source,
    ) {
        value = loadPlatformThumbnail(
            context = context,
            uri = source.uri,
            targetSizePx = source.targetSizePx,
        )?.let(PlatformThumbnailState::Loaded) ?: PlatformThumbnailState.Error
    }

    when (val state = thumbnailState) {
        PlatformThumbnailState.Loading -> {
            AlbumCoverPlaceholder(
                directoryKey = directoryKey,
                displayName = displayName,
                modifier = modifier,
            )
        }

        PlatformThumbnailState.Error -> {
            CoilVideoFallbackCover(
                directoryKey = directoryKey,
                displayName = displayName,
                source = source,
                modifier = modifier,
            )
        }

        is PlatformThumbnailState.Loaded -> {
            Image(
                bitmap = state.bitmap,
                contentDescription = null,
                modifier = modifier.testTag("album-cover-image-$directoryKey"),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CoilVideoFallbackCover(
    directoryKey: String,
    displayName: String,
    source: AlbumCoverSourceSpec.PlatformThumbnail,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val model = remember(context, source) {
        val requestSpec = buildAlbumCoverRequestSpec(
            coverUri = source.uri,
            coverMediaKind = "video",
            targetSizePx = source.targetSizePx,
        )
        ImageRequest.Builder(context)
            .data(requestSpec.data)
            .size(requestSpec.targetSizePx, requestSpec.targetSizePx)
            .videoFrameMillis(requestSpec.videoFrameMillis)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = model)

    if (painter.state is AsyncImagePainter.State.Error) {
        AlbumCoverPlaceholder(
            directoryKey = directoryKey,
            displayName = displayName,
            modifier = modifier,
        )
        return
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier.testTag("album-cover-image-$directoryKey"),
        contentScale = ContentScale.Crop,
    )
}

private suspend fun loadPlatformThumbnail(
    context: android.content.Context,
    uri: String,
    targetSizePx: Int,
): ImageBitmap? {
    return runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.loadThumbnail(
                Uri.parse(uri),
                Size(targetSizePx, targetSizePx),
                null,
            ).asImageBitmap()
        }
    }.getOrNull()
}

private sealed interface PlatformThumbnailState {
    data object Loading : PlatformThumbnailState

    data object Error : PlatformThumbnailState

    data class Loaded(
        val bitmap: ImageBitmap,
    ) : PlatformThumbnailState
}

@Composable
private fun AlbumCoverPlaceholder(
    directoryKey: String,
    displayName: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .testTag("album-cover-placeholder-$directoryKey"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.trim().take(1).ifBlank { "#" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}
