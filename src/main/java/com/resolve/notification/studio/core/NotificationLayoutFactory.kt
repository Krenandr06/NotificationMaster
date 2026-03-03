package com.resolve.notification.studio.core

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import com.resolve.notification.studio.R
import com.resolve.notification.studio.style.NotificationStyleApplier

/**
 * Creates and binds data to notification layout views.
 */
object NotificationLayoutFactory {

    data class NotificationParams(
        val title: String,
        val timeText: String,
        val location: String,
        val leaveBy: String,
        val prepItems: String,
        val showLocation: Boolean,
        val showLeaveBy: Boolean,
        val showPrepItems: Boolean,
        val urgentMode: Boolean,
        val simulate5MinLeft: Boolean,
        val currentTime: String,
    )

    fun createCompactRemoteViews(
        context: Context,
        params: NotificationParams,
        styleConfig: NotificationStyleApplier.StyleConfig,
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_event_compact).also { rv ->
            bindCommonToRemoteViews(rv, params, styleConfig, context)
            if (styleConfig.backgroundColor != Color.TRANSPARENT) {
                rv.setInt(R.id.notification_compact_root, "setBackgroundColor", styleConfig.backgroundColor)
            }
        }
    }

    fun createExpandedRemoteViews(
        context: Context,
        params: NotificationParams,
        styleConfig: NotificationStyleApplier.StyleConfig,
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_event_expanded).also { rv ->
            bindCommonToRemoteViews(rv, params, styleConfig, context)
            bindExpandedToRemoteViews(rv, params, styleConfig, context)
            if (styleConfig.backgroundColor != Color.TRANSPARENT) {
                rv.setInt(R.id.notification_expanded_root, "setBackgroundColor", styleConfig.backgroundColor)
            }
        }
    }

    fun bindToView(
        view: View,
        params: NotificationParams,
        styleConfig: NotificationStyleApplier.StyleConfig,
    ) {
        bindCommonToView(view, params, styleConfig)
        bindExpandedToView(view, params, styleConfig)
    }

    private fun bindCommonToRemoteViews(
        rv: RemoteViews,
        params: NotificationParams,
        config: NotificationStyleApplier.StyleConfig,
        @Suppress("UNUSED_PARAMETER") ctx: Context,
    ) {
        val displayTime = if (params.simulate5MinLeft) "IN 5 MIN" else params.timeText

        rv.setImageViewResource(R.id.notification_icon, config.iconDrawableRes)
        rv.setInt(R.id.notification_icon, "setColorFilter", config.accentColor)

        rv.setTextViewText(R.id.notification_header_text, config.headerText)
        rv.setInt(R.id.notification_header_text, "setTextColor", config.accentColor)

        rv.setTextViewText(R.id.notification_time_header, params.currentTime)
        rv.setInt(R.id.notification_time_header, "setTextColor", config.headerTimeColor)

        rv.setTextViewText(R.id.notification_title, params.title.uppercase())
        rv.setInt(R.id.notification_title, "setTextColor", config.titleColor)
        rv.setTextViewTextSize(R.id.notification_title, TypedValue.COMPLEX_UNIT_SP, config.titleTextSizeSp)
        rv.setInt(R.id.notification_title, "setGravity", config.titleGravity)

        rv.setTextViewText(R.id.notification_time, displayTime)
        rv.setInt(R.id.notification_time, "setTextColor", config.timeColor)
        rv.setTextViewTextSize(R.id.notification_time, TypedValue.COMPLEX_UNIT_SP, config.timeTextSizeSp)
        rv.setInt(R.id.notification_time, "setGravity", config.timeBodyGravity)
    }

    private fun bindExpandedToRemoteViews(
        rv: RemoteViews,
        params: NotificationParams,
        config: NotificationStyleApplier.StyleConfig,
        @Suppress("UNUSED_PARAMETER") ctx: Context,
    ) {
        val showDivider = (params.showLocation && params.location.isNotBlank()) ||
            (params.showLeaveBy && params.leaveBy.isNotBlank()) ||
            (params.showPrepItems && params.prepItems.isNotBlank())

        rv.setViewVisibility(R.id.notification_divider, if (showDivider) View.VISIBLE else View.GONE)
        if (showDivider) {
            rv.setInt(R.id.notification_divider, "setBackgroundColor", config.dividerColor)
        }

        val showBadge = params.urgentMode && config.urgencyBadgeVisible
        rv.setViewVisibility(R.id.notification_urgency_badge, if (showBadge) View.VISIBLE else View.GONE)
        if (showBadge) {
            rv.setTextViewText(R.id.notification_urgency_badge, config.urgencyBadgeText)
            rv.setInt(R.id.notification_urgency_badge, "setTextColor", config.urgencyBadgeColor)
        }

        val locationVis = if (params.showLocation && params.location.isNotBlank()) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.notification_location, locationVis)
        rv.setViewVisibility(R.id.notification_location_row, locationVis)
        if (locationVis == View.VISIBLE) {
            rv.setTextViewText(R.id.notification_location, "📍 ${params.location}")
            rv.setInt(R.id.notification_location, "setTextColor", config.locationColor)
        }

        val leaveByVis = if (params.showLeaveBy && params.leaveBy.isNotBlank()) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.notification_leave_by, leaveByVis)
        if (leaveByVis == View.VISIBLE) {
            rv.setTextViewText(R.id.notification_leave_by, "LEAVE BY ${params.leaveBy}")
            rv.setInt(R.id.notification_leave_by, "setTextColor", config.leaveByColor)
        }

        val prepVis = if (params.showPrepItems && params.prepItems.isNotBlank()) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.notification_prep_items, prepVis)
        if (prepVis == View.VISIBLE) {
            rv.setTextViewText(R.id.notification_prep_items, params.prepItems)
            rv.setInt(R.id.notification_prep_items, "setTextColor", config.prepColor)
        }
    }

    private fun bindCommonToView(
        view: View,
        params: NotificationParams,
        config: NotificationStyleApplier.StyleConfig,
    ) {
        val displayTime = if (params.simulate5MinLeft) "IN 5 MIN" else params.timeText

        if (config.backgroundColor != Color.TRANSPARENT) {
            view.setBackgroundColor(config.backgroundColor)
        } else {
            view.background = null
        }

        view.findViewById<ImageView>(R.id.notification_icon)?.apply {
            setImageResource(config.iconDrawableRes)
            setColorFilter(config.accentColor)
        }

        view.findViewById<TextView>(R.id.notification_header_text)?.apply {
            text = config.headerText
            setTextColor(config.accentColor)
        }

        view.findViewById<TextView>(R.id.notification_time_header)?.apply {
            text = params.currentTime
            setTextColor(config.headerTimeColor)
        }

        view.findViewById<TextView>(R.id.notification_title)?.apply {
            text = params.title.uppercase()
            setTextColor(config.titleColor)
            textSize = config.titleTextSizeSp
            setTypeface(typeface, if (config.titleBold) Typeface.BOLD else Typeface.NORMAL)
            gravity = config.titleGravity
        }

        view.findViewById<TextView>(R.id.notification_time)?.apply {
            text = displayTime
            setTextColor(config.timeColor)
            textSize = config.timeTextSizeSp
            gravity = config.timeBodyGravity
        }
    }

    private fun bindExpandedToView(
        view: View,
        params: NotificationParams,
        config: NotificationStyleApplier.StyleConfig,
    ) {
        val showDivider = (params.showLocation && params.location.isNotBlank()) ||
            (params.showLeaveBy && params.leaveBy.isNotBlank()) ||
            (params.showPrepItems && params.prepItems.isNotBlank())

        view.findViewById<View>(R.id.notification_divider)?.apply {
            visibility = if (showDivider) View.VISIBLE else View.GONE
            if (showDivider) setBackgroundColor(config.dividerColor)
        }

        view.findViewById<TextView>(R.id.notification_urgency_badge)?.apply {
            val showBadge = params.urgentMode && config.urgencyBadgeVisible
            visibility = if (showBadge) View.VISIBLE else View.GONE
            if (showBadge) {
                text = config.urgencyBadgeText
                setTextColor(config.urgencyBadgeColor)
            }
        }

        val showLocation = params.showLocation && params.location.isNotBlank()
        view.findViewById<View>(R.id.notification_location_row)?.visibility =
            if (showLocation) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.notification_location)?.apply {
            visibility = if (showLocation) View.VISIBLE else View.GONE
            if (showLocation) {
                text = "📍 ${params.location}"
                setTextColor(config.locationColor)
            }
        }

        val showLeaveBy = params.showLeaveBy && params.leaveBy.isNotBlank()
        view.findViewById<TextView>(R.id.notification_leave_by)?.apply {
            visibility = if (showLeaveBy) View.VISIBLE else View.GONE
            if (showLeaveBy) {
                text = "LEAVE BY ${params.leaveBy}"
                setTextColor(config.leaveByColor)
            }
        }

        val showPrep = params.showPrepItems && params.prepItems.isNotBlank()
        view.findViewById<TextView>(R.id.notification_prep_items)?.apply {
            visibility = if (showPrep) View.VISIBLE else View.GONE
            if (showPrep) {
                text = params.prepItems
                setTextColor(config.prepColor)
            }
        }
    }
}
