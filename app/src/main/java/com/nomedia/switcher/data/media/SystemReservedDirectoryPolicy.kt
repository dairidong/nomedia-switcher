package com.nomedia.switcher.data.media

class SystemReservedDirectoryPolicy {
    fun isReserved(directoryKey: String): Boolean {
        val segments = directoryKey
            .split('/')
            .map(String::trim)
            .filter(String::isNotEmpty)

        if (segments.isEmpty()) {
            return false
        }

        if (segments.size == 1 && isDefaultPublicDirectory(segments[0])) {
            return true
        }

        if (segments.size == 2 &&
            segments[0].equals(DCIM_DIRECTORY, ignoreCase = true) &&
            segments[1].equals(CAMERA_DIRECTORY, ignoreCase = true)
        ) {
            return true
        }

        if (!segments.last().equals(SCREENSHOTS_DIRECTORY, ignoreCase = true)) {
            return false
        }

        return when (segments.size) {
            1 -> true
            2 -> isDefaultPublicDirectory(segments[0])
            else -> false
        }
    }

    fun isSwitchable(directoryKey: String): Boolean = !isReserved(directoryKey)

    private fun isDefaultPublicDirectory(segment: String): Boolean {
        return DEFAULT_PUBLIC_DIRECTORIES.any { it.equals(segment, ignoreCase = true) }
    }

    private companion object {
        const val DCIM_DIRECTORY = "DCIM"
        const val CAMERA_DIRECTORY = "Camera"
        const val SCREENSHOTS_DIRECTORY = "Screenshots"

        val DEFAULT_PUBLIC_DIRECTORIES = setOf(
            "Music",
            "Podcasts",
            "Ringtones",
            "Alarms",
            "Notifications",
            "Pictures",
            "Movies",
            "Download",
            DCIM_DIRECTORY,
            "Documents",
            "Audiobooks",
            "Recordings",
        )
    }
}
