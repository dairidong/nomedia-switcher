package com.nomedia.switcher.data.media

import android.content.ContentResolver
import android.provider.MediaStore

class MediaStoreAlbumLoader(
    private val scanner: MediaStoreAlbumScanner = MediaStoreAlbumScanner(),
    private val queryRows: (ContentResolver, Array<String>) -> List<MediaStoreAlbumRow> = ::queryMediaRows,
) {
    fun load(contentResolver: ContentResolver): List<AlbumCandidate> {
        return scanner.fromRows(queryRows(contentResolver, scanner.projection))
    }
}

private fun queryMediaRows(
    contentResolver: ContentResolver,
    projection: Array<String>,
): List<MediaStoreAlbumRow> {
    val rows = mutableListOf<MediaStoreAlbumRow>()
    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
        val volumeNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)

        while (cursor.moveToNext()) {
            rows += MediaStoreAlbumRow(
                mediaId = cursor.getLong(idIndex),
                bucketId = cursor.getString(bucketIdIndex),
                bucketName = cursor.getString(bucketNameIndex),
                relativePath = cursor.getString(relativePathIndex),
                dataPath = null,
                volumeName = cursor.getString(volumeNameIndex),
            )
        }
    }
    return rows
}
