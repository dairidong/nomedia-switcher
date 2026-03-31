package com.nomedia.switcher.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.ToggleAction

data class ToggleProgressSheetState(
    val albumId: AlbumId,
    val albumName: String,
    val action: ToggleAction,
    val message: String,
)

@Composable
fun ToggleProgressSheet(
    state: ToggleProgressSheetState,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.albumName,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Large albums can take a while. You can hide this panel and let the task continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onHide) {
            Text(text = "Hide panel")
        }
    }
}
