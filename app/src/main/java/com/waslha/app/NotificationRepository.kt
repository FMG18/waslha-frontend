package com.waslha.app

class NotificationRepository(private val api: WaslhaApi) {
    suspend fun list(userId: String): Result<List<NotificationDto>> = runCatching {
        require(userId.isNotBlank()) { "معرّف المستخدم مطلوب" }
        val response = api.notifications(userId)
        require(response.success && response.data != null) {
            response.message ?: "تعذر تحميل الإشعارات"
        }
        response.data
    }

    suspend fun markRead(userId: String, notificationId: String): Result<Boolean> = runCatching {
        require(userId.isNotBlank() && notificationId.isNotBlank()) { "بيانات الإشعار غير مكتملة" }
        val response = api.markNotificationRead(notificationId, MarkNotificationReadRequest(userId))
        require(response.success && response.data != null) {
            response.message ?: "تعذر تحديث الإشعار"
        }
        response.data.read
    }
}
