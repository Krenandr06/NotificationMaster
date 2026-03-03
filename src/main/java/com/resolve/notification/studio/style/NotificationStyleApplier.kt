package com.resolve.notification.studio.style

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.resolve.notification.studio.R

/**
 * Maps a [NotificationStyle] to a [StyleConfig] and applies it to inflated notification Views.
 */
object NotificationStyleApplier {

    @ColorInt val CYAN          = Color.parseColor("#00E5FF")
    @ColorInt val ELECTRIC_BLUE = Color.parseColor("#00F0FF")
    @ColorInt val RED           = Color.parseColor("#FF3D00")
    @ColorInt val GOLD          = Color.parseColor("#FFD700")
    @ColorInt val GREEN         = Color.parseColor("#11D918")
    @ColorInt val PARTICLE_BLUE = Color.parseColor("#00B0FF")
    @ColorInt val DIM_WHITE     = Color.parseColor("#B3FFFFFF")
    @ColorInt val VOID_BORDER   = Color.parseColor("#3300E5FF")
    @ColorInt val WHITE         = Color.WHITE
    @ColorInt val BLACK         = Color.BLACK

    val STUDIO_PALETTE: List<Int> = listOf(
        Color.TRANSPARENT, WHITE, DIM_WHITE, CYAN, ELECTRIC_BLUE,
        PARTICLE_BLUE, GOLD, RED, GREEN, BLACK,
    )

    data class StyleConfig(
        @ColorInt val accentColor: Int,
        @ColorInt val titleColor: Int,
        @ColorInt val timeColor: Int,
        @ColorInt val headerTimeColor: Int,
        @ColorInt val locationColor: Int,
        @ColorInt val leaveByColor: Int,
        @ColorInt val prepColor: Int,
        @ColorInt val dividerColor: Int,
        @ColorInt val urgencyBadgeColor: Int,
        @ColorInt val backgroundColor: Int,
        val titleBold: Boolean,
        val titleTextSizeSp: Float,
        val timeTextSizeSp: Float,
        val titleGravity: Int,
        val timeBodyGravity: Int,
        val urgencyBadgeVisible: Boolean,
        val headerText: String,
        val urgencyBadgeText: String,
        @DrawableRes val iconDrawableRes: Int,
    )

    val SYSTEM_CONFIG = StyleConfig(
        accentColor         = CYAN,
        titleColor          = WHITE,
        timeColor           = CYAN,
        headerTimeColor     = DIM_WHITE,
        locationColor       = DIM_WHITE,
        leaveByColor        = RED,
        prepColor           = GOLD,
        dividerColor        = VOID_BORDER,
        urgencyBadgeColor   = RED,
        backgroundColor     = Color.TRANSPARENT,
        titleBold           = true,
        titleTextSizeSp     = 15f,
        timeTextSizeSp      = 13f,
        titleGravity        = Gravity.START,
        timeBodyGravity     = Gravity.START,
        urgencyBadgeVisible = false,
        headerText          = "SYSTEM",
        urgencyBadgeText    = "⚠ URGENT — ACTION REQUIRED",
        iconDrawableRes     = R.drawable.ic_system_down_triangle,
    )

