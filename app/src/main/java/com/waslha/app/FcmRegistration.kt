package com.waslha.app

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmRegistration {
    private const val TAG = "WaslhaFCM"
    private const val PREFS = "waslha_fcm"
    private const val LAST_TOKEN = "last_token"

    fun register(context: Context) {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> sendToken(context.applicationContext, token) }
                .addOnFailureListener { Log.w(TAG, "Unable to obtain FCM token", it) }
        }.onFailure { Log.w(TAG, "FCM is unavailable", it) }
    }

    fun sendToken(context: Context, token: String) {
        if (token.isBlank()) return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(LAST_TOKEN, null) == token) return
        val session = SessionStore(app)
        if (!session.isSignedIn) return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ApiProvider.init(app)
                ApiProvider.api.registerDeviceToken(DeviceTokenRequest(token))
            }.onSuccess { response ->
                if (response.success) prefs.edit().putString(LAST_TOKEN, token).apply()
                else Log.w(TAG, "Backend rejected FCM token: ${response.message}")
            }.onFailure { Log.w(TAG, "Failed to register FCM token", it) }
        }
    }
}
