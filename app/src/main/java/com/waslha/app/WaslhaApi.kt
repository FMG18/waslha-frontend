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
    @POST("/api/v1/auth/google")
    suspend fun signInWithGoogle(@Body body: GoogleAuthRequest): ApiEnvelope<GoogleSessionResponse>
    @GET("/api/v1/update")
    suspend fun latestUpdate(@Query("currentCode") currentCode: Int, @Query("abi") abi: String): ApiEnvelope<AppUpdateDto>
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
    @GET("/api/v1/trips/{id}/tracking")
    suspend fun tripTracking(@Path("id") id: String): ApiEnvelope<TripTrackingDto>
    @PATCH("/api/v1/trips/{id}/status")
    suspend fun updateTripStatus(@Path("id") id: String, @Body body: TripStatusRequest): ApiEnvelope<Trip>
    @POST("/api/v1/trips/{id}/cancel")
    suspend fun cancelTrip(@Path("id") id: String, @Body body: CancelTripRequest): ApiEnvelope<Trip>
    @GET("/api/v1/places/search")
    suspend fun searchPlaces(@Query("q") query: String): ApiEnvelope<List<PlaceSearchDto>>
    @GET("/api/v1/notifications")
    suspend fun notifications(@Query("userId") userId: String, @Query("limit") limit: Int = 50): ApiEnvelope<List<NotificationDto>>
    @PATCH("/api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String, @Body body: MarkNotificationReadRequest): ApiEnvelope<NotificationReadDto>
}

data class AppUpdateDto(val updateAvailable: Boolean, val versionName: String = "", val versionCode: Int = 0, val releaseNotes: String = "", val publishedAt: String? = null, val mandatory: Boolean = false, val apk: AppUpdateApkDto? = null)
data class AppUpdateApkDto(val name: String, val size: Long, val url: String, val sha256: String? = null)
data class VehicleTypeDto(val id: String, val name: String, val description: String, val seats: Int, val badge: String?)
data class TripStatusRequest(val status: String)
data class CancelTripRequest(val reason: String)
data class TripTrackingDto(val tripId: String, val status: String, val driver: Driver? = null, val etaMinutes: Int? = null, val distanceToPickupKm: Double? = null, val updatedAt: Long = 0L)
data class PlaceSearchDto(val id: String, val name: String, val address: String, val coordinates: Coordinates)
data class NotificationDto(val id: String, val title: String, val body: String, val type: String = "trip", val tripId: String? = null, val read: Boolean = false, val createdAt: Long = 0L)
data class MarkNotificationReadRequest(val userId: String)
data class NotificationReadDto(val id: String, val read: Boolean)
