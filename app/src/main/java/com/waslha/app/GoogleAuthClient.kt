package com.waslha.app

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GoogleAuthClient(
    private val activity: Activity,
    private val repository: AuthRepository
) {
    companion object { const val REQUEST_CODE = 9017 }

    private val client: GoogleSignInClient
    private var pending: ((Result<SessionData>) -> Unit)? = null

    init {
        val serverClientId = activity.getString(R.string.google_web_client_id).trim()
        require(serverClientId.isNotBlank() && !serverClientId.startsWith("REPLACE_")) {
            "تسجيل الدخول باستخدام Google غير مهيأ بعد"
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()
        client = GoogleSignIn.getClient(activity, options)
    }

    suspend fun signIn(): Result<SessionData> = suspendCancellableCoroutine { continuation ->
        if (pending != null) {
            continuation.resume(Result.failure(IllegalStateException("تسجيل الدخول قيد التنفيذ")))
            return@suspendCancellableCoroutine
        }
        pending = { result -> if (continuation.isActive) continuation.resume(result) }
        continuation.invokeOnCancellation { pending = null }
        try {
            client.signOut().addOnCompleteListener {
                if (continuation.isActive) activity.startActivityForResult(client.signInIntent, REQUEST_CODE)
            }
        } catch (t: Throwable) {
            pending?.invoke(Result.failure(t)); pending = null
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) return
        val callback = pending ?: return
        pending = null
        if (data == null) {
            callback(Result.failure(IllegalStateException("تم إغلاق نافذة Google قبل إكمال تسجيل الدخول")))
            return
        }
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                callback(Result.failure(IllegalStateException("لم تُرجع Google رمز تسجيل الدخول")))
            } else {
                callback(runCatching { repository.signInWithGoogle(idToken).getOrThrow() })
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                CommonStatusCodes.CANCELED -> "تم إلغاء تسجيل الدخول باستخدام Google"
                CommonStatusCodes.DEVELOPER_ERROR -> "إعداد Google للتطبيق غير صحيح أو بصمة APK غير مطابقة."
                CommonStatusCodes.NETWORK_ERROR -> "تعذر الاتصال بخدمة Google. تحقق من الإنترنت."
                else -> "تعذر تسجيل الدخول باستخدام Google (رمز ${e.statusCode})"
            }
            callback(Result.failure(IllegalStateException(message, e)))
        } catch (e: Throwable) {
            callback(Result.failure(e))
        }
    }

    fun userMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
}
