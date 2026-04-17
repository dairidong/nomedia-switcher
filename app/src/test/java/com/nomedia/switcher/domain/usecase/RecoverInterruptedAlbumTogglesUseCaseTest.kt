package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.toggle.NomediaDirectoryAccess
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoverInterruptedAlbumTogglesUseCaseTest {
    @Test
    fun restores_hidden_state_when_no_work_is_running_and_nomedia_exists() = runTest {
        val albumStateWriter = FakeAlbumStateWriter()
        val useCase = RecoverInterruptedAlbumTogglesUseCase(
            findProcessingAlbums = {
                listOf(
                    AlbumRecordEntity(
                        directoryKey = "Pictures/Screenshots",
                        displayName = "Screenshots",
                        state = AlbumState.Processing,
                        treeUri = null,
                        lastAction = ToggleAction.Hide,
                        lastFailure = null,
                        seenInLastScan = true,
                        updatedAtEpochMs = 1L,
                    ),
                )
            },
            unfinishedDirectoryKeys = { emptySet() },
            findGrant = { "content://tree/screenshots" },
            directoryAccess = FakeNomediaDirectoryAccess(
                existingFiles = mapOf(
                    "content://tree/screenshots" to true,
                ),
            ),
            albumStateWriter = albumStateWriter,
        )

        useCase()

        assertEquals(
            listOf(
                RecoveredAlbumWrite(
                    directoryKey = "Pictures/Screenshots",
                    displayName = "Screenshots",
                    state = AlbumState.Hidden,
                    lastAction = ToggleAction.Hide,
                    lastFailure = null,
                    treeUri = "content://tree/screenshots",
                ),
            ),
            albumStateWriter.writes,
        )
    }

    @Test
    fun marks_album_failed_when_processing_record_has_no_unfinished_work_and_nomedia_is_missing() = runTest {
        val albumStateWriter = FakeAlbumStateWriter()
        val useCase = RecoverInterruptedAlbumTogglesUseCase(
            findProcessingAlbums = {
                listOf(
                    AlbumRecordEntity(
                        directoryKey = "Pictures/Cyberpunk 2077",
                        displayName = "Cyberpunk 2077",
                        state = AlbumState.Processing,
                        treeUri = null,
                        lastAction = ToggleAction.Hide,
                        lastFailure = null,
                        seenInLastScan = true,
                        updatedAtEpochMs = 1L,
                    ),
                )
            },
            unfinishedDirectoryKeys = { emptySet() },
            findGrant = { "content://tree/cyberpunk" },
            directoryAccess = FakeNomediaDirectoryAccess(
                existingFiles = mapOf(
                    "content://tree/cyberpunk" to false,
                ),
            ),
            albumStateWriter = albumStateWriter,
        )

        useCase()

        assertEquals(
            listOf(
                RecoveredAlbumWrite(
                    directoryKey = "Pictures/Cyberpunk 2077",
                    displayName = "Cyberpunk 2077",
                    state = AlbumState.Failed,
                    lastAction = ToggleAction.Hide,
                    lastFailure = ToggleFailureReason.Interrupted.persistedKey,
                    treeUri = "content://tree/cyberpunk",
                ),
            ),
            albumStateWriter.writes,
        )
    }

    @Test
    fun keeps_processing_state_when_matching_work_is_still_unfinished() = runTest {
        val albumStateWriter = FakeAlbumStateWriter()
        val useCase = RecoverInterruptedAlbumTogglesUseCase(
            findProcessingAlbums = {
                listOf(
                    AlbumRecordEntity(
                        directoryKey = "Pictures/lovewallpaper",
                        displayName = "lovewallpaper",
                        state = AlbumState.Processing,
                        treeUri = null,
                        lastAction = ToggleAction.Hide,
                        lastFailure = null,
                        seenInLastScan = true,
                        updatedAtEpochMs = 1L,
                    ),
                )
            },
            unfinishedDirectoryKeys = { setOf("Pictures/lovewallpaper") },
            findGrant = { "content://tree/lovewallpaper" },
            directoryAccess = FakeNomediaDirectoryAccess(),
            albumStateWriter = albumStateWriter,
        )

        useCase()

        assertEquals(emptyList<RecoveredAlbumWrite>(), albumStateWriter.writes)
    }

    private class FakeNomediaDirectoryAccess(
        private val existingFiles: Map<String, Boolean> = emptyMap(),
    ) : NomediaDirectoryAccess {
        override suspend fun exists(
            treeUri: String,
            fileName: String,
        ): Boolean = existingFiles[treeUri] ?: false

        override suspend fun createFile(
            treeUri: String,
            fileName: String,
        ): Boolean = false

        override suspend fun deleteFile(
            treeUri: String,
            fileName: String,
        ): Boolean = false
    }

    private class FakeAlbumStateWriter : AlbumStateWriter {
        val writes = mutableListOf<RecoveredAlbumWrite>()

        override suspend fun updateAlbum(
            directoryKey: String,
            displayName: String,
            state: AlbumState,
            lastAction: ToggleAction,
            lastFailure: String?,
            treeUri: String?,
        ) {
            writes += RecoveredAlbumWrite(
                directoryKey = directoryKey,
                displayName = displayName,
                state = state,
                lastAction = lastAction,
                lastFailure = lastFailure,
                treeUri = treeUri,
            )
        }

        override suspend fun updateCachedCover(
            directoryKey: String,
            cachedCoverPath: String,
            cachedCoverMediaKind: String,
            cachedCoverUpdatedAtEpochMs: Long,
        ) {
        }
    }
}

private data class RecoveredAlbumWrite(
    val directoryKey: String,
    val displayName: String,
    val state: AlbumState,
    val lastAction: ToggleAction,
    val lastFailure: String?,
    val treeUri: String?,
)
