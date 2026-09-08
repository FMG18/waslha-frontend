package com.waslha.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WaslhaApi {
    @POST("/api/v1/auth/request-code")
    suspend fun requestCode(@Body body: OtpRequest): ApiEnvelope<OtpResponse>

    @POST("/api/v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyOtpRequest): ApiEnvelope<VerifySessionResponse>

    @GET("/api/v1/catalog/vehicle-types")
    suspend fun vehicleTypes(): ApiEnvelope<List<VehicleTypeDto>>

    @GET("/api/v1/trips/estimate")
    suspend fun estimate(@Query("pickup") pickup: String): ApiEnvelope<FareEstimate>

    @GET("/api/v1/trips")
    suspend fun trips(@Query("customerId") customerId: String? = null): ApiEnvelope<List<Trip>>

    @POST("/api/v1/trips")
    suspend fun createTrip(@Body body: TripRequest): ApiEnvelope<Trip>

    @GET("/api/v1/trips/{id}")
    suspend fun trip(@Path("id") id: String): ApiEnvelope<Trip>

    @PATCH("/api/v1/trips/{id}/status")
    suspend fun updateTripStatus(@Path("id") id: String, @Body body: TripStatusRequest): ApiEnvelope<Trip>

    @POST("/api/v1/trips/{id}/cancel")
    suspend fun cancelTrip(@Path("id") id: String, @Body body: CancelTripRequest): ApiEnvelope<Trip>
}

data class VehicleTypeDto(val id: String, val name: String, val description: String, val seats: Int, val badge: String?)
data class TripStatusRequest(val status: String)
data class CancelTripRequest(val reason: String)
