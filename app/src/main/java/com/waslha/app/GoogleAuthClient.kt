package com.waslha.app

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import kotlinx.coroutines.withTimeout

class GoogleAuthClient(
    private val context: Context,
    private val repository: AuthRepository
) {
    private val credentialManager = CredentialManager.create(context)
    private val secureRandom = SecureRandom()

    suspend fun signIn(): Result<SessionData> = runCatching {
        withTimeout(30_000L) {
            val serverClientId = context.getString(R.string.google_web_client_id).trim()
            require(serverClientId.isNotBlank() && !serverClientId.startsWith("REPLACE_")) {
                "تسجيل الدخول باستخدام Google غير مهيأ بعد"
            }

            val activityContext = context as? Activity
                ?: error("تعذر فتح نافذة Google من سياق التطبيق الحالي")

            val nonceBytes = ByteArray(32).also(secureRandom::nextBytes)
            val nonce = Base64.encodeToString(
                nonceBytes,
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
            )

            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val credential = credentialManager.getCredential(
                context = activityContext,
                request = request
            ).credential

            require(
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "لم يتم اختيار حساب Google صالح" }

            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (_: GoogleIdTokenParsingException) {
                error("تعذر قراءة بيانات حساب Google")
            }

            val idToken = googleCredential.idToken
            require(idToken.isNotBlank()) { "تعذر الحصول على Google ID Token" }
            repository.signInWithGoogle(idToken).getOrThrow()
        }
    }

    fun userMessage(error: Throwable): String = when (error) {
        is NoCredentialException -> "تعذر فتح تسجيل Google. تأكد من وجود حساب Google وتحديث Google Play services ثم حاول مرة أخرى."
        is GetCredentialCancellationException -> "تم إغلاق نافذة Google قبل إكمال تسجيل الدخول"
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
    }
}
