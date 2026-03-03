package com.resolve.notification.studio

import android.content.Context
import androidx.lifecycle.ViewModel
import com.resolve.notification.studio.core.NotificationBuilderUtil
import com.resolve.notification.studio.core.NotificationChannelManager
import com.resolve.notification.studio.core.NotificationLayoutFactory
import com.resolve.notification.studio.core.NotificationPermissionHelper
import com.resolve.notification.studio.style.NotificationStyle
import com.resolve.notification.studio.style.NotificationStyleApplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebugLogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO,
)

enum class LogLevel { INFO, WARN, ERROR }

data class StudioState(
    val title: String = "TEAM MEETING",
    val timeText: String = "IN 15 MIN",
    val location: String = "Conference Room A",
    val leaveBy: String = "9:45 AM",
    val prepItems: String = "• Laptop\n• Notebook",

    val showLocation: Boolean = true,
    val showLeaveBy: Boolean = true,
    val showPrepItems: Boolean = false,

    val urgentMode: Boolean = false,
    val forceHeadsUp: Boolean = true,
    val simulate5MinLeft: Boolean = false,
    val simulateChannelDisabled: Boolean = false,

    val selectedStyle: NotificationStyle = NotificationStyle.SYSTEM,
    val customStyleConfig: NotificationStyleApplier.StyleConfig = NotificationStyleApplier.SYSTEM_CONFIG,

    val isPreviewExpanded: Boolean = true,

    val isDebugPanelExpanded: Boolean = false,
    val lastNotificationId: Int? = null,
    val lastNotificationTimestamp: String? = null,
    val debugLogs: List<DebugLogEntry> = emptyList(),
)

class NotificationStudioViewModel : ViewModel() {

    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setTitle(title: String) = _state.update { it.copy(title = title) }
    fun setTimeText(timeText: String) = _state.update { it.copy(timeText = timeText) }
    fun setLocation(location: String) = _state.update { it.copy(location = location) }
    fun setLeaveBy(leaveBy: String) = _state.update { it.copy(leaveBy = leaveBy) }
    fun setPrepItems(prepItems: String) = _state.update { it.copy(prepItems = prepItems) }

    fun setShowLocation(show: Boolean) = _state.update { it.copy(showLocation = show) }
    fun setShowLeaveBy(show: Boolean) = _state.update { it.copy(showLeaveBy = show) }
    fun setShowPrepItems(show: Boolean) = _state.update { it.copy(showPrepItems = show) }
    fun setUrgentMode(urgent: Boolean) = _state.update { it.copy(urgentMode = urgent) }
    fun setForceHeadsUp(forceHeadsUp: Boolean) = _state.update { it.copy(forceHeadsUp = forceHeadsUp) }
    fun setSimulate5MinLeft(simulate: Boolean) = _state.update { it.copy(simulate5MinLeft = simulate) }
    fun setSimulateChannelDisabled(simulate: Boolean) = _state.update { it.copy(simulateChannelDisabled = simulate) }
    fun setSelectedStyle(style: NotificationStyle) = _state.update { it.copy(selectedStyle = style) }
    fun setPreviewExpanded(expanded: Boolean) = _state.update { it.copy(isPreviewExpanded = expanded) }
    fun toggleDebugPanel() = _state.update { it.copy(isDebugPanelExpanded = !it.isDebugPanelExpanded) }

