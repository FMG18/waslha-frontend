package com.waslha.app

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("waslha_session", Context.MODE_PRIVATE)

    val token: String? get() = prefs.getString("token", null)
    val userId: String? get() = prefs.getString("userId", null)
    val phone: String? get() = prefs.getString("phone", null)
    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun save(session: SessionData) {
        prefs.edit()
            .putString("token", session.token)
            .putString("userId", session.userId)
            .putString("phone", session.phone)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
