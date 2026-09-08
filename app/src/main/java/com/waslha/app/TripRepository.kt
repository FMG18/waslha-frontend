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
