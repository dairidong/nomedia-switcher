package com.nomedia.switcher.notifications

import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.nomedia.switcher.domain.model.ToggleAction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
