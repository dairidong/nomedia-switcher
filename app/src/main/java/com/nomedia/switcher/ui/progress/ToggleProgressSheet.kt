package com.nomedia.switcher.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nomedia.switcher.R
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.ui.UiMessage
import com.nomedia.switcher.ui.resolve

data class ToggleProgressSheetState(
    val albumId: AlbumId,
    val albumName: String,
    val action: ToggleAction,
    val message: UiMessage,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .testTag("toggle-progress-loading"),
                strokeWidth = 3.dp,
            )
            Text(
                text = state.message.resolve(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = stringResource(id = R.string.toggle_progress_helper),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onHide) {
            Text(text = stringResource(id = R.string.toggle_progress_hide_panel))
        }
    }
}
