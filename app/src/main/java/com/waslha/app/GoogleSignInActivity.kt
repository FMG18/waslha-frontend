package com.waslha.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoogleSignInActivity : Activity() {
    companion object { private const val REQUEST_CODE = 9018 }
    private val signInScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serverClientId = getString(R.string.google_web_client_id).trim()
        if (serverClientId.isBlank() || serverClientId.startsWith("REPLACE_")) {
            finishWithError("تسجيل الدخول باستخدام Google غير مهيأ بعد")
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()
        try {
            startActivityForResult(GoogleSignIn.getClient(this, options).signInIntent, REQUEST_CODE)
        } catch (t: Throwable) {
            finishWithError(t.message ?: "تعذر فتح تسجيل Google")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) return
        if (data == null) {
            finishWithError("تم إغلاق نافذة Google قبل إكمال تسجيل الدخول")
            return
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                finishWithError("لم تُرجع Google رمز تسجيل الدخول")
                return
            }
            ApiProvider.init(this)
            val repository = AuthRepository(ApiProvider.api, SessionStore(this))
            signInScope.launch {
                try {
                    repository.signInWithGoogle(token)
                        .onSuccess { result -> finishWithResult(Result.success(result)) }
                        .onFailure { finishWithResult(Result.failure(it)) }
                } catch (t: Throwable) {
                    finishWithError(t.message ?: "تعذر إكمال تسجيل الدخول باستخدام Google")
                }
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                CommonStatusCodes.CANCELED -> "تم إلغاء تسجيل الدخول باستخدام Google"
                CommonStatusCodes.DEVELOPER_ERROR -> "إعداد Google غير صحيح أو بصمة APK غير مطابقة لـ Firebase"
                CommonStatusCodes.NETWORK_ERROR -> "تعذر الاتصال بخدمة Google"
                else -> "تعذر تسجيل الدخول باستخدام Google (رمز ${e.statusCode})"
            }
            finishWithError(message)
        } catch (t: Throwable) {
            finishWithError(t.message ?: "تعذر تسجيل الدخول باستخدام Google")
        }
    }

    override fun onDestroy() {
        signInScope.coroutineContext.cancel()
        super.onDestroy()
    }

    private fun finishWithError(message: String) =
        finishWithResult(Result.failure(IllegalStateException(message)))

    private fun finishWithResult(result: Result<SessionData>) {
        GoogleAuthClient.pendingResult?.invoke(result)
        GoogleAuthClient.pendingResult = null
        finish()
    }
}
