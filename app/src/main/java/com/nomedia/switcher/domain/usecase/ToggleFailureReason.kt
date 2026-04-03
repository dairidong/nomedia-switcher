package com.nomedia.switcher.domain.usecase

enum class ToggleFailureReason(val persistedKey: String) {
    RestrictedRoot("toggle_failure/restricted_root"),
    GrantDenied("toggle_failure/grant_denied"),
    WrongDirectorySelected("toggle_failure/wrong_directory_selected"),
    PersistPermissionDenied("toggle_failure/persist_permission_denied"),
    Interrupted("toggle_failure/interrupted"),
    ;

    companion object {
        fun fromPersistedKey(value: String): ToggleFailureReason? {
            return entries.firstOrNull { it.persistedKey == value }
        }
    }
}
