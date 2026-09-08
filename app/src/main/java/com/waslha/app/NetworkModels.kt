package com.waslha.app

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(val success: Boolean, val data: T? = null, val message: String? = null)
data class OtpRequest(val phone: String)
data class OtpResponse(val expiresIn: Int, val devCode: String? = null)
data class VerifyOtpRequest(val phone: String, val code: String)

data class VerifySessionResponse(
    val userId: String,
    val phone: String,
    val role: String,
    val token: String
)

data class SessionData(val userId: String, val phone: String, val role: String, val token: String)
data class Coordinates(val lat: Double, val lng: Double)
data class TripRequest(val customerId: String, val pickup: Coordinates, val destination: Coordinates, val vehicleType: String = "economy", val paymentMethod: String = "cash", val scheduledAt: Long? = null)
data class FareEstimate(val distanceKm: Double, val durationMin: Int, val currency: String, val estimatedFare: Int)
data class Driver(val id: String, val name: String, val rating: Double, val vehicle: String, val plate: String, val type: String, val lat: Double, val lng: Double, val available: Boolean)
data class Trip(val id: String, val customerId: String, val pickup: Coordinates, val destination: Coordinates, val vehicleType: String, val paymentMethod: String, val distanceKm: Double, val durationMin: Int, val currency: String, val estimatedFare: Int, val status: String, val driver: Driver? = null, val createdAt: Long, val updatedAt: Long, @SerializedName("cancelReason") val cancelReason: String? = null)
data class RatingRequest(val tripId: String, val customerId: String, val driverId: String, val score: Int, val comment: String = "")
