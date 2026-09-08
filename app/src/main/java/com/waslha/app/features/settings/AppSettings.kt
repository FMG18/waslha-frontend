package com.waslha.app.features.settings

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val tripNotificationsEnabled: Boolean = true,
    val promotionalNotificationsEnabled: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val locationPermissionExplained: Boolean = false,
    val preferredLanguage: String = "ar"
)

class SettingsStore {
    private var settings = AppSettings()

    fun read(): AppSettings = settings

    fun update(next: AppSettings) {
        settings = next
    }

    fun setNotifications(enabled: Boolean) {
        settings = settings.copy(notificationsEnabled = enabled)
    }

    fun setTripNotifications(enabled: Boolean) {
        settings = settings.copy(tripNotificationsEnabled = enabled)
    }

    fun setPromotionalNotifications(enabled: Boolean) {
        settings = settings.copy(promotionalNotificationsEnabled = enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        settings = settings.copy(darkModeEnabled = enabled)
    }
}
