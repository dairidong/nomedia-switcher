package com.nomedia.switcher.data.access

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "directory_grants")
data class DirectoryGrantEntity(
    @PrimaryKey val directoryKey: String,
    val treeUri: String,
    val persistedAtEpochMs: Long,
)