    fun setCustomBgColor(color: Int)           = updateCustomConfig { copy(backgroundColor = color) }
    fun setCustomAccentColor(color: Int)       = updateCustomConfig { copy(accentColor = color) }
    fun setCustomTitleColor(color: Int)        = updateCustomConfig { copy(titleColor = color) }
    fun setCustomTitleSizeSp(sp: Float)        = updateCustomConfig { copy(titleTextSizeSp = sp) }
    fun setCustomTitleGravity(gravity: Int)    = updateCustomConfig { copy(titleGravity = gravity) }
    fun setCustomTitleBold(bold: Boolean)      = updateCustomConfig { copy(titleBold = bold) }
    fun setCustomTimeColor(color: Int)         = updateCustomConfig { copy(timeColor = color) }
    fun setCustomTimeSizeSp(sp: Float)         = updateCustomConfig { copy(timeTextSizeSp = sp) }
    fun setCustomTimeGravity(gravity: Int)     = updateCustomConfig { copy(timeBodyGravity = gravity) }
    fun setCustomHeaderTimeColor(color: Int)   = updateCustomConfig { copy(headerTimeColor = color) }
    fun setCustomHeaderText(text: String)      = updateCustomConfig { copy(headerText = text) }
    fun setCustomLocationColor(color: Int)     = updateCustomConfig { copy(locationColor = color) }
    fun setCustomLeaveByColor(color: Int)      = updateCustomConfig { copy(leaveByColor = color) }
    fun setCustomPrepColor(color: Int)         = updateCustomConfig { copy(prepColor = color) }
    fun setCustomDividerColor(color: Int)      = updateCustomConfig { copy(dividerColor = color) }
    fun setCustomUrgencyBadgeColor(color: Int) = updateCustomConfig { copy(urgencyBadgeColor = color) }
    fun setCustomUrgencyBadgeText(text: String)= updateCustomConfig { copy(urgencyBadgeText = text) }
    fun setCustomIconDrawable(res: Int)        = updateCustomConfig { copy(iconDrawableRes = res) }

    fun resetCustomConfig() = _state.update { it.copy(customStyleConfig = NotificationStyleApplier.SYSTEM_CONFIG) }

    private fun updateCustomConfig(
        block: NotificationStyleApplier.StyleConfig.() -> NotificationStyleApplier.StyleConfig,
    ) {
        _state.update { it.copy(customStyleConfig = it.customStyleConfig.block()) }
    }

    fun sendTestNotification(
        context: Context,
        channelManager: NotificationChannelManager,
        permHelper: NotificationPermissionHelper,
    ) {
        val s = _state.value

        if (!permHelper.isPostNotificationsGranted(context)) {
            appendLog("POST_NOTIFICATIONS not granted — cannot send notification", LogLevel.ERROR)
            return
        }

        if (!permHelper.areNotificationsEnabled(context)) {
            appendLog("Notifications disabled for app — cannot send notification", LogLevel.WARN)
            return
        }

        val params = buildParams(s)
        val notifId = NotificationBuilderUtil.sendTestNotification(
            context = context,
            params = params,
            style = s.selectedStyle,
            customStyleConfig = s.customStyleConfig,
            forceHeadsUp = s.forceHeadsUp,
            simulateChannelDisabled = s.simulateChannelDisabled,
            channelManager = channelManager,
        )

        val ts = timestamp()
        if (notifId >= 0) {
            appendLog(
                "Sent: id=$notifId | style=${s.selectedStyle.displayName} | " +
                    "urgent=${s.urgentMode} | headsUp=${s.forceHeadsUp}",
            )
            _state.update { it.copy(lastNotificationId = notifId, lastNotificationTimestamp = ts) }
        } else {
            appendLog("Suppressed: channel-disabled simulation is active", LogLevel.WARN)
        }
    }

