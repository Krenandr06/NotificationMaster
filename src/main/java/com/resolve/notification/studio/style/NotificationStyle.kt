package com.resolve.notification.studio.style

/**
 * Defines the available visual styles for the Notification Tailoring Studio.
 */
enum class NotificationStyle(
    val displayName: String,
    val description: String,
) {
    MINIMAL(
        displayName = "Minimal",
        description = "Clean & low-distraction",
    ),
    SYSTEM(
        displayName = "System",
        description = "Resolve Void aesthetic",
    ),
    RESOLVE_PREMIUM(
        displayName = "Resolve Premium",
        description = "Full branded premium",
    ),
    URGENT_ALERT(
        displayName = "Urgent Alert",
        description = "High-visibility critical",
    ),
    CUSTOM(
        displayName = "Custom",
        description = "Fully customisable",
    ),
}
