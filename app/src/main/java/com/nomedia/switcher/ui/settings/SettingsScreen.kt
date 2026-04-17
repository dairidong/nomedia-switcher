package com.nomedia.switcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nomedia.switcher.R
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.ui.theme.TopBarContainer
import com.nomedia.switcher.ui.theme.TopBarContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pinHiddenAlbumsToTop: Boolean,
    albumSortMode: AlbumSortMode,
    onPinHiddenChanged: (Boolean) -> Unit,
    onAlbumSortModeChanged: (AlbumSortMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var showSortModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(
                color = TopBarContainer,
                shadowElevation = 6.dp,
            ) {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = TopBarContent,
                        titleContentColor = TopBarContent,
                        actionIconContentColor = TopBarContent,
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_24),
                                contentDescription = stringResource(id = R.string.settings_back),
                            )
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_pin_hidden_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.settings_pin_hidden_helper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = pinHiddenAlbumsToTop,
                    onCheckedChange = onPinHiddenChanged,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSortModeDialog = true }
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_sort_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(id = R.string.settings_sort_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        id = R.string.settings_sort_current_value,
                        albumSortMode.label(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showSortModeDialog) {
        AlertDialog(
            onDismissRequest = { showSortModeDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_sort_dialog_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortModeRow(
                        title = stringResource(id = R.string.settings_sort_by_name),
                        selected = albumSortMode == AlbumSortMode.ByName,
                        onClick = {
                            showSortModeDialog = false
                            onAlbumSortModeChanged(AlbumSortMode.ByName)
                        },
                    )
                    SortModeRow(
                        title = stringResource(id = R.string.settings_sort_by_latest_media),
                        selected = albumSortMode == AlbumSortMode.ByLatestMedia,
                        onClick = {
                            showSortModeDialog = false
                            onAlbumSortModeChanged(AlbumSortMode.ByLatestMedia)
                        },
                    )
                }
            },
            confirmButton = {
                Text(
                    text = stringResource(id = android.R.string.cancel),
                    modifier = Modifier
                        .clickable { showSortModeDialog = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        )
    }
}

@Composable
private fun SortModeRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AlbumSortMode.label(): String {
    return when (this) {
        AlbumSortMode.ByName -> stringResource(id = R.string.settings_sort_by_name)
        AlbumSortMode.ByLatestMedia -> stringResource(id = R.string.settings_sort_by_latest_media)
    }
}