    fun exportConfig(): String {
        val s = _state.value
        return buildString {
            appendLine("<!-- ══════════════════════════════════════════════════════════════════ -->")
            appendLine("<!-- Notification Studio — Configuration Export                        -->")
            appendLine("<!-- Generated: ${timestamp()}                                          -->")
            appendLine("<!-- ══════════════════════════════════════════════════════════════════ -->")
            appendLine()
            appendLine("<!-- FIELDS -->")
            appendLine("""<!-- title         = "${s.title}" -->""")
            appendLine("""<!-- timeText      = "${s.timeText}" -->""")
            appendLine("""<!-- location      = "${s.location}" -->""")
            appendLine("""<!-- leaveBy       = "${s.leaveBy}" -->""")
            appendLine("""<!-- prepItems     = "${s.prepItems.replace("\n", "\\n")}" -->""")
            appendLine()
            appendLine("<!-- VISIBILITY -->")
            appendLine("<!-- showLocation   = ${s.showLocation} -->")
            appendLine("<!-- showLeaveBy    = ${s.showLeaveBy} -->")
            appendLine("<!-- showPrepItems  = ${s.showPrepItems} -->")
            appendLine()
            appendLine("<!-- BEHAVIOUR -->")
            appendLine("<!-- urgentMode     = ${s.urgentMode} -->")
            appendLine("<!-- forceHeadsUp   = ${s.forceHeadsUp} -->")
            appendLine()
            appendLine("<!-- STYLE -->")
            appendLine("<!-- selectedStyle  = ${s.selectedStyle.name} (${s.selectedStyle.displayName}) -->")
            if (s.selectedStyle == NotificationStyle.CUSTOM) {
                val c = s.customStyleConfig
                fun Int.hex() = "#${Integer.toHexString(this).uppercase().padStart(8, '0')}"
                appendLine()
                appendLine("<!-- CUSTOM STYLE CONFIG -->")
                appendLine("<!-- backgroundColor     = ${c.backgroundColor.hex()} -->")
                appendLine("<!-- accentColor         = ${c.accentColor.hex()} -->")
                appendLine("<!-- titleColor          = ${c.titleColor.hex()} -->")
                appendLine("<!-- titleTextSizeSp     = ${c.titleTextSizeSp} -->")
                appendLine("<!-- titleGravity        = ${c.titleGravity} -->")
                appendLine("<!-- titleBold           = ${c.titleBold} -->")
                appendLine("<!-- timeColor           = ${c.timeColor.hex()} -->")
                appendLine("<!-- timeTextSizeSp      = ${c.timeTextSizeSp} -->")
                appendLine("<!-- timeBodyGravity     = ${c.timeBodyGravity} -->")
                appendLine("""<!-- headerText          = "${c.headerText}" -->""")
                appendLine("<!-- headerTimeColor     = ${c.headerTimeColor.hex()} -->")
                appendLine("<!-- locationColor       = ${c.locationColor.hex()} -->")
                appendLine("<!-- leaveByColor        = ${c.leaveByColor.hex()} -->")
                appendLine("<!-- prepColor           = ${c.prepColor.hex()} -->")
                appendLine("<!-- dividerColor        = ${c.dividerColor.hex()} -->")
                appendLine("<!-- urgencyBadgeColor   = ${c.urgencyBadgeColor.hex()} -->")
                appendLine("""<!-- urgencyBadgeText    = "${c.urgencyBadgeText}" -->""")
                appendLine("<!-- iconDrawableRes     = ${c.iconDrawableRes} -->")
            }
            appendLine()
            appendLine("<!-- CHANNEL IDs -->")
            appendLine("<!-- normal  → ${NotificationChannelManager.STUDIO_CHANNEL_ID} -->")
            appendLine("<!-- urgent  → ${NotificationChannelManager.STUDIO_CHANNEL_ID_URGENT} -->")
        }
    }

    fun logEntry(message: String, level: LogLevel = LogLevel.INFO) {
        appendLog(message, level)
    }

    private fun buildParams(s: StudioState): NotificationLayoutFactory.NotificationParams {
        val wallClock = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return NotificationLayoutFactory.NotificationParams(
            title = s.title,
            timeText = s.timeText,
            location = s.location,
            leaveBy = s.leaveBy,
            prepItems = s.prepItems,
            showLocation = s.showLocation,
            showLeaveBy = s.showLeaveBy,
            showPrepItems = s.showPrepItems,
            urgentMode = s.urgentMode,
            simulate5MinLeft = s.simulate5MinLeft,
            currentTime = wallClock,
        )
    }

    private fun appendLog(message: String, level: LogLevel = LogLevel.INFO) {
        val entry = DebugLogEntry(timestamp = timestamp(), message = message, level = level)
        _state.update { it.copy(debugLogs = (listOf(entry) + it.debugLogs).take(50)) }
    }

    private fun timestamp(): String = timeFormat.format(Date())
}
