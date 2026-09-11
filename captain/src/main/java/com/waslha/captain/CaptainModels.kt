package com.waslha.captain

data class ApiEnvelope<T>(val success: Boolean, val data: T? = null, val message: String? = null)
data class OtpRequest(val phone: String)
data class VerifyOtpRequest(val phone: String, val code: String)
data class VerifySessionResponse(val userId: String, val phone: String, val role: String, val token: String)
data class Coordinates(val lat: Double, val lng: Double)
data class Driver(
    val id: String,
    val name: String,
    val rating: Double = 5.0,
    val vehicle: String = "",
    val plate: String = "",
    val type: String = "economy",
    val lat: Double = 33.5138,
    val lng: Double = 36.2765,
    val available: Boolean = false,
    val phone: String? = null
)
data class Trip(
    val id: String,
    val customerId: String,
    val pickup: Coordinates,
    val destination: Coordinates,
    val vehicleType: String,
    val paymentMethod: String,
    val distanceKm: Double = 0.0,
    val durationMin: Int = 0,
    val currency: String = "ل.س",
    val estimatedFare: Int = 0,
    val status: String,
    val driver: Driver? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val customerName: String? = null,
    val customerPhone: String? = null
)
data class DriverListMeta(val count: Int = 0)
data class DriverAvailabilityRequest(val available: Boolean)
data class DriverLocationRequest(val lat: Double, val lng: Double)
data class AssignDriverRequest(val driverId: String)
data class TripDispatchResult(val trip: Trip, val assigned: Boolean = false)
data class DashboardStats(val trips: Int, val earnings: Int, val rating: Double)
