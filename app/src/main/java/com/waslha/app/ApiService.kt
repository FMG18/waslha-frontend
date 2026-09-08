package com.waslha.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/request-code")
    suspend fun requestCode(@Body request: OtpRequest): ApiEnvelope<OtpResponse>

    @POST("api/v1/auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyOtpRequest): ApiEnvelope<SessionData>

    @GET("api/v1/catalog/vehicle-types")
    suspend fun vehicleTypes(): ApiEnvelope<List<VehicleTypeDto>>

    @GET("api/v1/trips/estimate")
    suspend fun estimate(@Query("pickup") pickup: String): ApiEnvelope<FareEstimate>

    @POST("api/v1/trips")
    suspend fun createTrip(@Body request: TripRequest): ApiEnvelope<Trip>

    @GET("api/v1/trips/{id}")
    suspend fun trip(@Path("id") id: String): ApiEnvelope<Trip>

    @GET("api/v1/trips")
    suspend fun trips(@Query("customerId") customerId: String): ApiEnvelope<List<Trip>>
}

data class VehicleTypeDto(
    val id: String,
    val name: String,
    val description: String,
    val seats: Int,
    val badge: String
)
