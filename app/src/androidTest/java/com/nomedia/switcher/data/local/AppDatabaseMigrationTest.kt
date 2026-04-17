package com.nomedia.switcher.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate3To4_adds_cached_cover_columns_without_dropping_existing_rows() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO album_records (
                    directoryKey,
                    displayName,
                    state,
                    treeUri,
                    lastAction,
                    lastFailure,
                    seenInLastScan,
                    updatedAtEpochMs,
                    coverRelativeFilePath,
                    coverDisplayName,
                    coverMediaKind,
                    coverUpdatedAtEpochMs
                ) VALUES (
                    'Pictures/Travel',
                    'Travel',
                    'Hidden',
                    'content://tree/travel',
                    'Hide',
                    NULL,
                    1,
                    11,
                    'IMG_0042.jpg',
                    'IMG_0042.jpg',
                    'image',
                    22
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            AppDatabase.MIGRATION_3_4,
        )

        migratedDatabase.query(
            """
            SELECT displayName, cachedCoverPath, cachedCoverMediaKind, cachedCoverUpdatedAtEpochMs
            FROM album_records
            WHERE directoryKey = 'Pictures/Travel'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Travel", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }

    @Test
    fun migrate4To5_adds_latest_media_timestamp_without_dropping_existing_rows() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
                INSERT INTO album_records (
                    directoryKey,
                    displayName,
                    state,
                    treeUri,
                    lastAction,
                    lastFailure,
                    seenInLastScan,
                    updatedAtEpochMs,
                    coverRelativeFilePath,
                    coverDisplayName,
                    coverMediaKind,
                    coverUpdatedAtEpochMs,
                    cachedCoverPath,
                    cachedCoverMediaKind,
                    cachedCoverUpdatedAtEpochMs
                ) VALUES (
                    'Pictures/Travel',
                    'Travel',
                    'Hidden',
                    'content://tree/travel',
                    'Hide',
                    NULL,
                    1,
                    11,
                    'IMG_0042.jpg',
                    'IMG_0042.jpg',
                    'image',
                    22,
                    '/cache/travel.webp',
                    'image',
                    33
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDatabase = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        )

        migratedDatabase.query(
            """
            SELECT displayName, cachedCoverPath, latestMediaTimestampEpochMs
            FROM album_records
            WHERE directoryKey = 'Pictures/Travel'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Travel", cursor.getString(0))
            assertEquals("/cache/travel.webp", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
