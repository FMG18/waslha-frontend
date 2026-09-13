package com.waslha.app

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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
    @POST("/api/v1/notifications/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ApiEnvelope<DeviceTokenResponse>

    @GET("/api/v1/customer/me")
    suspend fun customerMe(): ApiEnvelope<CustomerProfileDto>
    @PATCH("/api/v1/customer/me")
    suspend fun updateCustomer(@Body body: CustomerProfileUpdateRequest): ApiEnvelope<CustomerProfileDto>
    @DELETE("/api/v1/customer/me")
    suspend fun deleteCustomer(): ApiEnvelope<AccountDeleteDto>
    @GET("/api/v1/customer/wallet")
    suspend fun wallet(): ApiEnvelope<WalletDto>
    @GET("/api/v1/customer/places")
    suspend fun savedPlaces(): ApiEnvelope<List<SavedPlaceDto>>
    @PUT("/api/v1/customer/places/{slot}")
    suspend fun savePlace(@Path("slot") slot: String, @Body body: SavedPlaceRequest): ApiEnvelope<SavedPlaceDto>
    @DELETE("/api/v1/customer/places/{id}")
    suspend fun deletePlace(@Path("id") id: String): ApiEnvelope<PlaceDeleteDto>
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
data class DeviceTokenRequest(val token: String)
data class DeviceTokenResponse(val registered: Boolean, val tokenCount: Int = 0)

data class CustomerProfileDto(
    val id: String,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val picture: String = "",
    val walletBalance: Long = 0L,
    val currency: String = "SYP"
)
data class CustomerProfileUpdateRequest(val name: String? = null, val phone: String? = null, val picture: String? = null)
data class AccountDeleteDto(val deleted: Boolean)
data class WalletDto(val balance: Long = 0L, val currency: String = "SYP")
data class SavedPlaceDto(val id: String, val type: String, val name: String, val latitude: Double, val longitude: Double, val updatedAt: Long = 0L)
data class SavedPlaceRequest(val name: String, val latitude: Double, val longitude: Double)
data class PlaceDeleteDto(val deleted: Boolean)
