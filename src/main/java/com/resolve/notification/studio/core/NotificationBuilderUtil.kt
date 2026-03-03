package com.resolve.notification.studio.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.resolve.notification.studio.NotificationStudioActivity
import com.resolve.notification.studio.R
import com.resolve.notification.studio.style.NotificationStyle
import com.resolve.notification.studio.style.NotificationStyleApplier
import timber.log.Timber

/**
 * Builds and dispatches test notifications from the Notification Studio.
 */
object NotificationBuilderUtil {

    const val STUDIO_NOTIFICATION_ID = 99_001
    private const val TAG = "StudioNotifBuilder"

    fun sendTestNotification(
        context: Context,
        params: NotificationLayoutFactory.NotificationParams,
        style: NotificationStyle,
        customStyleConfig: NotificationStyleApplier.StyleConfig? = null,
        forceHeadsUp: Boolean,
        simulateChannelDisabled: Boolean,
        channelManager: NotificationChannelManager,
    ): Int {
        if (simulateChannelDisabled) {
            Timber.tag(TAG).d("Channel-disabled simulation active — notification suppressed")
            return -1
        }

        val channelId = channelManager.ensureChannelExists(urgent = params.urgentMode || forceHeadsUp)
        val styleConfig = NotificationStyleApplier.configFor(style, customStyleConfig)

        val compactView = NotificationLayoutFactory.createCompactRemoteViews(context, params, styleConfig)
        val expandedView = NotificationLayoutFactory.createExpandedRemoteViews(context, params, styleConfig)

        val tapPendingIntent = buildTapIntent(context, requestCode = STUDIO_NOTIFICATION_ID)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(
                if (params.urgentMode) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setContentTitle(params.title)
            .setContentText(if (params.simulate5MinLeft) "IN 5 MIN" else params.timeText)

        if (forceHeadsUp || params.urgentMode) {
            val fullScreenPendingIntent = buildTapIntent(context, requestCode = STUDIO_NOTIFICATION_ID + 1)
            builder.setFullScreenIntent(fullScreenPendingIntent, params.urgentMode)
        }

        NotificationManagerCompat.from(context).notify(STUDIO_NOTIFICATION_ID, builder.build())
        Timber.tag(TAG).d(
            "Test notification posted: id=$STUDIO_NOTIFICATION_ID, " +
                "channel=$channelId, style=${style.displayName}, urgent=${params.urgentMode}",
        )

        return STUDIO_NOTIFICATION_ID
    }

    private fun buildTapIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationStudioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
