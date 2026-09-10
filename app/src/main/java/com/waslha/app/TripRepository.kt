package com.waslha.app

class TripRepository(private val api: WaslhaApi) {
    suspend fun create(request: TripRequest): Result<Trip> = runCatching {
        val response = api.createTrip(request)
        require(response.success && response.data != null) { response.message ?: "تعذر إنشاء الرحلة" }
        response.data
    }

    suspend fun get(id: String): Result<Trip> = runCatching {
        val response = api.trip(id)
        require(response.success && response.data != null) { response.message ?: "تعذر جلب الرحلة" }
        response.data
    }

    suspend fun list(customerId: String? = null): Result<List<Trip>> = runCatching {
        val response = api.trips(customerId)
        require(response.success && response.data != null) { response.message ?: "تعذر جلب الرحلات" }
        response.data
    }

    suspend fun estimate(
        pickup: Coordinates,
        destination: Coordinates,
        vehicleType: String = "economy"
    ): Result<FareEstimate> = runCatching {
        val payload = """
            {"lat":${pickup.lat},"lng":${pickup.lng},"destination":{"lat":${destination.lat},"lng":${destination.lng}},"vehicleType":"$vehicleType"}
        """.trimIndent()
        val response = api.estimate(payload)
        require(response.success && response.data != null) { response.message ?: "تعذر حساب الأجرة" }
        response.data
    }

    suspend fun updateStatus(id: String, status: String): Result<Trip> = runCatching {
        val response = api.updateTripStatus(id, TripStatusRequest(status))
        require(response.success && response.data != null) { response.message ?: "تعذر تحديث حالة الرحلة" }
        response.data
    }

    suspend fun cancel(id: String, reason: String): Result<Trip> = runCatching {
        val response = api.cancelTrip(id, CancelTripRequest(reason))
        require(response.success && response.data != null) { response.message ?: "تعذر إلغاء الرحلة" }
        response.data
    }
}

fun Trip.statusLabel(): String = when (status) {
    "searching" -> "جاري البحث عن كابتن"
    "driver_assigned" -> "تم العثور على كابتن"
    "arriving" -> "الكابتن في الطريق إليك"
    "in_progress" -> "الرحلة بدأت"
    "completed" -> "انتهت الرحلة"
    "cancelled" -> "تم إلغاء الرحلة"
    else -> "حالة غير معروفة"
}
