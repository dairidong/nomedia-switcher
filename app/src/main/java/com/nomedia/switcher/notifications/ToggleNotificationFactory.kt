package com.nomedia.switcher.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.nomedia.switcher.MainActivity
import com.nomedia.switcher.R
import com.nomedia.switcher.domain.model.ToggleAction
import com.nomedia.switcher.domain.model.ToggleResult

class ToggleNotificationFactory(
    private val context: Context,
) {
    fun buildProgressInfo(
        albumName: String,
        action: ToggleAction,
    ): ForegroundInfo {
        ensureChannel(
            channelId = PROGRESS_CHANNEL_ID,
            channelName = context.getString(R.string.toggle_progress_channel_name),
            importance = NotificationManager.IMPORTANCE_LOW,
        )
        return ForegroundInfo(
            PROGRESS_NOTIFICATION_ID,
            NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(
                    when (action) {
                        ToggleAction.Hide -> context.getString(R.string.toggle_hide_progress_title, albumName)
                        ToggleAction.Show -> context.getString(R.string.toggle_show_progress_title, albumName)
                    },
                )
                .setContentText(context.getString(R.string.toggle_progress_message))
                .setOngoing(true)
                .build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    fun notifyCompletion(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
        result: ToggleResult,
    ) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel(
            channelId = RESULT_CHANNEL_ID,
            channelName = context.getString(R.string.toggle_result_channel_name),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager()?.notify(
            directoryKey.hashCode(),
            buildCompletionNotification(directoryKey, albumName, action, result),
        )
    }

    private fun buildCompletionNotification(
        directoryKey: String,
        albumName: String,
        action: ToggleAction,
        result: ToggleResult,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            directoryKey.hashCode(),
            MainActivity.createOpenAlbumResultIntent(
                context = context,
                albumId = directoryKey,
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (result) {
            ToggleResult.Success -> when (action) {
                ToggleAction.Hide -> context.getString(R.string.toggle_hide_success_title, albumName)
                ToggleAction.Show -> context.getString(R.string.toggle_show_success_title, albumName)
            }
            is ToggleResult.PermanentFailure,
            is ToggleResult.RetryableFailure,
            -> context.getString(R.string.toggle_failure_title, albumName)
        }
        val message = when (result) {
            ToggleResult.Success -> context.getString(R.string.toggle_success_message)
            is ToggleResult.PermanentFailure -> result.reason
            is ToggleResult.RetryableFailure -> result.reason
        }

        return NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(
        channelId: String,
        channelName: String,
        importance: Int,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager()?.createNotificationChannel(
            NotificationChannel(channelId, channelName, importance),
        )
    }

    private fun notificationManager(): NotificationManager? {
        return context.getSystemService(NotificationManager::class.java)
    }

    companion object {
        private const val PROGRESS_CHANNEL_ID = "toggle_progress"
        private const val RESULT_CHANNEL_ID = "toggle_results"
        private const val PROGRESS_NOTIFICATION_ID = 1001
    }
}
