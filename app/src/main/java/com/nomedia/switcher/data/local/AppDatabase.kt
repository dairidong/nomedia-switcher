package com.nomedia.switcher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nomedia.switcher.data.access.DirectoryGrantDao
import com.nomedia.switcher.data.access.DirectoryGrantEntity
import com.nomedia.switcher.data.local.album.AlbumRecordDao
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction

@Database(
    entities = [AlbumRecordEntity::class, DirectoryGrantEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumRecordDao(): AlbumRecordDao
    abstract fun directoryGrantDao(): DirectoryGrantDao

    companion object {
        private const val DATABASE_NAME = "nomedia-switcher.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE album_records ADD COLUMN coverRelativeFilePath TEXT")
                db.execSQL("ALTER TABLE album_records ADD COLUMN coverDisplayName TEXT")
                db.execSQL("ALTER TABLE album_records ADD COLUMN coverMediaKind TEXT")
                db.execSQL("ALTER TABLE album_records ADD COLUMN coverUpdatedAtEpochMs INTEGER")
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_2_3).build()
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
