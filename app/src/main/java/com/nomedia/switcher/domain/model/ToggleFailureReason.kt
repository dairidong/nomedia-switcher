package com.nomedia.switcher.domain.model

/**
 * Reasons a toggle operation can fail. The [persistedKey] values are serialized to long-lived
 * storage (album records, ViewModel state, etc.) and must never be mutated once released.
 * Append-only keys preserve the persistence contract for old data.
 */
enum class ToggleFailureReason(
    /**
     * Persistent key stored on disk; changing it or reusing another entry's value will break existing
     * persisted state. Only append new keys for new failure cases.
     */
    val persistedKey: String,
) {
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
