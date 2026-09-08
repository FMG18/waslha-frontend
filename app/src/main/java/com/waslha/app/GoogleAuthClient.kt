package com.waslha.app

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

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

        val result = credentialManager.getCredential(context, request)
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
            error("بيانات اعتماد Google غير مدعومة")
        }
    }
}
