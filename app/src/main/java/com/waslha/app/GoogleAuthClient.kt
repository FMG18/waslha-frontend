package com.waslha.app

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class GoogleAuthClient(
    private val activity: Activity,
    private val repository: AuthRepository
) {
    suspend fun signIn(): Result<SessionData> {
        return try {
            val serverClientId = activity.getString(R.string.google_web_client_id).trim()
            if (serverClientId.isBlank() || serverClientId.startsWith("REPLACE_")) {
                return Result.failure(IllegalStateException("تسجيل الدخول باستخدام Google غير مهيأ بعد"))
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = CredentialManager.create(activity).getCredential(
                context = activity,
                request = request
            )

            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(response.credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                return Result.failure(IllegalStateException("تعذر قراءة بيانات حساب Google", e))
            }

            val token = googleCredential.idToken.trim()
            if (token.isBlank()) {
                return Result.failure(IllegalStateException("لم تُرجع Google رمز تسجيل الدخول"))
            }

            repository.signInWithGoogle(token)
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Result.failure(IllegalStateException("تم إلغاء تسجيل الدخول باستخدام Google", e))
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Result.failure(IllegalStateException("لم يتم العثور على حساب Google متاح لتسجيل الدخول", e))
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Result.failure(IllegalStateException("تعذر تسجيل الدخول باستخدام Google: ${e.message ?: "خطأ في خدمة Google"}", e))
        } catch (t: Throwable) {
            Result.failure(IllegalStateException("تعذر تسجيل الدخول باستخدام Google: ${t.message ?: "خطأ غير معروف"}", t))
        }
    }

    fun userMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() }
            ?: "تعذر تسجيل الدخول باستخدام Google. حاول مرة أخرى."
}
