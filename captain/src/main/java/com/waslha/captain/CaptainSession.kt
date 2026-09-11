package com.waslha.captain

import android.content.Context

class CaptainSession(context: Context) {
    private val prefs = context.getSharedPreferences("waslha_captain", Context.MODE_PRIVATE)

    var userId: String?
        get() = prefs.getString("userId", null)
        private set(value) = prefs.edit().putString("userId", value).apply()

    var phone: String?
        get() = prefs.getString("phone", null)
        private set(value) = prefs.edit().putString("phone", value).apply()

    var token: String?
        get() = prefs.getString("token", null)
        private set(value) = prefs.edit().putString("token", value).apply()

    val isSignedIn: Boolean get() = !token.isNullOrBlank() && !userId.isNullOrBlank()

    fun save(session: VerifySessionResponse) {
        userId = session.userId
        phone = session.phone
        token = session.token
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
