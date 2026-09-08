package com.waslha.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object ApiConfig {
    // Replace with the deployed Waslha API before release.
    const val BASE_URL = "https://waslha-api.example.com/api/v1/"
}

data class PhoneRequest(val phone: String)
data class VerifyRequest(val phone: String, val code: String)
data class AuthUser(val id: String, val phone: String, val role: String)
data class AuthData(val user: AuthUser, val token: String)
data class ApiResponse<T>(val success: Boolean, val message: String? = null, val data: T? = null)
data class LocationPoint(val lat: Double, val lng: Double, val label: String? = null, val distanceKm: Double? = null)
data class TripRequest(val customerId: String, val pickup: LocationPoint, val destination: LocationPoint, val vehicleType: String = "economy", val paymentMethod: String = "cash")
data class Driver(val id: String, val name: String, val rating: Double, val vehicle: String, val plate: String, val type: String, val lat: Double, val lng: Double, val available: Boolean)
data class FareEstimate(val distanceKm: Double, val durationMin: Int, val currency: String, val estimatedFare: Int)
data class Trip(val id: String, val customerId: String, val pickup: LocationPoint, val destination: LocationPoint, val vehicleType: String, val paymentMethod: String, val distanceKm: Double, val durationMin: Int, val estimatedFare: Int, val currency: String, val status: String, val driver: Driver?)

interface WaslhaApi {
    @POST("auth/request-code")
    suspend fun requestCode(@Body body: PhoneRequest): ApiResponse<Any>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyRequest): ApiResponse<AuthData>

    @GET("catalog/vehicle-types")
    suspend fun vehicleTypes(): ApiResponse<List<Map<String, Any>>>

    @GET("drivers/nearby")
    suspend fun nearbyDrivers(@Query("vehicleType") vehicleType: String? = null): ApiResponse<List<Driver>>

    @POST("trips")
    suspend fun createTrip(@Body body: TripRequest): ApiResponse<Trip>

    @GET("trips")
    suspend fun trips(@Query("customerId") customerId: String): ApiResponse<List<Trip>>

    @GET("trips/estimate")
    suspend fun estimate(@Query("pickup") pickup: String): ApiResponse<FareEstimate>
}

object WaslhaApiFactory {
    fun create(): WaslhaApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaslhaApi::class.java)
    }
}
