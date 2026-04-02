package com.nomedia.switcher.ui.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nomedia.switcher.ui.common.AlbumCover
import com.nomedia.switcher.ui.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    state: AlbumListUiState,
    onToggleClick: (AlbumRowState) -> Unit,
    onOpenSettings: () -> Unit,
    highlightedAlbumId: com.nomedia.switcher.domain.model.AlbumId?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = { Text(text = "Albums") },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(text = "Settings")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.albums.isEmpty()) {
            EmptyAlbumState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.albums,
                    key = { it.id.directoryKey },
                ) { album ->
                    AlbumRow(
                        album = album.copy(isHighlighted = album.id == highlightedAlbumId),
                        onToggleClick = onToggleClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAlbumState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No switchable albums yet",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AlbumRow(
    album: AlbumRowState,
    onToggleClick: (AlbumRowState) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (album.isHighlighted) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(
                directoryKey = album.id.directoryKey,
                displayName = album.displayName,
                coverUri = album.coverUri,
                coverMediaKind = album.coverMediaKind,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = album.directorySummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                album.statusText?.let { status ->
                    StatusChip(
                        text = status,
                        state = album.state,
                    )
                }
                if (album.isHighlighted) {
                    StatusChip(
                        text = "Opened from notification",
                        state = album.state,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (album.showsInlineProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("album-inline-progress-${album.id.directoryKey}"),
                        strokeWidth = 2.dp,
                    )
                }
                Switch(
                    checked = album.isChecked,
                    enabled = album.isToggleEnabled,
                    onCheckedChange = { onToggleClick(album) },
                )
            }
        }
    }
}
