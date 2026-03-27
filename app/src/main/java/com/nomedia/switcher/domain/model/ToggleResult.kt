package com.nomedia.switcher.domain.model

sealed interface ToggleResult {
    data object Success : ToggleResult

    data class RetryableFailure(
        val reason: String,
    ) : ToggleResult

    data class PermanentFailure(
        val reason: String,
    ) : ToggleResult
}
