package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.data.local.settings.UserSettingsRepository

class SetAlbumSortModeUseCase(
    private val userSettingsRepository: UserSettingsRepository,
) {
    suspend operator fun invoke(mode: AlbumSortMode) {
        userSettingsRepository.setAlbumSortMode(mode)
    }
}
