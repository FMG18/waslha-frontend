package com.waslha.app.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("waslha_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()

    fun clear() = prefs.edit().clear().apply()
}
