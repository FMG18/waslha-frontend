package com.waslha.app.data

enum class TripStage {
    IDLE,
    SELECTING_DESTINATION,
    CONFIRMING_RIDE,
    SEARCHING_CAPTAIN,
    CAPTAIN_ASSIGNED,
    CAPTAIN_ARRIVING,
    TRIP_STARTED,
    TRIP_FINISHED,
    RATING,
    COMPLETED,
    CANCELLED
}

data class PassengerState(
    val stage: TripStage = TripStage.IDLE,
    val pickupLabel: String = "موقعك الحالي",
    val destinationLabel: String = "",
    val carType: String = "اقتصادي",
    val estimatedFare: Int = 0,
    val estimatedMinutes: Int = 0,
    val captainName: String = "",
    val captainRating: Double = 0.0,
    val vehicleName: String = "",
    val vehiclePlate: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val errorMessage: String? = null
)

enum class PaymentMethod(val label: String) {
    CASH("نقداً"),
    CARD("بطاقة"),
    WALLET("محفظة")
}

sealed interface PassengerAction {
    data class DestinationSelected(val label: String) : PassengerAction
    data class CarTypeSelected(val type: String, val baseFare: Int) : PassengerAction
    data object RequestRide : PassengerAction
    data object CaptainAssigned : PassengerAction
    data object CaptainArrived : PassengerAction
    data object StartTrip : PassengerAction
    data object FinishTrip : PassengerAction
    data class RateTrip(val rating: Int) : PassengerAction
    data class SelectPayment(val method: PaymentMethod) : PassengerAction
    data object CancelTrip : PassengerAction
    data object ClearError : PassengerAction
}

object PassengerStateReducer {
    fun reduce(state: PassengerState, action: PassengerAction): PassengerState = when (action) {
        is PassengerAction.DestinationSelected -> state.copy(
            stage = TripStage.CONFIRMING_RIDE,
            destinationLabel = action.label,
            errorMessage = null
        )
        is PassengerAction.CarTypeSelected -> state.copy(
            carType = action.type,
            estimatedFare = action.baseFare,
            errorMessage = null
        )
        PassengerAction.RequestRide -> if (state.destinationLabel.isBlank()) {
            state.copy(errorMessage = "حدد وجهتك أولاً")
        } else {
            state.copy(stage = TripStage.SEARCHING_CAPTAIN, errorMessage = null)
        }
        PassengerAction.CaptainAssigned -> state.copy(stage = TripStage.CAPTAIN_ASSIGNED)
        PassengerAction.CaptainArrived -> state.copy(stage = TripStage.CAPTAIN_ARRIVING)
        PassengerAction.StartTrip -> state.copy(stage = TripStage.TRIP_STARTED)
        PassengerAction.FinishTrip -> state.copy(stage = TripStage.TRIP_FINISHED)
        is PassengerAction.RateTrip -> state.copy(stage = TripStage.COMPLETED)
        is PassengerAction.SelectPayment -> state.copy(paymentMethod = action.method)
        PassengerAction.CancelTrip -> state.copy(stage = TripStage.CANCELLED)
        PassengerAction.ClearError -> state.copy(errorMessage = null)
    }
}
