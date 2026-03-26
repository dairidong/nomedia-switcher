package com.nomedia.switcher.data.local.album

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumRecordDao {
    @Query("SELECT * FROM album_records ORDER BY directoryKey")
    fun observeAll(): Flow<List<AlbumRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AlbumRecordEntity)
}
