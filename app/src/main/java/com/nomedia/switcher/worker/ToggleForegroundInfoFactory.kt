package com.nomedia.switcher.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.nomedia.switcher.R
import com.nomedia.switcher.domain.model.ToggleAction

object ToggleForegroundInfoFactory {
    private const val CHANNEL_ID = "toggle_progress"
    private const val NOTIFICATION_ID = 1001

    fun create(
        context: Context,
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(
                when (action) {
                    ToggleAction.Hide -> context.getString(R.string.toggle_hide_progress_title, albumName)
                    ToggleAction.Show -> context.getString(R.string.toggle_show_progress_title, albumName)
                },
            )
            .setContentText(context.getString(R.string.toggle_progress_message))
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.toggle_progress_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }
}
