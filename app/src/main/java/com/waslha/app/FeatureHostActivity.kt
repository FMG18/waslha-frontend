package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class FeatureHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "support"
        setContent {
            MaterialTheme {
                when (screen) {
                    "places" -> SavedPlacesScreen { finish() }
                    "payments" -> PaymentsScreen { finish() }
                    "notifications" -> NotificationsScreen { finish() }
                    "rating" -> RatingScreen { finish() }
                    else -> SupportScreen { finish() }
                }
            }
        }
    }
}
