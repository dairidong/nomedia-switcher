package com.nomedia.switcher.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFailureReasonUiPolicyTest {
    @Test
    fun grant_related_failures_do_not_persist_album_failure_state() {
        assertFalse(ToggleFailureReason.RestrictedRoot.shouldPersistAlbumFailureState())
        assertFalse(ToggleFailureReason.GrantDenied.shouldPersistAlbumFailureState())
        assertFalse(ToggleFailureReason.WrongDirectorySelected.shouldPersistAlbumFailureState())
        assertFalse(ToggleFailureReason.PersistPermissionDenied.shouldPersistAlbumFailureState())
        assertFalse(ToggleFailureReason.MissingDirectoryGrant.shouldPersistAlbumFailureState())
    }

    @Test
    fun operational_failures_still_persist_album_failure_state() {
        assertTrue(ToggleFailureReason.Interrupted.shouldPersistAlbumFailureState())
        assertTrue(ToggleFailureReason.UnableToCreateNomedia.shouldPersistAlbumFailureState())
        assertTrue(ToggleFailureReason.UnableToRemoveNomedia.shouldPersistAlbumFailureState())
    }
}
