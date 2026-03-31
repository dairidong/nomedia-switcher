package com.nomedia.switcher.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomedia.switcher.domain.model.AlbumState

@Composable
fun StatusChip(
    text: String,
    state: AlbumState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when (state) {
        AlbumState.Hidden, AlbumState.HiddenMissingFromScan -> colors.secondaryContainer
        AlbumState.Processing -> colors.tertiaryContainer
        AlbumState.Failed -> colors.errorContainer
        AlbumState.Shown -> colors.surfaceVariant
    }
    val contentColor = when (state) {
        AlbumState.Failed -> colors.onErrorContainer
        AlbumState.Processing -> colors.onTertiaryContainer
        AlbumState.Hidden, AlbumState.HiddenMissingFromScan -> colors.onSecondaryContainer
        AlbumState.Shown -> colors.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
