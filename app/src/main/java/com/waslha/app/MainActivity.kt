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
            finish()
            return
        }

        startActivity(Intent(this, PassengerHomeV2Activity::class.java))
        finish()
    }
}
