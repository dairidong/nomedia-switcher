package com.nomedia.switcher.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.ui.theme.NoMediaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settings_screen_places_back_icon_and_title_in_top_bar() {
        composeTestRule.setContent {
            NoMediaTheme {
                SettingsScreen(
                    pinHiddenAlbumsToTop = true,
                    albumSortMode = AlbumSortMode.ByName,
                    onPinHiddenChanged = {},
                    onAlbumSortModeChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("返回")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("设置")
            .assertIsDisplayed()
    }

    @Test
    fun settings_screen_system_back_invokes_onBack() {
        val backInvocations = mutableIntStateOf(0)

        composeTestRule.setContent {
            NoMediaTheme {
                SettingsScreen(
                    pinHiddenAlbumsToTop = true,
                    albumSortMode = AlbumSortMode.ByName,
                    onPinHiddenChanged = {},
                    onAlbumSortModeChanged = {},
                    onBack = { backInvocations.intValue += 1 },
                )
            }
        }

        Espresso.pressBackUnconditionally()

        composeTestRule.runOnIdle {
            assertEquals(1, backInvocations.intValue)
        }
    }

    @Test
    fun settings_screen_shows_current_sort_value_and_opens_picker_on_click() {
        val selections = mutableListOf<AlbumSortMode>()

        composeTestRule.setContent {
            NoMediaTheme {
                SettingsScreen(
                    pinHiddenAlbumsToTop = true,
                    albumSortMode = AlbumSortMode.ByLatestMedia,
                    onPinHiddenChanged = {},
                    onAlbumSortModeChanged = selections::add,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("排序方式").assertIsDisplayed()
        composeTestRule.onNodeWithText("当前值: 按最新媒体时间").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("按名称").assertCountEquals(0)

        composeTestRule.onNodeWithText("仅在当前分组内排序，不影响已隐藏图集置顶。").performClick()
        composeTestRule.onNodeWithText("按名称").assertIsDisplayed().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(AlbumSortMode.ByName), selections)
        }
    }
}
