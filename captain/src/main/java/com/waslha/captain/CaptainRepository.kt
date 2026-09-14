package com.waslha.captain

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.min

class CaptainRepository {
    private val api: CaptainApi
        get() = CaptainApiProvider.api

    suspend fun me(): ApiEnvelope<Driver> = retry { api.me() }

    suspend fun availability(available: Boolean): ApiEnvelope<Driver> =
        retry { api.availability(DriverAvailabilityRequest(available)) }

    suspend fun updateLocation(point: Coordinates): ApiEnvelope<Driver> =
        retry(maxAttempts = 2) { api.updateLocation(DriverLocationRequest(point.lat, point.lng)) }

    suspend fun availableTrips(): ApiEnvelope<List<Trip>> = retry { api.availableTrips() }

    suspend fun trips(): ApiEnvelope<List<Trip>> = retry { api.trips() }

    suspend fun trip(id: String): ApiEnvelope<Trip> = retry { api.trip(id) }

    suspend fun acceptTrip(id: String): ApiEnvelope<Trip> = retry { api.acceptTrip(id) }

    suspend fun updateTripStatus(id: String, status: String): ApiEnvelope<Trip> =
        retry { api.updateTripStatus(id, TripStatusRequest(status)) }

    suspend fun messages(id: String, after: Long = 0L): ApiEnvelope<List<TripMessage>> =
        retry { api.tripMessages(id, after) }

    suspend fun sendMessage(id: String, text: String): ApiEnvelope<TripMessage> =
        retry { api.sendTripMessage(id, TripMessageRequest(text)) }

    suspend fun rateTrip(request: DriverRatingRequest): ApiEnvelope<DriverRating> =
        retry(maxAttempts = 2) { api.createRating(request) }

    suspend fun notifications(userId: String): ApiEnvelope<List<CaptainNotification>> =
        retry { api.notifications(userId) }

    suspend fun registerDevice(token: String): ApiEnvelope<Map<String, Any>> =
        retry(maxAttempts = 2) { api.registerDevice(DeviceTokenRequest(token)) }

    private suspend fun <T> retry(maxAttempts: Int = 3, block: suspend () -> T): T {
        var attempt = 0
        var last: Throwable? = null
        while (attempt < maxAttempts) {
            attempt++
            try {
                return block()
            } catch (error: Throwable) {
                last = error
                if (!isRetryable(error) || attempt >= maxAttempts) throw error
                delay((350L * (1 shl (attempt - 1))).coerceAtMost(1800L))
            }
        }
        throw last ?: IllegalStateException("Network request failed")
    }

    private fun isRetryable(error: Throwable): Boolean {
        if (error is IOException) return true
        if (error is HttpException) return error.code() == 408 || error.code() == 429 || error.code() in 500..599
        return false
    }
}

data class DriverRatingRequest(
    val tripId: String,
    val customerId: String,
    val driverId: String,
    val score: Int,
    val comment: String = ""
)

data class DriverRating(
    val id: String,
    val tripId: String,
    val customerId: String,
    val driverId: String,
    val score: Int,
    val comment: String = "",
    val createdAt: Long = 0L
)

fun retryDelay(attempt: Int): Long = min(1800L, 350L * (1 shl attempt.coerceIn(0, 3)))
