package com.nomedia.switcher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFailureReasonTest {
    @Test
    fun persisted_keys_are_stable_unique_and_round_trip() {
        val keys = ToggleFailureReason.entries.map { it.persistedKey }

        assertEquals("toggle_failure/restricted_root", ToggleFailureReason.RestrictedRoot.persistedKey)
        assertEquals("toggle_failure/grant_denied", ToggleFailureReason.GrantDenied.persistedKey)
        assertEquals("toggle_failure/wrong_directory_selected", ToggleFailureReason.WrongDirectorySelected.persistedKey)
        assertEquals("toggle_failure/persist_permission_denied", ToggleFailureReason.PersistPermissionDenied.persistedKey)
        assertEquals("toggle_failure/interrupted", ToggleFailureReason.Interrupted.persistedKey)
        assertEquals("toggle_failure/missing_directory_grant", ToggleFailureReason.MissingDirectoryGrant.persistedKey)
        assertEquals("toggle_failure/unable_to_create_nomedia", ToggleFailureReason.UnableToCreateNomedia.persistedKey)
        assertEquals("toggle_failure/unable_to_remove_nomedia", ToggleFailureReason.UnableToRemoveNomedia.persistedKey)

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it.startsWith("toggle_failure/") })

        ToggleFailureReason.entries.forEach { reason ->
            assertEquals(reason, ToggleFailureReason.fromPersistedKey(reason.persistedKey))
        }
    }
}
