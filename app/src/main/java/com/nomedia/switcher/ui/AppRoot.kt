package com.nomedia.switcher.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nomedia.switcher.NoMediaApplication
import com.nomedia.switcher.domain.model.AlbumId
import com.nomedia.switcher.domain.usecase.ObserveAlbumsUseCase
import com.nomedia.switcher.domain.usecase.ResolveToggleRequestUseCase
import com.nomedia.switcher.domain.usecase.ToggleRequestResolution
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.ui.access.DirectoryGrantLauncher
import com.nomedia.switcher.ui.albums.AlbumListScreen
import com.nomedia.switcher.ui.albums.AlbumListViewModel
import com.nomedia.switcher.ui.albums.AlbumRowState
import com.nomedia.switcher.ui.progress.ToggleProgressSheet
import com.nomedia.switcher.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    highlightedAlbumId: AlbumId? = null,
) {
    val context = LocalContext.current
    val application = context.applicationContext as NoMediaApplication
    val container = application.appContainer
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val scanResults = remember(container) { MutableStateFlow(emptyList<com.nomedia.switcher.data.media.AlbumCandidate>()) }
    val scanCompleted = remember(container) { MutableStateFlow(false) }
    val resolveToggleRequest = remember(container) { ResolveToggleRequestUseCase(container.directoryGrantRepository) }
    val coroutineScope = rememberCoroutineScope()
    var hasMediaPermission by rememberSaveable { mutableStateOf(context.hasMediaPermission()) }
    var pendingGrantRequest by remember { mutableStateOf<ToggleRequestResolution.RequestGrant?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        hasMediaPermission = requiredMediaPermissions().all { permission ->
            grantResults[permission] == true || context.hasPermission(permission)
        }
    }
    val albumEntries = remember(container) {
        combine(
            container.database.albumRecordDao().observeAll(),
            container.userSettingsRepository.settings,
            scanResults,
            scanCompleted,
        ) { records, settings, scan, completed ->
            ObserveAlbumsUseCase().merge(
                records = records,
                scan = scan,
                pinHidden = settings.pinHiddenAlbumsToTop,
                scanCompleted = completed,
            )
        }
    }

    LaunchedEffect(container) {
        withContext(Dispatchers.IO) {
            container.recoverInterruptedAlbumTogglesUseCase()
        }
    }

    LaunchedEffect(hasMediaPermission) {
        if (!hasMediaPermission) {
            scanResults.value = emptyList()
            scanCompleted.value = false
            permissionLauncher.launch(requiredMediaPermissions())
            return@LaunchedEffect
        }

        val albums = withContext(Dispatchers.IO) {
            container.mediaStoreAlbumLoader.load(context.contentResolver)
        }
        scanResults.value = albums
        scanCompleted.value = true
        withContext(Dispatchers.IO) {
            container.persistScannedAlbumCoverReferencesUseCase.persist(albums)
        }
    }

    val viewModel: AlbumListViewModel = viewModel(
        factory = AlbumListViewModel.factory(
            albums = albumEntries,
            settings = container.userSettingsRepository.settings,
            enqueueToggle = container.enqueueToggleAlbumUseCase::invoke,
            setPinHiddenAlbums = container.setHiddenAlbumsPinnedUseCase::invoke,
            resolveFallbackCover = container.albumCoverFallbackResolver::resolve,
        ),
    )
    val grantLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val request = pendingGrantRequest ?: return@rememberLauncherForActivityResult
        pendingGrantRequest = null
        if (result.resultCode != Activity.RESULT_OK) {
            coroutineScope.launch {
                recordToggleFailure(
                    application = application,
                    album = request.toAlbumRowState(),
                    reason = ToggleFailureReason.GrantDenied,
                )
                viewModel.onForegroundFailure(ToggleFailureReason.GrantDenied.toUiMessage())
            }
            return@rememberLauncherForActivityResult
        }

        val treeUri = result.data?.data
        if (treeUri == null || !DirectoryGrantLauncher.matchesDirectory(treeUri, request.directoryKey)) {
            coroutineScope.launch {
                recordToggleFailure(
                    application = application,
                    album = request.toAlbumRowState(),
                    reason = ToggleFailureReason.WrongDirectorySelected,
                )
                viewModel.onForegroundFailure(ToggleFailureReason.WrongDirectorySelected.toUiMessage())
            }
            return@rememberLauncherForActivityResult
        }

        val grantFlags = (result.data?.flags ?: 0) and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                grantFlags.takeIf { it != 0 }
                    ?: (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            )
        } catch (_: SecurityException) {
            coroutineScope.launch {
                recordToggleFailure(
                    application = application,
                    album = request.toAlbumRowState(),
                    reason = ToggleFailureReason.PersistPermissionDenied,
                )
                viewModel.onForegroundFailure(ToggleFailureReason.PersistPermissionDenied.toUiMessage())
            }
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            container.directoryGrantRepository.saveGrant(
                directoryKey = request.directoryKey,
                treeUri = treeUri.toString(),
            )
            viewModel.onToggleClick(request.toAlbumRowState())
        }
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel, context) {
        viewModel.transientMessages.collectLatest { message ->
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
        }
    }

    if (showSettings) {
        SettingsScreen(
            pinHiddenAlbumsToTop = state.pinHiddenAlbumsToTop,
            onPinHiddenChanged = viewModel::onPinHiddenChanged,
            onBack = { showSettings = false },
        )
    } else {
        AlbumListScreen(
            state = state,
            onToggleClick = { album ->
                coroutineScope.launch {
                    when (val resolution = resolveToggleRequest(
                        directoryKey = album.id.directoryKey,
                        albumName = album.displayName,
                        action = album.nextAction ?: return@launch,
                    )) {
                        is ToggleRequestResolution.Enqueue -> viewModel.onToggleClick(album)
                        is ToggleRequestResolution.RequestGrant -> {
                            pendingGrantRequest = resolution
                            grantLauncher.launch(
                                DirectoryGrantLauncher.createIntent(resolution.initialUri),
                            )
                        }
                        is ToggleRequestResolution.Blocked -> recordToggleFailure(
                            application = application,
                            album = album,
                            reason = resolution.reason,
                        ).also {
                            viewModel.onForegroundFailure(resolution.reason.toUiMessage())
                        }
                    }
                }
            },
            onOpenSettings = { showSettings = true },
            highlightedAlbumId = highlightedAlbumId,
        )
    }

    state.progressSheet?.let { progressSheet ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissProgressSheet,
        ) {
            ToggleProgressSheet(
                state = progressSheet,
                onHide = viewModel::dismissProgressSheet,
            )
        }
    }
}

private suspend fun recordToggleFailure(
    application: NoMediaApplication,
    album: AlbumRowState,
    reason: ToggleFailureReason,
) {
    val action = album.nextAction ?: return
    application.appContainer.albumStateWriter.updateAlbum(
        directoryKey = album.id.directoryKey,
        displayName = album.displayName,
        state = AlbumState.Failed,
        lastAction = action,
        lastFailure = reason.persistedKey,
        treeUri = null,
    )
}

private fun ToggleRequestResolution.RequestGrant.toAlbumRowState(): AlbumRowState {
    return AlbumRowState(
        id = AlbumId(directoryKey),
        displayName = albumName,
        directorySummary = directoryKey,
        state = AlbumState.Shown,
        isChecked = action == com.nomedia.switcher.domain.model.ToggleAction.Show,
        isToggleEnabled = true,
        nextAction = action,
    )
}

private fun requiredMediaPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun android.content.Context.hasMediaPermission(): Boolean {
    return requiredMediaPermissions().all(::hasPermission)
}

private fun android.content.Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
