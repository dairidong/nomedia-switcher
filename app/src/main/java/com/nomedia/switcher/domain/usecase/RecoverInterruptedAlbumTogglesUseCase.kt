package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.toggle.NomediaDirectoryAccess
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

class RecoverInterruptedAlbumTogglesUseCase(
    private val findProcessingAlbums: suspend () -> List<AlbumRecordEntity>,
    private val unfinishedDirectoryKeys: suspend () -> Set<String>,
    private val findGrant: suspend (String) -> String?,
    private val directoryAccess: NomediaDirectoryAccess,
    private val albumStateWriter: AlbumStateWriter,
) {
    suspend operator fun invoke() {
        val activeDirectoryKeys = unfinishedDirectoryKeys()
        findProcessingAlbums()
            .filterNot { it.directoryKey in activeDirectoryKeys }
            .forEach { record ->
                recover(record)
            }
    }

    private suspend fun recover(record: AlbumRecordEntity) {
        val action = record.lastAction ?: ToggleAction.Hide
        val treeUri = findGrant(record.directoryKey)
        val nomediaExists = treeUri?.let { directoryAccess.exists(it, NOMEDIA_FILE) } ?: false
        val recoveredState = when (action) {
            ToggleAction.Hide -> if (nomediaExists) AlbumState.Hidden else AlbumState.Failed
            ToggleAction.Show -> if (nomediaExists) AlbumState.Failed else AlbumState.Shown
        }

        albumStateWriter.updateAlbum(
            directoryKey = record.directoryKey,
            displayName = record.displayName,
            state = recoveredState,
            lastAction = action,
            lastFailure = if (recoveredState == AlbumState.Failed) {
                ToggleFailureReason.Interrupted.persistedKey
            } else {
                null
            },
            treeUri = treeUri,
        )
    }

    companion object {
        private const val NOMEDIA_FILE = ".nomedia"
    }
}
