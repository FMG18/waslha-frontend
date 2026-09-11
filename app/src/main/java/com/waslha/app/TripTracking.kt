package com.waslha.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                val lat = driver?.lat
                val lng = driver?.lng
                val eta = trip.etaMinutes
                val distance = trip.distanceToPickupKm
                onUpdate(
                    TripTrackingSnapshot(
                        driverLocation = if (lat != null && lng != null) DriverLocation(lat, lng) else null,
                        etaMinutes = eta,
                        distanceToPickupKm = distance
                    )
                )
            }
            .onFailure { error -> onError(error.message ?: "تعذر تحديث موقع الكابتن") }
        delay(5000)
    }
}

fun estimateEtaMinutes(distanceKm: Double?): Int? {
    if (distanceKm == null || !distanceKm.isFinite()) return null
    return (distanceKm * 3.2 + 1).roundToInt().coerceAtLeast(1)
}
