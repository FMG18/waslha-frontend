package com.waslha.captain

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CaptainApi {
    @POST("/api/v1/auth/request-code")
    suspend fun requestCode(@Body body: OtpRequest): ApiEnvelope<OtpResponse>

    @POST("/api/v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyOtpRequest): ApiEnvelope<VerifySessionResponse>

    @GET("/api/v1/trips")
    suspend fun trips(@Query("driverId") driverId: String? = null): ApiEnvelope<List<Trip>>

    @GET("/api/v1/trips/{id}")
    suspend fun trip(@Path("id") id: String): ApiEnvelope<Trip>

    @PATCH("/api/v1/trips/{id}/status")
    suspend fun updateTripStatus(@Path("id") id: String, @Body body: Map<String, String>): ApiEnvelope<Trip>

    @GET("/api/v1/drivers/{id}")
    suspend fun driver(@Path("id") id: String): ApiEnvelope<Driver>

    @PATCH("/api/v1/drivers/{id}/availability")
    suspend fun availability(@Path("id") id: String, @Body body: DriverAvailabilityRequest): ApiEnvelope<Driver>

    @PATCH("/api/v1/drivers/{id}/location")
    suspend fun location(@Path("id") id: String, @Body body: DriverLocationRequest): ApiEnvelope<Driver>
}

data class OtpResponse(val expiresIn: Int, val devCode: String? = null)

object CaptainApiProvider {
    private const val BASE_URL = "https://waslha-backend.vercel.app"

    val api: CaptainApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(CaptainApi::class.java)
    }
}
