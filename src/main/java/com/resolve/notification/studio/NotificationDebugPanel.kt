package com.resolve.notification.studio

import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import com.resolve.notification.studio.core.NotificationChannelManager
import com.resolve.notification.studio.core.NotificationPermissionHelper
import com.resolve.notification.studio.databinding.ActivityNotificationStudioBinding

/**
 * Manages the collapsible debug information panel inside [NotificationStudioActivity].
 */
class NotificationDebugPanel(
    private val binding: ActivityNotificationStudioBinding,
) {

    fun setToggleListener(onToggle: () -> Unit) {
        binding.debugPanelHeader.setOnClickListener { onToggle() }
    }

    fun setExpanded(expanded: Boolean) {
        binding.debugPanelContent.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.tvDebugPanelTitle.text = if (expanded) "DEBUG PANEL  ▲" else "DEBUG PANEL  ▼"
    }

    fun updateSystemInfo(
        context: android.content.Context,
        permHelper: NotificationPermissionHelper,
        channelManager: NotificationChannelManager,
        channelId: String,
        lastNotifId: Int?,
        lastTimestamp: String?,
    ) {
        val notifEnabled = permHelper.areNotificationsEnabled(context)
        val postGranted = permHelper.isPostNotificationsGranted(context)
        val importance = permHelper.getChannelImportance(context, channelId)
        val headsUp = permHelper.isHeadsUpEligible(context, channelId)

        binding.tvDebugNotifEnabled.apply {
            text = "Notifications enabled: ${yesNo(notifEnabled)}"
            setTextColor(statusColor(notifEnabled))
        }

        binding.tvDebugPermissionStatus.apply {
            text = "POST_NOTIFICATIONS: ${if (postGranted) "GRANTED ✓" else "DENIED ✗"}"
            setTextColor(statusColor(postGranted))
        }

        binding.tvDebugChannelImportance.text = "Channel importance: ${importanceName(importance)}"

        binding.tvDebugHeadsUp.apply {
            text = "Heads-up eligible: ${yesNo(headsUp)}"
            setTextColor(statusColor(headsUp))
        }

        binding.tvDebugApiLevel.text = "API Level: ${Build.VERSION.SDK_INT}"

        binding.tvDebugChannelInfo.text = channelManager.getChannelDebugInfo()

        binding.tvDebugLastNotif.text = if (lastNotifId != null) {
            "Last notification: id=$lastNotifId @ $lastTimestamp"
        } else {
            "Last notification: none sent yet"
        }
    }

    fun updateLogs(logs: List<DebugLogEntry>) {
        binding.tvDebugLog.text = if (logs.isEmpty()) {
            "(no logs)"
        } else {
            logs.take(20).joinToString("\n") { entry ->
                val prefix = when (entry.level) {
                    LogLevel.INFO  -> "[I]"
                    LogLevel.WARN  -> "[W]"
                    LogLevel.ERROR -> "[E]"
                }
                "${entry.timestamp} $prefix ${entry.message}"
            }
        }
    }

    private fun yesNo(value: Boolean) = if (value) "YES ✓" else "NO ✗"

    private fun statusColor(ok: Boolean): Int {
        val ctx = binding.root.context
        return ContextCompat.getColor(
            ctx,
            if (ok) R.color.system_green else R.color.system_red,
        )
    }

    private fun importanceName(importance: Int): String = when (importance) {
        android.app.NotificationManager.IMPORTANCE_HIGH        -> "HIGH (heads-up eligible)"
        android.app.NotificationManager.IMPORTANCE_DEFAULT     -> "DEFAULT (sound, no heads-up)"
        android.app.NotificationManager.IMPORTANCE_LOW         -> "LOW (silent)"
        android.app.NotificationManager.IMPORTANCE_MIN         -> "MIN (collapsed)"
        android.app.NotificationManager.IMPORTANCE_NONE        -> "NONE (blocked by user)"
        android.app.NotificationManager.IMPORTANCE_UNSPECIFIED -> "UNSPECIFIED"
        else                                                   -> "UNKNOWN ($importance)"
    }
}
