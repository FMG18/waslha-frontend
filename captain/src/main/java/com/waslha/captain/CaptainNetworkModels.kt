package com.waslha.captain

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private const val BASE_URL = "https://waslha-backend.vercel.app/"

data class ApiEnvelope<T>(val success: Boolean, val data: T? = null, val message: String? = null)
data class OtpRequest(val phone: String)
data class OtpResponse(val expiresIn: Int, val devCode: String? = null)
data class VerifyOtpRequest(val phone: String, val code: String)
data class VerifySessionResponse(val userId: String, val phone: String = "", val role: String = "", val token: String = "")
data class Driver(val id: String, val name: String = "", val rating: Double = 0.0, val vehicle: String = "", val plate: String = "", val type: String = "economy", val lat: Double = 0.0, val lng: Double = 0.0, val available: Boolean = false, val phone: String? = null)
data class Coordinates(val lat: Double, val lng: Double)
data class Trip(val id: String, val customerId: String = "", val pickup: Coordinates, val destination: Coordinates, val vehicleType: String = "economy", val paymentMethod: String = "cash", val distanceKm: Double = 0.0, val durationMin: Int = 0, val currency: String = "ل.س", val estimatedFare: Int = 0, val status: String = "searching", val driver: Driver? = null, val createdAt: Long = 0L, val updatedAt: Long = 0L)
data class DriverAvailabilityRequest(val available: Boolean)
data class TripStatusRequest(val status: String)
data class DeviceTokenRequest(val token: String)
data class CaptainNotification(val id: String, val title: String, val body: String, val tripId: String? = null, val read: Boolean = false, val createdAt: Long = 0L)

data class CaptainSessionData(val userId: String, val phone: String, val token: String, val role: String)

interface CaptainApi {
    @POST("api/v1/auth/request-code")
    suspend fun requestCode(@Body body: OtpRequest): ApiEnvelope<OtpResponse>

    @POST("api/v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyOtpRequest): ApiEnvelope<VerifySessionResponse>

    @GET("api/v1/captain/me")
    suspend fun me(): ApiEnvelope<Driver>

    @PATCH("api/v1/captain/availability")
    suspend fun availability(@Body body: DriverAvailabilityRequest): ApiEnvelope<Driver>

    @GET("api/v1/captain/trips/available")
    suspend fun availableTrips(): ApiEnvelope<List<Trip>>

    @GET("api/v1/captain/trips")
    suspend fun trips(): ApiEnvelope<List<Trip>>

    @POST("api/v1/captain/trips/{id}/accept")
    suspend fun acceptTrip(@Path("id") id: String): ApiEnvelope<Trip>

    @PATCH("api/v1/captain/trips/{id}/status")
    suspend fun updateTripStatus(@Path("id") id: String, @Body body: TripStatusRequest): ApiEnvelope<Trip>

    @GET("api/v1/captain/trips/{id}")
    suspend fun trip(@Path("id") id: String): ApiEnvelope<Trip>

    @GET("api/v1/notifications")
    suspend fun notifications(@Query("userId") userId: String, @Query("limit") limit: Int = 50): ApiEnvelope<List<CaptainNotification>>

    @POST("api/v1/notifications/device-token")
    suspend fun registerDevice(@Body body: DeviceTokenRequest): ApiEnvelope<Map<String, Any>>
}

object CaptainApiProvider {
    private var initialized = false
    lateinit var api: CaptainApi
        private set

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences("waslha_captain", Context.MODE_PRIVATE)
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
            prefs.getString("token", null)?.takeIf { it.isNotBlank() }?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
        val client = OkHttpClient.Builder().addInterceptor(authInterceptor).build()
        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CaptainApi::class.java)
        initialized = true
    }
}
