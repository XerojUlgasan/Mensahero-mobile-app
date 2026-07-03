package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.fcm.FcmTokenManager
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
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

        if (deviceId == null || apiKey == null) return

        val result = apiService.checkDevice(apiKey, deviceId)
        if (result.isFailure) {
            preferencesManager.clearDeviceRegistration()
            return
        }

        // Device exists on backend — refresh FCM token in case it became UNREGISTERED
        val storedToken = preferencesManager.fcmToken.first()
        val tokenResult = FcmTokenManager.getToken()
        val freshToken = tokenResult.getOrNull() ?: return

        if (freshToken != storedToken) {
            android.util.Log.d("FCMToken", "Old FCM Token: $storedToken")
            android.util.Log.d("FCMToken", "New FCM Token: $freshToken")
            val deviceName = preferencesManager.deviceName.first() ?: return
            val updateRequest = DeviceUpdateRequest(
                device_id = deviceId,
                api_key = apiKey,
                fcm_token = freshToken,
                isActive = true,
                device_name = deviceName
            )
            val updateResult = apiService.updateDevice(updateRequest)
            if (updateResult.isSuccess) {
                preferencesManager.saveFcmToken(freshToken)
                android.util.Log.d("FCMToken", "FCM token updated on backend")
            }
        } else {
            // Token matches stored — but backend may have it as UNREGISTERED.
            // Force delete + refresh to get a guaranteed-fresh token.
            android.util.Log.d("FCMToken", "FCM token unchanged, force-refreshing to ensure validity...")
            val refreshResult = FcmTokenManager.deleteAndRefreshToken()
            val newToken = refreshResult.getOrNull() ?: return
            if (newToken != storedToken) {
                android.util.Log.d("FCMToken", "Got new FCM token after force refresh: $newToken")
                val deviceName = preferencesManager.deviceName.first() ?: return
                val updateRequest = DeviceUpdateRequest(
                    device_id = deviceId,
                    api_key = apiKey,
                    fcm_token = newToken,
                    isActive = true,
                    device_name = deviceName
                )
                val updateResult = apiService.updateDevice(updateRequest)
                if (updateResult.isSuccess) {
                    preferencesManager.saveFcmToken(newToken)
                    android.util.Log.d("FCMToken", "FCM token force-refreshed and updated on backend")
                }
            } else {
                android.util.Log.d("FCMToken", "FCM tokens match - no update needed")
            }
        }
    }
}
