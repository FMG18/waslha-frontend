package com.waslha.app.features.account

data class UserProfile(
    val displayName: String = "مستخدم وصلها",
    val phoneNumber: String = "",
    val email: String = "",
    val rating: Double = 5.0,
    val completedTrips: Int = 0
)

data class SavedPlace(
    val id: String,
    val title: String,
    val address: String
)

enum class PaymentMethodType { CASH, CARD, WALLET }

data class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val title: String,
    val subtitle: String,
    val isDefault: Boolean = false
)
