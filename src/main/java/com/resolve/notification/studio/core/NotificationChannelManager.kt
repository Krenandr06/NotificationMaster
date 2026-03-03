package com.resolve.notification.studio.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * Manages the notification channels used by the Notification Studio standalone app.
 */
class NotificationChannelManager(private val context: Context) {

    companion object {
        const val STUDIO_CHANNEL_ID = "notification_studio_preview"
        const val STUDIO_CHANNEL_ID_URGENT = "notification_studio_preview_urgent"
        private const val TAG = "StudioChannelMgr"
    }

    fun ensureChannelExists(urgent: Boolean): String {
        val channelId = if (urgent) STUDIO_CHANNEL_ID_URGENT else STUDIO_CHANNEL_ID

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return channelId
        }

        val manager = context.getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(channelId) != null) {
            Timber.tag(TAG).d("Channel already exists: $channelId — reusing")
            return channelId
        }

        val importance = if (urgent) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

        val channel = NotificationChannel(
            channelId,
            if (urgent) "Notification Studio — Urgent Preview" else "Notification Studio Preview",
            importance,
        ).apply {
            description = if (urgent) {
                "Urgent test notifications from the Notification Studio"
            } else {
                "Standard test notifications from the Notification Studio"
            }
            enableVibration(urgent)
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
        Timber.tag(TAG).d("Created channel: $channelId")

        return channelId
    }

    fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun getChannelDebugInfo(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "NotificationChannels not available (API < 26)"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        val sb = StringBuilder()

        for (channelId in listOf(STUDIO_CHANNEL_ID, STUDIO_CHANNEL_ID_URGENT)) {
            val channel = manager.getNotificationChannel(channelId)
            if (channel != null) {
                sb.appendLine("[$channelId]")
                sb.appendLine("  Name       : ${channel.name}")
                sb.appendLine("  Importance : ${importanceName(channel.importance)}")
                sb.appendLine("  Enabled    : ${channel.importance != NotificationManager.IMPORTANCE_NONE}")
                sb.appendLine("  Vibration  : ${channel.shouldVibrate()}")
                sb.appendLine("  Badge      : ${channel.canShowBadge()}")
            } else {
                sb.appendLine("[$channelId] — NOT YET CREATED")
            }
        }

        return sb.trimEnd().toString()
    }

    private fun importanceName(importance: Int): String = when (importance) {
        NotificationManager.IMPORTANCE_HIGH        -> "HIGH (heads-up)"
        NotificationManager.IMPORTANCE_DEFAULT     -> "DEFAULT (sound)"
        NotificationManager.IMPORTANCE_LOW         -> "LOW (silent)"
        NotificationManager.IMPORTANCE_MIN         -> "MIN (collapsed)"
        NotificationManager.IMPORTANCE_NONE        -> "NONE (blocked)"
        NotificationManager.IMPORTANCE_UNSPECIFIED -> "UNSPECIFIED"
        else                                       -> "UNKNOWN ($importance)"
    }
}
