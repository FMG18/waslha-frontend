package com.waslha.app

import android.content.Context
import android.content.Intent

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("waslha_session", Context.MODE_PRIVATE)

    val token: String? get() = prefs.getString("token", null)
    val userId: String? get() = prefs.getString("userId", null)
    val phone: String? get() = prefs.getString("phone", null)
    val email: String? get() = prefs.getString("email", null)
    val name: String? get() = prefs.getString("name", null)
    val picture: String? get() = prefs.getString("picture", null)
    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun save(session: SessionData) {
        prefs.edit()
            .putString("token", session.token)
            .putString("userId", session.userId)
            .putString("phone", session.phone)
            .putString("email", session.email)
            .putString("name", session.name)
            .putString("picture", session.picture)
            .apply()
    }

    fun updateProfile(name: String?, phone: String?, email: String?) {
        prefs.edit()
            .putString("name", name)
            .putString("phone", phone)
            .putString("email", email)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
        appContext.startActivity(
            Intent(appContext, AuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
    }
}
