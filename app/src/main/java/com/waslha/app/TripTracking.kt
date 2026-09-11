package com.waslha.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DriverLocation(
    val lat: Double,
    val lng: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TripTrackingSnapshot(
    val driverLocation: DriverLocation? = null,
    val etaMinutes: Int? = null,
    val distanceToPickupKm: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

fun startTripTracking(
    scope: CoroutineScope,
    repository: TripRepository,
    tripId: String,
    onUpdate: (TripTrackingSnapshot) -> Unit,
    onError: (String) -> Unit = {}
): Job = scope.launch {
    while (isActive) {
        runCatching { repository.get(tripId).getOrThrow() }
            .onSuccess { trip ->
                val driver = trip.driver
                val location = driver?.let { DriverLocation(it.lat, it.lng) }
                val distance = location?.let { haversineKm(it, trip.pickup) }
                onUpdate(
                    TripTrackingSnapshot(
                        driverLocation = location,
                        etaMinutes = estimateEtaMinutes(distance),
                        distanceToPickupKm = distance
                    )
                )
            }
            .onFailure { error -> onError(error.message ?: "تعذر تحديث موقع الكابتن") }
        delay(5000)
    }
}

private fun haversineKm(from: DriverLocation, to: Coordinates): Double {
    val radiusKm = 6371.0
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val dLat = Math.toRadians(to.lat - from.lat)
    val dLng = Math.toRadians(to.lng - from.lng)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
    return radiusKm * 2 * atan2(sqrt(h), sqrt(1 - h))
}

fun estimateEtaMinutes(distanceKm: Double?): Int? {
    if (distanceKm == null || !distanceKm.isFinite()) return null
    return (distanceKm * 3.2 + 1).roundToInt().coerceAtLeast(1)
}
