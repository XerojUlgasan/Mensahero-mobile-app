package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ServerConnectionState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isConnected: Boolean = false
)

class ServerConnectionViewModel(context: Context) : ViewModel() {
    private val apiService = MensaheroApiService()
    private val preferencesManager = PreferencesManager(context)
    
    private val _state = MutableStateFlow(ServerConnectionState())
    val state: StateFlow<ServerConnectionState> = _state

    init {
        checkConnection()
    }

    fun checkConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = apiService.pingServer()
            
            if (result.isSuccess) {
                checkDeviceExistence()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null,
                    isConnected = true
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Connection failed",
                    isConnected = false
                )
            }
        }
    }

    private suspend fun checkDeviceExistence() {
        val deviceId = preferencesManager.deviceId.first()
        val apiKey = preferencesManager.apiKey.first()

        if (deviceId != null && apiKey != null) {
            val result = apiService.checkDevice(apiKey, deviceId)
            if (result.isFailure) {
                preferencesManager.clearDeviceRegistration()
            }
        }
    }
}
