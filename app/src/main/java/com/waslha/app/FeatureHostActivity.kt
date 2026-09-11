package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class FeatureHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "support"
        val session = SessionStore(this)
        setContent {
            MaterialTheme {
                when (screen) {
                    "account" -> AccountCenterScreen(session) { finish() }
                    "settings" -> SettingsCenterScreen { finish() }
                    "places" -> SavedPlacesScreen { finish() }
                    "payments" -> PaymentsScreen { finish() }
                    "notifications" -> NotificationsScreen { finish() }
                    "rating" -> RatingScreen { finish() }
                    "security" -> SecurityScreen { finish() }
                    "about" -> AboutScreen { finish() }
                    else -> SupportScreen { finish() }
                }
            }
        }
    }
}
