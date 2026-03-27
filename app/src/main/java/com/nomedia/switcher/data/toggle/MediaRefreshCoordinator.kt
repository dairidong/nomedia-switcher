package com.nomedia.switcher.data.toggle

import com.nomedia.switcher.domain.model.ToggleResult

interface MediaRefreshCoordinator {
    suspend fun refresh(directoryKey: String): ToggleResult
}

class NoopMediaRefreshCoordinator : MediaRefreshCoordinator {
    override suspend fun refresh(directoryKey: String): ToggleResult = ToggleResult.Success
}
