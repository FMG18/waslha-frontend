package com.waslha.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class FeatureHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = AndroidColor.WHITE
        window.navigationBarColor = AndroidColor.WHITE
        window.navigationBarDividerColor = AndroidColor.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val screen = intent.getStringExtra("screen") ?: "support"
        val session = SessionStore(this)
        setContent {
            WaslhaTheme {
                when (screen) {
                    "account" -> AccountCenterScreen(session) { finish() }
                    "settings" -> SettingsCenterScreen { finish() }
                    "places" -> SavedPlacesScreen { finish() }
                    "payments" -> PaymentsScreen { finish() }
                    "notifications" -> NotificationsScreen { finish() }
                    "rating" -> RatingScreen { finish() }
                    "security" -> SecurityScreen { finish() }
                    "about" -> CurrentAboutScreen { finish() }
                    else -> SupportScreen { finish() }
                }
            }
        }
    }
}
