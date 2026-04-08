package com.nomedia.switcher.domain.usecase

import android.net.Uri
import com.nomedia.switcher.data.access.DirectoryGrantRepository
import com.nomedia.switcher.data.access.GrantError
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleFailureReason
import com.nomedia.switcher.ui.access.DirectoryGrantLauncher

sealed interface ToggleRequestResolution {
    data class Enqueue(
        val directoryKey: String,
        val albumName: String,
        val action: ToggleAction,
    ) : ToggleRequestResolution

    data class RequestGrant(
        val directoryKey: String,
        val albumName: String,
        val action: ToggleAction,
        val initialUri: Uri,
    ) : ToggleRequestResolution

    data class Blocked(
        val directoryKey: String,
        val albumName: String,
        val action: ToggleAction,
        val reason: ToggleFailureReason,
    ) : ToggleRequestResolution
}

class ResolveToggleRequestUseCase(
    private val directoryGrantRepository: DirectoryGrantRepository,
) {
    suspend operator fun invoke(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
    ): ToggleRequestResolution {
        if (directoryGrantRepository.findGrant(directoryKey) != null) {
            return ToggleRequestResolution.Enqueue(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
            )
        }

        return when (directoryGrantRepository.validateGrantRequest(directoryKey)) {
            GrantError.RestrictedRoot -> ToggleRequestResolution.Blocked(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                reason = ToggleFailureReason.RestrictedRoot,
            )
            null -> ToggleRequestResolution.RequestGrant(
                directoryKey = directoryKey,
                albumName = albumName,
                action = action,
                initialUri = DirectoryGrantLauncher.createInitialUri(directoryKey),
            )
        }
    }
}
