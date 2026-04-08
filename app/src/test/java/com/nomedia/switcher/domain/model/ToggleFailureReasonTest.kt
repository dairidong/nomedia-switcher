package com.nomedia.switcher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFailureReasonTest {
    @Test
    fun persisted_keys_are_unique_and_round_trip() {
        val keys = ToggleFailureReason.entries.map { it.persistedKey }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it.startsWith("toggle_failure/") })

        ToggleFailureReason.entries.forEach { reason ->
            assertEquals(reason, ToggleFailureReason.fromPersistedKey(reason.persistedKey))
        }
    }
}
