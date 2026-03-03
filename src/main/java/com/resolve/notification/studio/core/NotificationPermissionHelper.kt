package com.resolve.notification.studio.core

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Provides notification permission checks and diagnostic utilities for the Notification Studio.
 */
object NotificationPermissionHelper {

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isPostNotificationsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getChannelImportance(context: Context, channelId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return NotificationManager.IMPORTANCE_DEFAULT
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(channelId) ?: return NotificationManager.IMPORTANCE_NONE
        return channel.importance
    }

    fun isHeadsUpEligible(context: Context, channelId: String): Boolean {
        if (!areNotificationsEnabled(context)) return false
        if (!isPostNotificationsGranted(context)) return false
        val importance = getChannelImportance(context, channelId)
        return importance >= NotificationManager.IMPORTANCE_HIGH
    }

    fun openNotificationSettings(context: Context, channelId: String? = null) {
        val intent = if (channelId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getDebugSummary(context: Context, channelId: String): String {
        val notifEnabled = areNotificationsEnabled(context)
        val postGranted = isPostNotificationsGranted(context)
        val importance = getChannelImportance(context, channelId)
        val headsUp = isHeadsUpEligible(context, channelId)

        return buildString {
            appendLine("Notifications enabled : ${yesNo(notifEnabled)}")
            appendLine("POST_NOTIFICATIONS    : ${if (postGranted) "GRANTED ✓" else "DENIED ✗"}")
            appendLine("Channel ($channelId)")
            appendLine("  Importance          : ${importanceName(importance)}")
            appendLine("Heads-up eligible     : ${yesNo(headsUp)}")
            append("API Level             : ${Build.VERSION.SDK_INT}")
        }
    }

    private fun yesNo(value: Boolean) = if (value) "YES ✓" else "NO ✗"

    private fun importanceName(importance: Int): String = when (importance) {
        NotificationManager.IMPORTANCE_HIGH        -> "HIGH — heads-up eligible"
        NotificationManager.IMPORTANCE_DEFAULT     -> "DEFAULT — sound, no heads-up"
        NotificationManager.IMPORTANCE_LOW         -> "LOW — silent"
        NotificationManager.IMPORTANCE_MIN         -> "MIN — collapsed"
        NotificationManager.IMPORTANCE_NONE        -> "NONE — blocked by user"
        NotificationManager.IMPORTANCE_UNSPECIFIED -> "UNSPECIFIED"
        else                                       -> "UNKNOWN ($importance)"
    }
}
