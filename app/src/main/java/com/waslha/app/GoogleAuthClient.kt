package com.waslha.app

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

            val credential = try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setNonce(nonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                credentialManager.getCredential(
                    context = activityContext,
                    request = request
                ).credential
            } catch (_: NoCredentialException) {
                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .setNonce(nonce)
                    .build()

                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()

                credentialManager.getCredential(
                    context = activityContext,
                    request = fallbackRequest
                ).credential
            }

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
        is NoCredentialException -> "تعذر فتح تسجيل Google. تأكد من تحديث Google Play services ثم حاول مرة أخرى."
        is GetCredentialCancellationException -> "تم إلغاء تسجيل الدخول باستخدام Google"
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
    }
}
