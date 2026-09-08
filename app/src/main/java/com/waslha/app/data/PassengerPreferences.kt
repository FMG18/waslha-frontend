package com.waslha.app.data

data class PassengerPreferences(
    val language: String = "العربية",
    val darkMode: Boolean = false,
    val tripNotifications: Boolean = true,
    val generalNotifications: Boolean = true,
    val offersNotifications: Boolean = true,
    val locationPermissionExplained: Boolean = false
)

class PassengerPreferencesStore(initial: PassengerPreferences = PassengerPreferences()) {
    var preferences: PassengerPreferences = initial
        private set

    fun setDarkMode(enabled: Boolean) {
        preferences = preferences.copy(darkMode = enabled)
    }

    fun setTripNotifications(enabled: Boolean) {
        preferences = preferences.copy(tripNotifications = enabled)
    }

    fun setGeneralNotifications(enabled: Boolean) {
        preferences = preferences.copy(generalNotifications = enabled)
    }

    fun setOffersNotifications(enabled: Boolean) {
        preferences = preferences.copy(offersNotifications = enabled)
    }

    fun markLocationPermissionExplained() {
        preferences = preferences.copy(locationPermissionExplained = true)
    }
}
