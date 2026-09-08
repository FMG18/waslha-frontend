package com.waslha.app.domain

data class GeoPoint(val latitude: Double, val longitude: Double)

enum class RideStatus { SEARCHING, DRIVER_ASSIGNED, ARRIVING, IN_PROGRESS, COMPLETED, CANCELLED }

data class VehicleType(
    val id: String,
    val name: String,
    val description: String,
    val seats: Int,
    val priceFrom: Int,
    val etaMin: Int,
    val etaMax: Int
)

data class Captain(
    val id: String,
    val name: String,
    val rating: Double,
    val vehicle: String,
    val plate: String,
    val type: String,
    val location: GeoPoint? = null,
    val phone: String? = null
)

data class Trip(
    val id: String,
    val customerId: String,
    val pickup: GeoPoint,
    val destination: GeoPoint,
    val vehicleType: String,
    val paymentMethod: String,
    val estimatedFare: Int,
    val currency: String,
    val status: RideStatus,
    val captain: Captain? = null,
    val distanceKm: Double = 0.0,
    val durationMin: Int = 0
)

data class FareEstimate(
    val vehicleType: String,
    val distanceKm: Double,
    val durationMin: Int,
    val estimatedFare: Int,
    val currency: String
)