    fun configFor(
        style: NotificationStyle,
        customConfig: StyleConfig? = null,
    ): StyleConfig = when (style) {

        NotificationStyle.MINIMAL -> StyleConfig(
            accentColor         = WHITE,
            titleColor          = WHITE,
            timeColor           = DIM_WHITE,
            headerTimeColor     = DIM_WHITE,
            locationColor       = DIM_WHITE,
            leaveByColor        = DIM_WHITE,
            prepColor           = DIM_WHITE,
            dividerColor        = VOID_BORDER,
            urgencyBadgeColor   = RED,
            backgroundColor     = Color.TRANSPARENT,
            titleBold           = false,
            titleTextSizeSp     = 14f,
            timeTextSizeSp      = 12f,
            titleGravity        = Gravity.START,
            timeBodyGravity     = Gravity.START,
            urgencyBadgeVisible = false,
            headerText          = "NOTIFY",
            urgencyBadgeText    = "⚠ URGENT",
            iconDrawableRes     = R.drawable.ic_system_down_triangle,
        )

        NotificationStyle.SYSTEM -> SYSTEM_CONFIG

        NotificationStyle.RESOLVE_PREMIUM -> StyleConfig(
            accentColor         = ELECTRIC_BLUE,
            titleColor          = WHITE,
            timeColor           = GOLD,
            headerTimeColor     = DIM_WHITE,
            locationColor       = DIM_WHITE,
            leaveByColor        = RED,
            prepColor           = GOLD,
            dividerColor        = VOID_BORDER,
            urgencyBadgeColor   = RED,
            backgroundColor     = Color.TRANSPARENT,
            titleBold           = true,
            titleTextSizeSp     = 16f,
            timeTextSizeSp      = 14f,
            titleGravity        = Gravity.START,
            timeBodyGravity     = Gravity.START,
            urgencyBadgeVisible = false,
            headerText          = "RESOLVE",
            urgencyBadgeText    = "⚠ URGENT — ACTION REQUIRED",
            iconDrawableRes     = R.drawable.ic_system_diamond,
        )

        NotificationStyle.URGENT_ALERT -> StyleConfig(
            accentColor         = RED,
            titleColor          = WHITE,
            timeColor           = RED,
            headerTimeColor     = DIM_WHITE,
            locationColor       = DIM_WHITE,
            leaveByColor        = RED,
            prepColor           = RED,
            dividerColor        = RED,
            urgencyBadgeColor   = RED,
            backgroundColor     = Color.TRANSPARENT,
            titleBold           = true,
            titleTextSizeSp     = 16f,
            timeTextSizeSp      = 14f,
            titleGravity        = Gravity.START,
            timeBodyGravity     = Gravity.START,
            urgencyBadgeVisible = true,
            headerText          = "URGENT",
            urgencyBadgeText    = "⚠ URGENT — ACTION REQUIRED",
            iconDrawableRes     = R.drawable.ic_system_right_triangle,
        )

        NotificationStyle.CUSTOM -> customConfig ?: SYSTEM_CONFIG
    }

    fun applyToView(rootView: View, config: StyleConfig) {
        if (config.backgroundColor != Color.TRANSPARENT) {
            rootView.setBackgroundColor(config.backgroundColor)
        } else {
            rootView.background = null
        }

        rootView.findViewById<ImageView>(R.id.notification_icon)?.apply {
            setImageResource(config.iconDrawableRes)
            setColorFilter(config.accentColor)
        }

        rootView.findViewById<TextView>(R.id.notification_header_text)?.apply {
            text = config.headerText
            setTextColor(config.accentColor)
        }

        rootView.findViewById<TextView>(R.id.notification_time_header)
            ?.setTextColor(config.headerTimeColor)

        rootView.findViewById<TextView>(R.id.notification_title)?.apply {
            setTextColor(config.titleColor)
            textSize = config.titleTextSizeSp
            setTypeface(typeface, if (config.titleBold) Typeface.BOLD else Typeface.NORMAL)
            gravity = config.titleGravity
        }

        rootView.findViewById<TextView>(R.id.notification_time)?.apply {
            setTextColor(config.timeColor)
            textSize = config.timeTextSizeSp
            gravity = config.timeBodyGravity
        }

        rootView.findViewById<TextView>(R.id.notification_location)
            ?.setTextColor(config.locationColor)

        rootView.findViewById<TextView>(R.id.notification_leave_by)
            ?.setTextColor(config.leaveByColor)

        rootView.findViewById<TextView>(R.id.notification_prep_items)
            ?.setTextColor(config.prepColor)

        rootView.findViewById<View>(R.id.notification_divider)
            ?.setBackgroundColor(config.dividerColor)

        rootView.findViewById<TextView>(R.id.notification_urgency_badge)?.apply {
            visibility = if (config.urgencyBadgeVisible) View.VISIBLE else View.GONE
            if (config.urgencyBadgeVisible) {
                text = config.urgencyBadgeText
                setTextColor(config.urgencyBadgeColor)
            }
        }
    }
}
