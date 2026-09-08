package com.waslha.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WaslhaApi {
    @POST("/api/v1/auth/request-code")
    suspend fun requestCode(@Body body: OtpRequest): ApiEnvelope<OtpResponse>

    @POST("/api/v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyOtpRequest): ApiEnvelope<SessionData>

    @GET("/api/v1/catalog/vehicle-types")
    suspend fun vehicleTypes(): ApiEnvelope<List<VehicleTypeDto>>

    @POST("/api/v1/trips")
    suspend fun createTrip(@Body body: TripRequest): ApiEnvelope<Trip>

    @GET("/api/v1/trips/{id}")
    suspend fun trip(@Path("id") id: String): ApiEnvelope<Trip>
}

data class VehicleTypeDto(val id: String, val name: String, val description: String, val seats: Int, val badge: String?)
