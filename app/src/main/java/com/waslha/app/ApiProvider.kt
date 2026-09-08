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

private class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            sessionStore.token?.takeIf { it.isNotBlank() }?.let {
                header("Authorization", "Bearer $it")
            }
        }.build()
        return chain.proceed(request)
    }
}

object ApiProvider {
    private lateinit var apiInstance: WaslhaApi
    private var initialized = false

    val api: WaslhaApi
        get() {
            check(initialized) { "ApiProvider.init(context) must be called before ApiProvider.api" }
            return apiInstance
        }

    fun init(context: Context) {
        if (initialized) return

        val sessionStore = SessionStore(context.applicationContext)
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .addInterceptor(logger)
            .build()

        apiInstance = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaslhaApi::class.java)

        initialized = true
    }
}
