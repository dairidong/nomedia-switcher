package com.nomedia.switcher.data.media

import android.content.ContentUris
import android.provider.MediaStore

class MediaStoreAlbumScanner(
    private val directoryNormalizer: DirectoryNormalizer = DirectoryNormalizer(),
) {
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.VOLUME_NAME,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
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

                val newestRow = bucketRows.first()
                val coverUri = newestRow.volumeName
                    ?.let { volumeName ->
                        when (newestRow.mediaKind) {
                            MEDIA_KIND_VIDEO -> MediaStore.Video.Media.getContentUri(volumeName, newestRow.mediaId)
                            else -> MediaStore.Images.Media.getContentUri(volumeName, newestRow.mediaId)
                        }
                    }
                    ?: when (newestRow.mediaKind) {
                        MEDIA_KIND_VIDEO -> ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            newestRow.mediaId,
                        )
                        else -> ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            newestRow.mediaId,
                        )
                    }
                val coverUriString = coverUri.toString()

                AlbumCandidate(
                    bucketId = bucketId,
                    bucketName = bucketName,
                    directoryKey = directories.single(),
                    volumeName = bucketRows.firstNotNullOfOrNull { it.volumeName },
                    coverUri = coverUriString,
                    coverRelativeFilePath = newestRow.albumRelativeFilePath,
                    coverDisplayName = newestRow.displayName,
                    coverMediaKind = newestRow.mediaKind,
                )
            }
    }

    companion object {
        const val MEDIA_KIND_IMAGE = "image"
        const val MEDIA_KIND_VIDEO = "video"
    }
}
