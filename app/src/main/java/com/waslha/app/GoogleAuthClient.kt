package com.waslha.app

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GoogleAuthClient(
    private val activity: Activity,
    private val repository: AuthRepository
) {
    companion object {
        internal var pendingResult: ((Result<SessionData>) -> Unit)? = null
    }

    suspend fun signIn(): Result<SessionData> = suspendCancellableCoroutine { continuation ->
        if (pendingResult != null) {
            continuation.resume(Result.failure(IllegalStateException("تسجيل الدخول قيد التنفيذ")))
            return@suspendCancellableCoroutine
        }
        val serverClientId = activity.getString(R.string.google_web_client_id).trim()
        if (serverClientId.isBlank() || serverClientId.startsWith("REPLACE_")) {
            continuation.resume(Result.failure(IllegalStateException("تسجيل الدخول باستخدام Google غير مهيأ بعد")))
            return@suspendCancellableCoroutine
        }
        pendingResult = { result -> if (continuation.isActive) continuation.resume(result) }
        continuation.invokeOnCancellation { pendingResult = null }
        try {
            activity.startActivity(Intent(activity, GoogleSignInActivity::class.java))
        } catch (t: Throwable) {
            pendingResult?.invoke(Result.failure(t)); pendingResult = null
        }
    }

    fun userMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
}
