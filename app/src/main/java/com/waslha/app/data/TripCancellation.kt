package com.waslha.app.data

enum class TripCancellationReason(val label: String) {
    CHANGED_PLANS("غيّرت خطتي"),
    WAIT_TOO_LONG("وقت الانتظار طويل"),
    WRONG_PICKUP("موقع الانطلاق غير صحيح"),
    WRONG_DESTINATION("الوجهة غير صحيحة"),
    DRIVER_ISSUE("مشكلة مع الكابتن"),
    PRICE_ISSUE("السعر غير مناسب"),
    OTHER("سبب آخر")
}

data class CancellationRequest(
    val tripId: String,
    val reason: TripCancellationReason,
    val note: String = ""
)

data class TripReceipt(
    val tripId: String,
    val pickup: String,
    val destination: String,
    val carType: String,
    val paymentMethod: PaymentMethod,
    val subtotal: Int,
    val serviceFee: Int,
    val total: Int
)
