package com.waslha.app.domain

sealed interface PassengerUiState<out T> {
    data object Idle : PassengerUiState<Nothing>
    data object Loading : PassengerUiState<Nothing>
    data class Success<T>(val data: T) : PassengerUiState<T>
    data class Error(val message: String, val canRetry: Boolean = true) : PassengerUiState<Nothing>
    data object Empty : PassengerUiState<Nothing>
}

sealed interface TripActionState {
    data object None : TripActionState
    data object Submitting : TripActionState
    data object Success : TripActionState
    data class Error(val message: String) : TripActionState
}
