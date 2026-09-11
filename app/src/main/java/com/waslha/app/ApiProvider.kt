package com.waslha.app

import android.content.Context
import java.io.IOException
import java.util.concurrent.TimeUnit
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
            sessionStore.token?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}

private class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastError: IOException? = null
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.code !in setOf(408, 429, 500, 502, 503, 504) || attempt == maxRetries) return response
                response.close()
            } catch (error: IOException) {
                lastError = error
                if (attempt == maxRetries) throw error
            }
            attempt++
            try { Thread.sleep(350L * attempt) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
        throw lastError ?: IOException("Network request failed")
    }
}

object ApiProvider {
    private lateinit var apiInstance: WaslhaApi
    private lateinit var appContext: Context
    private var initialized = false

    val api: WaslhaApi
        get() { check(initialized) { "ApiProvider.init(context) must be called before ApiProvider.api" }; return apiInstance }
    val context: Context
        get() { check(initialized) { "ApiProvider.init(context) must be called before ApiProvider.context" }; return appContext }
    val session: SessionStore get() = SessionStore(context)

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        val sessionStore = SessionStore(appContext)
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionStore))
            .addInterceptor(RetryInterceptor())
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
