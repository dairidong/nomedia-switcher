package com.nomedia.switcher

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.data.local.AppDatabase
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.domain.model.AlbumState
import com.nomedia.switcher.domain.model.ToggleAction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runBlocking {
        database = AppDatabase.create(ApplicationProvider.getApplicationContext())
        database.clearAllTables()
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
        database.close()
    }

    @Test
    fun launch_showsTopBarSettingsAction() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Albums").assertCountEquals(0)

        scenario.close()
    }

    @Test
    fun background_completion_reopens_app_with_album_highlighted() = runBlocking {
        database.albumRecordDao().upsert(
            AlbumRecordEntity(
                directoryKey = "Pictures/Secret",
                displayName = "Secret",
                state = AlbumState.Hidden,
                treeUri = "content://tree/secret",
                lastAction = ToggleAction.Hide,
                lastFailure = null,
                seenInLastScan = true,
                updatedAtEpochMs = 1L,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenario = ActivityScenario.launch<MainActivity>(
            MainActivity.createOpenAlbumResultIntent(
                context = context,
                albumId = "Pictures/Secret",
            ),
        )

        composeTestRule.onNodeWithText("Opened from notification").assertIsDisplayed()
        composeTestRule.onNodeWithText("Secret").assertIsDisplayed()

        scenario.close()
    }
}
