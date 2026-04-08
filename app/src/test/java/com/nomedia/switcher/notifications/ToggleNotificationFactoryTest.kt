package com.nomedia.switcher.notifications

import android.content.Context
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.R
import com.nomedia.switcher.domain.model.ToggleAction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ToggleNotificationFactoryTest {
    @Test
    fun progress_info_uses_data_sync_foreground_service_type() {
        val factory = ToggleNotificationFactory(ApplicationProvider.getApplicationContext())

        val info = factory.buildProgressInfo(
            albumName = "Camera",
            action = ToggleAction.Hide,
        )

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            info.foregroundServiceType,
        )
    }

    @Test
    fun progress_info_shows_localized_strings_default_locale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = ToggleNotificationFactory(context)

        val info = factory.buildProgressInfo(
            albumName = "Camera",
            action = ToggleAction.Hide,
        )
        val shadow = Shadows.shadowOf(info.notification)

        assertEquals(
            context.getString(R.string.toggle_hide_progress_title, "Camera"),
            shadow.contentTitle.toString(),
        )
        assertEquals(
            context.getString(R.string.toggle_progress_message),
            shadow.contentText.toString(),
        )
    }

    @Test
    fun progress_info_shows_localized_strings_zh_locale() {
        val context = createLocaleContext(Locale.SIMPLIFIED_CHINESE)
        val factory = ToggleNotificationFactory(context)

        val info = factory.buildProgressInfo(
            albumName = "相机",
            action = ToggleAction.Hide,
        )
        val shadow = Shadows.shadowOf(info.notification)

        assertEquals(
            context.getString(R.string.toggle_hide_progress_title, "相机"),
            shadow.contentTitle.toString(),
        )
        assertEquals(
            context.getString(R.string.toggle_progress_message),
            shadow.contentText.toString(),
        )
    }

    @Test
    fun app_name_is_consistent_across_locales() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("NoMedia Switcher", context.getString(R.string.app_name))

        val zhContext = createLocaleContext(Locale.SIMPLIFIED_CHINESE)
        assertEquals("NoMedia Switcher", zhContext.getString(R.string.app_name))
    }

    private fun createLocaleContext(locale: Locale): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }
}
