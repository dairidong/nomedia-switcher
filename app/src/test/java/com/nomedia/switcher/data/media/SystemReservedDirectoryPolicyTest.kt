package com.nomedia.switcher.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemReservedDirectoryPolicyTest {
    private val policy = SystemReservedDirectoryPolicy()

    @Test
    fun isReserved_returns_true_for_top_level_public_directories() {
        assertTrue(policy.isReserved("Pictures"))
        assertTrue(policy.isReserved("DCIM"))
        assertTrue(policy.isReserved("Download"))
        assertTrue(policy.isReserved("Recordings"))
    }

    @Test
    fun isReserved_returns_true_for_camera_and_standard_screenshots_directories() {
        assertTrue(policy.isReserved("DCIM/Camera"))
        assertTrue(policy.isReserved("Screenshots"))
        assertTrue(policy.isReserved("Pictures/Screenshots"))
        assertTrue(policy.isReserved("DCIM/Screenshots"))
        assertTrue(policy.isReserved("Movies/Screenshots"))
    }

    @Test
    fun isReserved_returns_false_for_nested_screenshots_and_regular_albums() {
        assertFalse(policy.isReserved("Pictures/foo/Screenshots"))
        assertFalse(policy.isReserved("DCIM/Foo/Screenshots"))
        assertFalse(policy.isReserved("Pictures/Vacation"))
        assertFalse(policy.isReserved("Pictures/.thumbnails"))
        assertFalse(policy.isReserved("Pictures/Screenshots/Edited"))
        assertFalse(policy.isReserved("DCIM/Camera/Burst"))
    }
}
