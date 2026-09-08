package com.waslha.app

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    const val BASE_URL = "https://waslha-backend.vercel.app/"
}

private class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.token
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}

object ApiProvider {
    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    fun create(context: android.content.Context): WaslhaApi {
        val session = SessionStore(context.applicationContext)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(logger)
            .build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaslhaApi::class.java)
    }
}
