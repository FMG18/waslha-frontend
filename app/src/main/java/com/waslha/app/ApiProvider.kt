package com.waslha.app

import android.content.Context
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
    private var appContext: Context? = null
    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    lateinit var api: WaslhaApi
        private set

    fun init(context: Context) {
        if (::api.isInitialized) return
        appContext = context.applicationContext
        val session = SessionStore(appContext!!)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(logger)
            .build()
        api = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaslhaApi::class.java)
    }
}
