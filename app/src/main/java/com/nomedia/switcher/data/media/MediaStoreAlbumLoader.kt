package com.nomedia.switcher.data.media

import android.content.ContentResolver
import android.provider.MediaStore

class MediaStoreAlbumLoader(
    private val scanner: MediaStoreAlbumScanner = MediaStoreAlbumScanner(),
    private val reservedDirectoryPolicy: SystemReservedDirectoryPolicy = SystemReservedDirectoryPolicy(),
    private val queryRows: (ContentResolver, Array<String>) -> List<MediaStoreAlbumRow> = ::queryMediaRows,
) {
    fun load(contentResolver: ContentResolver): List<AlbumCandidate> {
        return scanner
            .fromRows(queryRows(contentResolver, scanner.projection))
            .filter { candidate ->
                reservedDirectoryPolicy.isSwitchable(candidate.directoryKey)
            }
    }
}

private fun queryMediaRows(
    contentResolver: ContentResolver,
    projection: Array<String>,
): List<MediaStoreAlbumRow> {
    val rows = mutableListOf<MediaStoreAlbumRow>()
    contentResolver.query(
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
        projection,
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
        arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        ),
        "${MediaStore.MediaColumns.DATE_MODIFIED} DESC, ${MediaStore.Files.FileColumns._ID} DESC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
        val volumeNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
        val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val mediaTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val displayName = cursor.getString(displayNameIndex)
            rows += MediaStoreAlbumRow(
                mediaId = cursor.getLong(idIndex),
                bucketId = cursor.getString(bucketIdIndex),
                bucketName = cursor.getString(bucketNameIndex),
                relativePath = cursor.getString(relativePathIndex),
                dataPath = null,
                volumeName = cursor.getString(volumeNameIndex),
                displayName = displayName,
                albumRelativeFilePath = displayName,
                mediaKind = when (cursor.getInt(mediaTypeIndex)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStoreAlbumScanner.MEDIA_KIND_VIDEO
                    else -> MediaStoreAlbumScanner.MEDIA_KIND_IMAGE
                },
                dateModifiedEpochMs = cursor.getLong(dateModifiedIndex)
                    .takeIf { it > 0L }
                    ?.times(1_000L),
            )
        }
    }
    return rows
}
