package com.waslha.admin

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

private const val BASE_URL = "https://waslha-backend.vercel.app/"

data class Envelope<T>(val success: Boolean, val data: T? = null, val message: String? = null)
data class CodeRequest(val phone: String)
data class CodeResponse(val expiresIn: Int = 300, val devCode: String? = null)
data class VerifyRequest(val phone: String, val code: String)
data class Session(val userId: String, val phone: String = "", val role: String = "", val token: String = "", val name: String = "")
data class AdminTotals(val trips: Int = 0, val activeTrips: Int = 0, val waitingTrips: Int = 0, val completedTrips: Int = 0, val drivers: Int = 0, val onlineDrivers: Int = 0, val revenue: Int = 0)
data class AdminTripDto(val id: String, val status: String = "", val customerId: String = "", val estimatedFare: Int = 0, val currency: String = "ل.س", val driver: AdminDriverDto? = null)
data class AdminDriverDto(val id: String, val name: String = "", val type: String = "economy", val available: Boolean = false, val rating: Double = 0.0)
data class AdminOverview(val totals: AdminTotals, val latestTrips: List<AdminTripDto> = emptyList())

interface AdminApi {
    @POST("api/v1/auth/request-code") suspend fun requestCode(@Body body: CodeRequest): Envelope<CodeResponse>
    @POST("api/v1/auth/verify-code") suspend fun verifyCode(@Body body: VerifyRequest): Envelope<Session>
    @GET("api/v1/admin/overview") suspend fun overview(): Envelope<AdminOverview>
    @GET("api/v1/admin/trips") suspend fun trips(): Envelope<List<AdminTripDto>>
    @GET("api/v1/admin/drivers") suspend fun drivers(): Envelope<List<AdminDriverDto>>
}

object AdminApiProvider {
    private var initialized = false
    lateinit var api: AdminApi
        private set

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences("waslha_admin", Context.MODE_PRIVATE)
        val interceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
            prefs.getString("token", null)?.takeIf { it.isNotBlank() }?.let { request.header("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        api = Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(AdminApi::class.java)
        initialized = true
    }
}

class AdminSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("waslha_admin", Context.MODE_PRIVATE)
    val signedIn get() = !prefs.getString("token", null).isNullOrBlank()
    fun save(session: Session) { prefs.edit().putString("token", session.token).putString("userId", session.userId).putString("phone", session.phone).putString("name", session.name).putString("role", session.role).apply() }
    fun clear() { prefs.edit().clear().apply() }
    val name get() = prefs.getString("name", "مدير وصلها") ?: "مدير وصلها"
}
