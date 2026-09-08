package com.waslha.app

class AuthRepository(private val api: WaslhaApi, private val sessionStore: SessionStore) {
    suspend fun requestCode(phone: String): Result<OtpResponse> = runCatching {
        val response = api.requestCode(OtpRequest(phone))
        require(response.success && response.data != null) { response.message ?: "تعذر إرسال رمز التحقق" }
        response.data
    }

    suspend fun verifyCode(phone: String, code: String): Result<SessionData> = runCatching {
        val response = api.verifyCode(VerifyOtpRequest(phone, code))
        require(response.success && response.data != null) { response.message ?: "رمز التحقق غير صحيح" }
        sessionStore.save(response.data)
        response.data
    }

    fun signOut() = sessionStore.clear()
}
