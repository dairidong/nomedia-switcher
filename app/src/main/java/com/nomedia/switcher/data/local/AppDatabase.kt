package com.nomedia.switcher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.nomedia.switcher.data.local.album.AlbumRecordDao
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

@Database(
    entities = [AlbumRecordEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumRecordDao(): AlbumRecordDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "nomedia-switcher.db",
            ).build()
        }
    }
}

class DatabaseConverters {
    @TypeConverter
    fun fromAlbumState(value: AlbumState): String = value.name

    @TypeConverter
    fun toAlbumState(value: String): AlbumState = AlbumState.valueOf(value)

    @TypeConverter
    fun fromToggleAction(value: ToggleAction?): String? = value?.name

    @TypeConverter
    fun toToggleAction(value: String?): ToggleAction? = value?.let(ToggleAction::valueOf)
}
