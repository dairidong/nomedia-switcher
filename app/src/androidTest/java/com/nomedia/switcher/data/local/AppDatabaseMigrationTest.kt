package com.nomedia.switcher.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun migrate2To3_adds_nullable_cover_reference_columns_without_dropping_rows() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO album_records (
                    directoryKey, displayName, state, treeUri, lastAction, lastFailure, seenInLastScan, updatedAtEpochMs
                ) VALUES (
                    'Pictures/Travel', 'Travel', 'Hidden', 'content://tree/travel', 'Hide', null, 1, 1
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)

        migratedDb.query("SELECT * FROM album_records").use { cursor ->
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())

            assertEquals("Pictures/Travel", cursor.getString(cursor.getColumnIndexOrThrow("directoryKey")))

            val coverRelativeFilePathIndex = cursor.getColumnIndexOrThrow("coverRelativeFilePath")
            val coverDisplayNameIndex = cursor.getColumnIndexOrThrow("coverDisplayName")
            val coverMediaKindIndex = cursor.getColumnIndexOrThrow("coverMediaKind")
            val coverUpdatedAtIndex = cursor.getColumnIndexOrThrow("coverUpdatedAtEpochMs")

            assertNull(cursor.getString(coverRelativeFilePathIndex))
            assertNull(cursor.getString(coverDisplayNameIndex))
            assertNull(cursor.getString(coverMediaKindIndex))
            assertTrue(cursor.isNull(coverUpdatedAtIndex))
        }

        migratedDb.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
