package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Stable authenticated entry point for the customer app. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(this)
        val target = if (sessionStore.isSignedIn) {
            if (!sessionStore.activeTripId.isNullOrBlank()) {
                TripResumeActivity::class.java
            } else {
                WaslhaCustomerActivity::class.java
            }
        } else {
            AuthActivity::class.java
        }

        startActivity(Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
