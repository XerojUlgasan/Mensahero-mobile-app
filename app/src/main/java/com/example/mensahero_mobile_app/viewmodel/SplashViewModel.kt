package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SplashState {
    data object Idle : SplashState()
    data object CheckingSession : SplashState()
    data object HasSession : SplashState()
    data object NoSession : SplashState()
}

class SplashViewModel(context: Context) : ViewModel() {
    
    private val authRepository = AuthRepository(context)
    
    private val _state = MutableStateFlow<SplashState>(SplashState.Idle)
    val state: StateFlow<SplashState> = _state

    fun checkSession() {
        viewModelScope.launch {
            _state.value = SplashState.CheckingSession
            delay(500) // Small delay for splash visibility
            
            val hasSession = authRepository.isUserLoggedIn()
            _state.value = if (hasSession) {
                SplashState.HasSession
            } else {
                SplashState.NoSession
            }
        }
    }
}
