package com.nomedia.switcher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis

@Composable
fun AlbumCover(
    directoryKey: String,
    displayName: String,
    coverUri: String?,
    coverMediaKind: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    val coverModifier = modifier
        .size(64.dp)
        .clip(shape)

    if (coverUri.isNullOrBlank()) {
        Box(
            modifier = coverModifier
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
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(coverUri)
            .apply {
                if (coverMediaKind == "video") {
                    videoFrameMillis(0)
                }
            }
            .build(),
        contentDescription = null,
        modifier = coverModifier.testTag("album-cover-image-$directoryKey"),
        contentScale = ContentScale.Crop,
    )
}
