package com.nomedia.switcher.data.access

import com.nomedia.switcher.worker.DirectoryGrantLookup

enum class GrantError {
    RestrictedRoot,
}

class DirectoryGrantRepository(
    private val directoryGrantDao: DirectoryGrantDao,
) : DirectoryGrantLookup {
    suspend fun saveGrant(
        directoryKey: String,
        treeUri: String,
    ) {
        directoryGrantDao.upsert(
            DirectoryGrantEntity(
                directoryKey = directoryKey,
                treeUri = treeUri,
                persistedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun findGrant(directoryKey: String): String? {
        return directoryGrantDao.findByDirectoryKey(directoryKey)?.treeUri
    }

    fun validateGrantRequest(directoryKey: String): GrantError? {
        val segments = directoryKey
            .split('/')
            .map(String::trim)
            .filter(String::isNotEmpty)

        return if (segments.size == 1 && segments[0].equals("Download", ignoreCase = true)) {
            GrantError.RestrictedRoot
        } else {
            null
        }
    }
}
