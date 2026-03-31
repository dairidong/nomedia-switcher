package com.nomedia.switcher.worker

import android.content.Context
import androidx.work.ForegroundInfo
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.notifications.ToggleNotificationFactory

object ToggleForegroundInfoFactory {
    fun create(
        context: Context,
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo {
        return ToggleNotificationFactory(context).buildProgressInfo(albumName, action)
    }
}
