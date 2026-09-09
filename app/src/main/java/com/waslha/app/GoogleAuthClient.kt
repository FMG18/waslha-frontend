package com.waslha.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.withTimeout

class GoogleAuthClient(
    private val context: Context,
    private val repository: AuthRepository
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): Result<SessionData> = runCatching {
        val serverClientId = context.getString(R.string.google_web_client_id).trim()
        require(serverClientId.isNotBlank() && !serverClientId.startsWith("REPLACE_")) {
            "تسجيل الدخول باستخدام Google غير مهيأ بعد"
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val activityContext = context as? Activity
            ?: error("تعذر فتح نافذة Google من سياق التطبيق الحالي")

        val result = withTimeout(30_000L) {
            credentialManager.getCredential(
                context = activityContext,
                request = request
            )
        }

        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (_: GoogleIdTokenParsingException) {
                error("تعذر قراءة بيانات حساب Google")
            }

            val idToken = googleCredential.idToken
            require(idToken.isNotBlank()) { "تعذر الحصول على Google ID Token" }
            repository.signInWithGoogle(idToken).getOrThrow()
        } else {
            error("لم يتم اختيار حساب Google صالح")
        }
    }

    fun userMessage(error: Throwable): String = when (error) {
        is NoCredentialException -> "لم يتم العثور على حساب Google متاح. تأكد من إضافة حساب Google إلى الهاتف."
        is GetCredentialCancellationException -> "تم إلغاء تسجيل الدخول باستخدام Google"
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
    }
}
