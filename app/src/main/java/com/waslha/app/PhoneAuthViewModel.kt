package com.waslha.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val phone: String = "",
    val otp: String = "",
    val step: AuthStep = AuthStep.Phone,
    val loading: Boolean = false,
    val error: String? = null,
    val devCode: String? = null
)

enum class AuthStep { Phone, Otp, SignedIn }

class PhoneAuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun phoneChanged(value: String) { _state.value = _state.value.copy(phone = value, error = null) }
    fun otpChanged(value: String) { _state.value = _state.value.copy(otp = value.filter(Char::isDigit).take(6), error = null) }

    fun requestCode() {
        val phone = _state.value.phone.trim()
        if (phone.length < 8) { _state.value = _state.value.copy(error = "أدخل رقم هاتف صحيح"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.requestCode(phone)
                .onSuccess { response -> _state.value = _state.value.copy(loading = false, step = AuthStep.Otp, devCode = response.devCode) }
                .onFailure { error -> _state.value = _state.value.copy(loading = false, error = error.message) }
        }
    }

    fun verifyCode() {
        val current = _state.value
        if (current.otp.length != 6) { _state.value = current.copy(error = "أدخل رمز التحقق المكوّن من 6 أرقام"); return }
        viewModelScope.launch {
            _state.value = current.copy(loading = true, error = null)
            repository.verifyCode(current.phone.trim(), current.otp)
                .onSuccess { _state.value = _state.value.copy(loading = false, step = AuthStep.SignedIn) }
                .onFailure { error -> _state.value = _state.value.copy(loading = false, error = error.message) }
        }
    }
}

class PhoneAuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PhoneAuthViewModel(repository) as T
}
