package com.waslha.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TripUiState {
    data object Idle : TripUiState
    data object Loading : TripUiState
    data class Success(val trip: Trip) : TripUiState
    data class Error(val message: String) : TripUiState
}

class PassengerTripViewModel(
    private val repository: TripRepository = TripRepository(ApiProvider.api)
) : ViewModel() {
    private val _state = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val state: StateFlow<TripUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun createTrip(request: TripRequest) {
        stopPolling()
        viewModelScope.launch {
            _state.value = TripUiState.Loading
            repository.create(request)
                .onSuccess { trip ->
                    _state.value = TripUiState.Success(trip)
                    startPolling(trip.id)
                }
                .onFailure { error ->
                    _state.value = TripUiState.Error(error.message ?: "تعذر إنشاء الرحلة")
                }
        }
    }

    fun loadTrip(id: String) {
        viewModelScope.launch {
            repository.get(id)
                .onSuccess { trip -> _state.value = TripUiState.Success(trip) }
                .onFailure { error -> _state.value = TripUiState.Error(error.message ?: "تعذر تحديث الرحلة") }
        }
    }

    fun cancelTrip(id: String, reason: String) {
        stopPolling()
        viewModelScope.launch {
            _state.value = TripUiState.Loading
            repository.cancel(id, reason)
                .onSuccess { trip -> _state.value = TripUiState.Success(trip) }
                .onFailure { error -> _state.value = TripUiState.Error(error.message ?: "تعذر إلغاء الرحلة") }
        }
    }

    private fun startPolling(id: String) {
        stopPolling()
        pollingJob = viewModelScope.launch {
            var keepPolling = true
            while (keepPolling) {
                delay(5000)
                repository.get(id).onSuccess { trip ->
                    _state.value = TripUiState.Success(trip)
                    if (trip.status == "completed" || trip.status == "cancelled") {
                        keepPolling = false
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}
