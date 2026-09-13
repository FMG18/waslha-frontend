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
    val walletBalance: Long get() = prefs.getLong("walletBalance", 0L)
    val activeTripId: String? get() = prefs.getString("activeTripId", null)
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

    fun updateProfile(nameValue: String?, phoneValue: String?, emailValue: String? = null, pictureValue: String? = null, walletBalanceValue: Long? = null) {
        val editor = prefs.edit()
            .putString("name", nameValue)
            .putString("phone", phoneValue)
            .putString("email", emailValue ?: email)
            .putString("picture", pictureValue ?: picture)
        if (walletBalanceValue != null) editor.putLong("walletBalance", walletBalanceValue.coerceAtLeast(0L))
        editor.apply()
    }

    fun updateWallet(balance: Long) {
        prefs.edit().putLong("walletBalance", balance.coerceAtLeast(0L)).apply()
    }

    fun setActiveTrip(tripId: String) {
        prefs.edit().putString("activeTripId", tripId).apply()
    }

    fun clearActiveTrip() {
        prefs.edit().remove("activeTripId").apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
        appContext.startActivity(Intent(appContext, AuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}
