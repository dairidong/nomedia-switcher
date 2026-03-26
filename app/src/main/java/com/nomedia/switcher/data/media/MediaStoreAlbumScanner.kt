package com.nomedia.switcher.data.media

import android.provider.MediaStore

class MediaStoreAlbumScanner(
    private val directoryNormalizer: DirectoryNormalizer = DirectoryNormalizer(),
) {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.MediaColumns.VOLUME_NAME,
    )

    fun fromRows(rows: List<MediaStoreAlbumRow>): List<AlbumCandidate> {
        return rows
            .filter { !it.bucketId.isNullOrBlank() }
            .groupBy { it.bucketId.orEmpty() }
            .mapNotNull { (bucketId, bucketRows) ->
                val directories = bucketRows
                    .mapNotNull { row ->
                        directoryNormalizer.normalize(
                            relativePath = row.relativePath,
                            dataPath = row.dataPath,
                        )
                    }
                    .distinct()

                if (directories.size != 1) {
                    return@mapNotNull null
                }

                val bucketName = bucketRows
                    .mapNotNull { it.bucketName?.trim()?.takeIf(String::isNotEmpty) }
                    .firstOrNull()
                    ?: directories.single().substringAfterLast('/')

                AlbumCandidate(
                    bucketId = bucketId,
                    bucketName = bucketName,
                    directoryKey = directories.single(),
                    volumeName = bucketRows.firstNotNullOfOrNull { it.volumeName },
                )
            }
    }
}
