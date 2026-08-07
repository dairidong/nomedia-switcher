package com.nomedia.switcher.data.media

class DirectoryNormalizer {
    fun normalize(
        relativePath: String?,
        dataPath: String?,
    ): String? {
        return normalizeRelativePath(relativePath) ?: normalizeDataPath(dataPath)
    }

    private fun normalizeRelativePath(relativePath: String?): String? {
        val trimmed = relativePath?.trim()?.trim('/')
        return trimmed
            ?.takeIf { it.isNotEmpty() }
            ?.canonicalizeTopLevelDirectory()
    }

    private fun normalizeDataPath(dataPath: String?): String? {
        val trimmed = dataPath?.trim()?.trimEnd('/') ?: return null
        val parentPath = trimmed.substringBeforeLast('/', missingDelimiterValue = "")
        if (parentPath.isEmpty()) {
            return null
        }

        return parentPath
            .substringAfter("/storage/emulated/0/", parentPath)
            .trimStart('/')
            .canonicalizeTopLevelDirectory()
            .takeIf { it.isNotEmpty() }
    }

    private fun String.canonicalizeTopLevelDirectory(): String {
        val segments = split('/')
        val first = segments.firstOrNull()?.takeIf { it.isNotEmpty() } ?: return this
        val canonicalFirst = DEFAULT_PUBLIC_DIRECTORIES.firstOrNull { it.equals(first, ignoreCase = true) }
            ?: first
        return (listOf(canonicalFirst) + segments.drop(1)).joinToString("/")
    }

    private companion object {
        val DEFAULT_PUBLIC_DIRECTORIES = listOf(
            "Music",
            "Podcasts",
            "Ringtones",
            "Alarms",
            "Notifications",
            "Pictures",
            "Movies",
            "Download",
            "DCIM",
            "Documents",
            "Audiobooks",
            "Recordings",
        )
    }
}
