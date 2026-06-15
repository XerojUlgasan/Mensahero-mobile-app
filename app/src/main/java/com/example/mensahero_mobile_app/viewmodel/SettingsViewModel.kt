package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val userName: String = "",
    val userEmail: String = ""
)

class SettingsViewModel(context: Context) : ViewModel() {
    
    private val authRepository = AuthRepository(context)
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val user = authRepository.getCurrentUser()
        user?.let {
            _state.value = _state.value.copy(
                userName = it.email?.substringBefore("@") ?: "User",
                userEmail = it.email ?: ""
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoggingOut = true)
            
            val result = authRepository.logout()
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = true
                )
            } else {
                _state.value = _state.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = false
                )
            }
        }
    }
}
