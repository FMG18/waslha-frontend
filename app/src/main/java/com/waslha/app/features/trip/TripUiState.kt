package com.waslha.app.features.trip

enum class TripPhase {
    IDLE,
    PICKUP_SELECTED,
    DESTINATION_SELECTED,
    ESTIMATING,
    CONFIRMING,
    SEARCHING_CAPTAIN,
    CAPTAIN_ASSIGNED,
    CAPTAIN_ARRIVING,
    TRIP_STARTED,
    TRIP_COMPLETED,
    RATING,
    CANCELLED
}

data class TripUiState(
    val phase: TripPhase = TripPhase.IDLE,
    val pickupLabel: String = "موقعك الحالي",
    val destinationLabel: String = "",
    val vehicleType: String = "اقتصادي",
    val estimatedFare: Int = 0,
    val estimatedMinutes: Int = 0,
    val captainName: String = "",
    val captainRating: Double = 0.0,
    val vehicleName: String = "",
    val vehiclePlate: String = "",
    val errorMessage: String? = null
)
