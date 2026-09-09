package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Stable authenticated entry point.
 * The full taxi experience lives in SafeTaxiActivity so the authentication
 * flow remains isolated from location, trip and UI initialization.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(this)
        if (!sessionStore.isSignedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        startActivity(Intent(this, SafeTaxiActivity::class.java))
        finish()
    }
}
