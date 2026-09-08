package com.waslha.app.domain

import java.time.Instant

enum class TripStatus { DRAFT, SEARCHING, DRIVER_ASSIGNED, DRIVER_ARRIVING, DRIVER_ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED }

enum class PaymentMethodType { CASH, CARD, WALLET }

enum class NotificationType { TRIP, PAYMENT, PROMOTION, SECURITY, SYSTEM }

enum class SupportTicketStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }

data class PassengerProfile(
    val id: String,
    val name: String,
    val phone: String,
    val avatarUrl: String? = null,
    val rating: Double = 5.0
)

data class SavedPlace(
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val label: String,
    val maskedNumber: String? = null,
    val isDefault: Boolean = false
)

data class DriverSummary(
    val id: String,
    val name: String,
    val rating: Double,
    val carName: String,
    val plateNumber: String
)

data class TripSummary(
    val id: String,
    val pickupLabel: String,
    val destinationLabel: String,
    val estimatedFare: Long,
    val distanceKm: Double,
    val etaMinutes: Int,
    val status: TripStatus,
    val paymentMethod: PaymentMethodType,
    val driver: DriverSummary? = null,
    val createdAt: Instant? = null
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val read: Boolean = false,
    val createdAt: Instant? = null
)

data class SupportTicket(
    val id: String,
    val subject: String,
    val message: String,
    val status: SupportTicketStatus,
    val createdAt: Instant? = null
)

data class RatingDraft(
    val tripId: String,
    val stars: Int = 5,
    val comment: String = ""
)
