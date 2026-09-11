package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Stable authenticated entry point. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(this)
        if (!sessionStore.isSignedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

        val target = if (!sessionStore.activeTripId.isNullOrBlank()) TripResumeActivity::class.java else TaxiBookingActivity::class.java
        startActivity(Intent(this, target))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
