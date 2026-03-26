package com.nomedia.switcher.data.access

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DirectoryGrantDao {
    @Query("SELECT * FROM directory_grants WHERE directoryKey = :directoryKey LIMIT 1")
    suspend fun findByDirectoryKey(directoryKey: String): DirectoryGrantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DirectoryGrantEntity)
}
