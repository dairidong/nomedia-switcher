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
        return trimmed?.takeIf { it.isNotEmpty() }
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
            .takeIf { it.isNotEmpty() }
    }
}
