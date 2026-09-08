package com.waslha.app.domain

data class PassengerSettings(
    val language: String = "ar",
    val darkMode: Boolean = false,
    val tripNotifications: Boolean = true,
    val marketingNotifications: Boolean = false,
    val locationExplainerSeen: Boolean = false
)

data class SafetyOptions(
    val shareTripEnabled: Boolean = true,
    val trustedContactEnabled: Boolean = false,
    val emergencyShortcutEnabled: Boolean = true
)
