package com.waslha.app

import android.app.Application
import com.mapbox.common.MapboxOptions

class WaslhaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val token = getString(R.string.mapbox_access_token).trim()
        if (token.isNotBlank() && !token.startsWith("YOUR_")) {
            MapboxOptions.accessToken = token
        }
    }
}
