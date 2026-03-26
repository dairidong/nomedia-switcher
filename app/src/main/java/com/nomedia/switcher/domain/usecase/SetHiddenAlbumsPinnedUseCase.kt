package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.settings.UserSettingsRepository

class SetHiddenAlbumsPinnedUseCase(
    private val userSettingsRepository: UserSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        userSettingsRepository.setPinHiddenAlbumsToTop(enabled)
    }
}
