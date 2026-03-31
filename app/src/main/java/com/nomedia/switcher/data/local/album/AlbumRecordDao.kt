package com.nomedia.switcher.data.local.album

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomedia.switcher.domain.model.AlbumState
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumRecordDao {
    @Query("SELECT * FROM album_records ORDER BY directoryKey")
    fun observeAll(): Flow<List<AlbumRecordEntity>>

    @Query("SELECT * FROM album_records WHERE directoryKey = :directoryKey LIMIT 1")
    suspend fun findByDirectoryKey(directoryKey: String): AlbumRecordEntity?

    @Query("SELECT * FROM album_records WHERE state = :state ORDER BY directoryKey")
    suspend fun findByState(state: AlbumState): List<AlbumRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AlbumRecordEntity)
}
