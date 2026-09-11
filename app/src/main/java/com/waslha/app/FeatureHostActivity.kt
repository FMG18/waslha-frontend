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
                    "places" -> ProSavedPlacesScreen { finish() }
                    "payments" -> ProPaymentsScreen { finish() }
                    "notifications" -> ProNotificationsScreen { finish() }
                    "rating" -> ProRatingScreen { finish() }
                    "security" -> ProSecurityScreen { finish() }
                    "about" -> ProAboutScreen { finish() }
                    else -> ProSupportScreen { finish() }
                }
            }
        }
    }
}
