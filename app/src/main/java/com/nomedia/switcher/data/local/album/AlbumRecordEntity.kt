package com.nomedia.switcher.data.local.album

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

@Entity(tableName = "album_records")
data class AlbumRecordEntity(
    @PrimaryKey val directoryKey: String,
    val displayName: String,
    val state: AlbumState,
    val treeUri: String?,
    val lastAction: ToggleAction?,
    val lastFailure: String?,
    val seenInLastScan: Boolean,
    val updatedAtEpochMs: Long,
)
