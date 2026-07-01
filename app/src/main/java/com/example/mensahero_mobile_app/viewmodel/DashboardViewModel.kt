package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.BuildConfig
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationRequest
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationResponse
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageStatus
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import com.example.mensahero_mobile_app.data.model.SimInfo
import com.example.mensahero_mobile_app.data.fcm.FcmTokenManager
import com.example.mensahero_mobile_app.data.sms.SmsManagerWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val apiKey: String = "",
    val chosenSimId: Int? = null,
    val agentActive: Boolean = true,
    val isFetching: Boolean = false,
    val isProcessing: Boolean = false,
    val canRefresh: Boolean = true,
    val availableSims: List<SimInfo> = emptyList(),
    val showSimSelection: Boolean = false,
    val totalMessagesFetched: Int = 0,
    val totalDelivered: Int = 0,
    val totalFailed: Int = 0,
    val lastActivityTimestamp: Long? = null,
    val uptimeSeconds: Long = 0,
    val processingMessageCount: Int = 0,
    val isDeviceRegistered: Boolean = false,
    val deviceName: String = "",
    val showDeviceRegistration: Boolean = false,
    val isRegisteringDevice: Boolean = false,
    val deviceRegistrationError: String? = null,
    val inputDeviceName: String = "",
    val inputApiKey: String = ""
)

class DashboardViewModel(context: Context) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)
    private val apiService = MensaheroApiService()
    private val smsManager = SmsManagerWrapper(context)
    private val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    private var fetchJob: kotlinx.coroutines.Job? = null
    private var uptimeJob: kotlinx.coroutines.Job? = null
    private var activeSince: Long = 0
    private var lastKnownApiKey: String = ""
    private var isInitialized: Boolean = false
    
    init {
        loadPreferences()
        loadAvailableSims()
        startUptimeTracking()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesManager.deviceId.collect { deviceId ->
                val isRegistered = deviceId != null && deviceId.isNotEmpty()
                _state.value = _state.value.copy(isDeviceRegistered = isRegistered)
                
                if (isInitialized && !isRegistered && _state.value.apiKey.isNotEmpty() && !_state.value.showDeviceRegistration) {
                    promptDeviceRegistration()
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.apiKey.collect { key ->
                val currentKey = key ?: ""
                _state.value = _state.value.copy(apiKey = currentKey)
                
                if (isInitialized && lastKnownApiKey.isNotEmpty() && currentKey != lastKnownApiKey && _state.value.isDeviceRegistered) {
                    autoUpdateDevice(currentKey)
                }
                lastKnownApiKey = currentKey
            }
        }
        viewModelScope.launch {
            preferencesManager.chosenSimId.collect { simId ->
                _state.value = _state.value.copy(chosenSimId = simId)
            }
        }
        viewModelScope.launch {
            preferencesManager.agentActive.collect { active ->
                val wasActive = _state.value.agentActive
                _state.value = _state.value.copy(agentActive = active)
                if (active && !wasActive) {
                    activeSince = System.currentTimeMillis()
                    startFetching()
                } else if (!active && wasActive) {
                    stopFetching()
                    _state.value = _state.value.copy(uptimeSeconds = 0)
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.totalMessagesFetched.collect { count ->
                _state.value = _state.value.copy(totalMessagesFetched = count)
            }
        }
        viewModelScope.launch {
            preferencesManager.totalDelivered.collect { count ->
                _state.value = _state.value.copy(totalDelivered = count)
            }
        }
        viewModelScope.launch {
            preferencesManager.totalFailed.collect { count ->
                _state.value = _state.value.copy(totalFailed = count)
            }
        }
        viewModelScope.launch {
            preferencesManager.lastActivityTimestamp.collect { timestamp ->
                _state.value = _state.value.copy(lastActivityTimestamp = timestamp)
            }
        }
        viewModelScope.launch {
            preferencesManager.deviceName.collect { deviceName ->
                _state.value = _state.value.copy(deviceName = deviceName ?: "")
            }
        }
        
        // Mark as initialized after a short delay to allow all preferences to load
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            isInitialized = true
        }
    }
    
    private fun loadAvailableSims() {
        val sims = subscriptionManager?.activeSubscriptionInfoList?.mapIndexed { index, info ->
            SimInfo(
                subscriptionId = info.subscriptionId,
                carrierName = info.carrierName?.toString() ?: "Unknown",
                phoneNumber = info.number,
                slotIndex = info.simSlotIndex
            )
        } ?: emptyList()
        _state.value = _state.value.copy(availableSims = sims)
        
        if (sims.size == 1 && _state.value.chosenSimId == null) {
            viewModelScope.launch {
                preferencesManager.saveChosenSimId(sims[0].subscriptionId)
            }
        } else if (sims.size > 1 && _state.value.chosenSimId == null) {
            _state.value = _state.value.copy(showSimSelection = true)
        }
    }
    
    fun selectSim(simId: Int) {
        viewModelScope.launch {
            preferencesManager.saveChosenSimId(simId)
            _state.value = _state.value.copy(showSimSelection = false)
        }
    }
    
    fun toggleAgentActive(active: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAgentActive(active)
            
            if (_state.value.isDeviceRegistered) {
                updateDeviceStatus(active)
            }
        }
    }

    private suspend fun updateDeviceStatus(isActive: Boolean) {
        val deviceId = preferencesManager.deviceId.first()
        val apiKey = _state.value.apiKey.trim()
        val deviceName = _state.value.deviceName.trim()

        if (deviceId == null || apiKey.isEmpty() || deviceName.isEmpty()) {
            return
        }

        val tokenResult = FcmTokenManager.getToken()
        if (tokenResult.isFailure) {
            return
        }

        val fcmToken = tokenResult.getOrNull() ?: ""
        val request = DeviceUpdateRequest(
            device_id = deviceId,
            api_key = apiKey,
            fcm_token = fcmToken,
            isActive = isActive,
            device_name = deviceName
        )

        apiService.updateDevice(request)
    }
    
    private fun startUptimeTracking() {
        activeSince = System.currentTimeMillis()
        uptimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_state.value.agentActive) {
                    val uptime = (System.currentTimeMillis() - activeSince) / 1000
                    _state.value = _state.value.copy(uptimeSeconds = uptime)
                }
            }
        }
    }
    
    private fun startFetching() {
        android.util.Log.d("DashboardViewModel", "startFetching called")
        android.util.Log.d("DashboardViewModel", "API Key: ${_state.value.apiKey}")
        android.util.Log.d("DashboardViewModel", "Chosen SIM ID: ${_state.value.chosenSimId}")
        android.util.Log.d("DashboardViewModel", "Agent Active: ${_state.value.agentActive}")
        android.util.Log.d("DashboardViewModel", "Device Registered: ${_state.value.isDeviceRegistered}")
        
        if (_state.value.apiKey.isEmpty() || _state.value.chosenSimId == null || !_state.value.isDeviceRegistered) {
            android.util.Log.e("DashboardViewModel", "Cannot start fetching - API key empty, SIM not selected, or device not registered")
            return
        }
        
        fetchJob = viewModelScope.launch {
            android.util.Log.d("DashboardViewModel", "Fetch job started")
            while (_state.value.agentActive) {
                fetchAndProcessMessages()
                delay(30000)
            }
        }
    }
    
    private fun stopFetching() {
        fetchJob?.cancel()
        fetchJob = null
    }
    
    fun manualRefresh() {
        if (!_state.value.canRefresh || _state.value.isFetching || _state.value.isProcessing || !_state.value.isDeviceRegistered) {
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(canRefresh = false)
            fetchAndProcessMessages()
            delay(3000)
            _state.value = _state.value.copy(canRefresh = true)
        }
    }
    
    private suspend fun fetchAndProcessMessages() {
        android.util.Log.d("DashboardViewModel", "fetchAndProcessMessages called")
        android.util.Log.d("DashboardViewModel", "Agent Active: ${_state.value.agentActive}")
        android.util.Log.d("DashboardViewModel", "API Key: '${_state.value.apiKey}'")
        android.util.Log.d("DashboardViewModel", "Chosen SIM ID: ${_state.value.chosenSimId}")
        android.util.Log.d("DashboardViewModel", "Device Registered: ${_state.value.isDeviceRegistered}")
        
        if (!_state.value.agentActive || _state.value.apiKey.isEmpty() || _state.value.chosenSimId == null || !_state.value.isDeviceRegistered) {
            android.util.Log.e("DashboardViewModel", "Cannot fetch - conditions not met")
            android.util.Log.e("DashboardViewModel", "Agent Active: ${_state.value.agentActive}, API Key empty: ${_state.value.apiKey.isEmpty()}, SIM null: ${_state.value.chosenSimId == null}, Device Registered: ${_state.value.isDeviceRegistered}")
            return
        }
        
        _state.value = _state.value.copy(isFetching = true)
        android.util.Log.d("DashboardViewModel", "Fetching messages from API...")
        
        val result = apiService.fetchMessages(_state.value.apiKey)
        
        _state.value = _state.value.copy(isFetching = false)
        
        result.onSuccess { messages ->
            android.util.Log.d("DashboardViewModel", "API fetch successful, got ${messages.size} messages")
            if (messages.isEmpty()) {
                android.util.Log.d("DashboardViewModel", "No messages to process")
                return
            }
            
            messages.forEach { msg ->
                android.util.Log.d("DashboardViewModel", "Message: id=${msg.id}, receiver=${msg.receiver}, status=${msg.status}")
            }
            
            viewModelScope.launch {
                messages.forEach { preferencesManager.incrementMessagesFetched() }
                preferencesManager.updateLastActivity()
            }
            
            processMessages(messages)
        }
        result.onFailure { error ->
            android.util.Log.e("DashboardViewModel", "API fetch failed: $error")
        }
    }
    
    private suspend fun processMessages(messages: List<Message>) {
        android.util.Log.d("DashboardViewModel", "Processing ${messages.size} messages")
        _state.value = _state.value.copy(isProcessing = true, processingMessageCount = messages.size)
        
        val updates = mutableListOf<MessageUpdateRequest>()
        
        for (message in messages) {
            android.util.Log.d("DashboardViewModel", "Processing message ID: ${message.id}")
            val success = sendMessage(message)
            android.util.Log.d("DashboardViewModel", "Message ${message.id} send result: $success")
            val status = if (success) MessageStatus.DELIVERED else MessageStatus.FAILED
            
            updates.add(MessageUpdateRequest(messageId = message.id, status = status.value))
            
            viewModelScope.launch {
                if (success) {
                    preferencesManager.incrementDelivered()
                } else {
                    preferencesManager.incrementFailed()
                }
            }
        }
        
        android.util.Log.d("DashboardViewModel", "Updating ${updates.size} messages to API")
        val updateResult = apiService.updateMessages(_state.value.apiKey, updates)
        
        _state.value = _state.value.copy(isProcessing = false, processingMessageCount = 0)
        
        updateResult.onSuccess {
            android.util.Log.d("DashboardViewModel", "API update successful, fetching next batch")
            fetchAndProcessMessages()
        }
        updateResult.onFailure { error ->
            android.util.Log.e("DashboardViewModel", "API update failed: $error")
        }
    }
    
    private fun sendMessage(message: Message): Boolean {
        var success = false
        android.util.Log.d("DashboardViewModel", "Sending message: ${message.id} to ${message.receiver}")
        android.util.Log.d("DashboardViewModel", "Chosen SIM ID: ${_state.value.chosenSimId}")
        smsManager.sendMessage(
            message = message,
            subscriptionId = _state.value.chosenSimId ?: return false,
            onSuccess = { 
                android.util.Log.d("DashboardViewModel", "SMS send success callback triggered")
                success = true 
            },
            onFailure = { 
                android.util.Log.e("DashboardViewModel", "SMS send failure callback triggered")
                success = false 
            }
        )
        android.util.Log.d("DashboardViewModel", "sendMessage returned: $success")
        return success
    }
    
    fun getMessagesTodayCount(): Int {
        val todayStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )?.time ?: return _state.value.totalMessagesFetched
        
        return _state.value.totalMessagesFetched
    }
    
    fun getDeliveryRate(): Double {
        val total = _state.value.totalDelivered + _state.value.totalFailed
        if (total == 0) return 0.0
        return (_state.value.totalDelivered.toDouble() / total.toDouble()) * 100.0
    }
    
    fun getLastActivityText(): String {
        val timestamp = _state.value.lastActivityTimestamp ?: return "Never"
        val diff = System.currentTimeMillis() - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} min ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
    
    fun getUptimeText(): String {
        val seconds = _state.value.uptimeSeconds
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
    
    fun getApiUrl(): String {
        return BuildConfig.API_URL
    }

    fun showDeviceRegistration() {
        _state.value = _state.value.copy(
            showDeviceRegistration = true,
            inputDeviceName = if (_state.value.isDeviceRegistered) _state.value.deviceName else ""
        )
    }

    fun hideDeviceRegistration() {
        _state.value = _state.value.copy(
            showDeviceRegistration = false,
            deviceRegistrationError = null,
            inputDeviceName = "",
            inputApiKey = ""
        )
    }

    fun onInputDeviceNameChange(name: String) {
        _state.value = _state.value.copy(inputDeviceName = name)
    }

    fun onInputApiKeyChange(key: String) {
        _state.value = _state.value.copy(inputApiKey = key)
    }

    fun registerDevice() {
        val apiKey = _state.value.inputApiKey.trim().takeIf { it.isNotEmpty() } ?: _state.value.apiKey.trim()
        val deviceName = _state.value.inputDeviceName.trim()
        val isUpdate = _state.value.isDeviceRegistered

        if (!isUpdate && apiKey.isEmpty()) {
            _state.value = _state.value.copy(deviceRegistrationError = "API key is required")
            return
        }
        if (deviceName.isEmpty()) {
            _state.value = _state.value.copy(deviceRegistrationError = "Device name is required")
            return
        }

        _state.value = _state.value.copy(isRegisteringDevice = true, deviceRegistrationError = null)

        viewModelScope.launch {
            if (isUpdate) {
                updateDeviceName(deviceName)
            } else {
                registerNewDevice(apiKey, deviceName)
            }
        }
    }

    private suspend fun registerNewDevice(apiKey: String, deviceName: String) {
        val tokenResult = FcmTokenManager.getToken()
        if (tokenResult.isFailure) {
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                deviceRegistrationError = "Failed to get FCM token"
            )
            return
        }

        val fcmToken = tokenResult.getOrNull() ?: ""
        val request = DeviceRegistrationRequest(
            fcm_token = fcmToken,
            api_key = apiKey,
            isActive = true,
            device_name = deviceName
        )

        val result = apiService.registerDevice(request)

        if (result.isSuccess) {
            val deviceId = result.getOrNull()?.id ?: ""
            preferencesManager.saveDeviceId(deviceId)
            preferencesManager.saveFcmToken(fcmToken)
            preferencesManager.saveDeviceName(deviceName)
            preferencesManager.saveApiKey(apiKey)

            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                showDeviceRegistration = false,
                inputDeviceName = "",
                inputApiKey = "",
                deviceRegistrationError = null,
                deviceName = deviceName,
                isDeviceRegistered = true
            )
        } else {
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                deviceRegistrationError = result.exceptionOrNull()?.message ?: "Registration failed"
            )
        }
    }

    private suspend fun updateDeviceName(deviceName: String) {
        val deviceId = preferencesManager.deviceId.first()
        if (deviceId == null) {
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                deviceRegistrationError = "Device not found"
            )
            return
        }

        val tokenResult = FcmTokenManager.getToken()
        if (tokenResult.isFailure) {
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                deviceRegistrationError = "Failed to get FCM token"
            )
            return
        }

        val fcmToken = tokenResult.getOrNull() ?: ""
        val request = DeviceUpdateRequest(
            device_id = deviceId,
            api_key = _state.value.apiKey,
            fcm_token = fcmToken,
            isActive = true,
            device_name = deviceName
        )

        val result = apiService.updateDevice(request)

        if (result.isSuccess) {
            preferencesManager.saveDeviceName(deviceName)
            preferencesManager.saveFcmToken(fcmToken)
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                showDeviceRegistration = false,
                inputDeviceName = "",
                inputApiKey = "",
                deviceRegistrationError = null,
                deviceName = deviceName
            )
        } else {
            _state.value = _state.value.copy(
                isRegisteringDevice = false,
                deviceRegistrationError = result.exceptionOrNull()?.message ?: "Update failed"
            )
        }
    }

    private fun promptDeviceRegistration() {
        _state.value = _state.value.copy(
            showDeviceRegistration = true,
            inputDeviceName = "",
            inputApiKey = ""
        )
    }

    private suspend fun autoUpdateDevice(newApiKey: String) {
        val deviceId = preferencesManager.deviceId.first()
        if (deviceId == null) return

        val tokenResult = FcmTokenManager.getToken()
        if (tokenResult.isFailure) return

        val fcmToken = tokenResult.getOrNull() ?: ""
        val request = DeviceUpdateRequest(
            device_id = deviceId,
            api_key = newApiKey,
            fcm_token = fcmToken,
            isActive = true,
            device_name = _state.value.deviceName
        )

        val result = apiService.updateDevice(request)
        if (result.isSuccess) {
            preferencesManager.saveFcmToken(fcmToken)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
        uptimeJob?.cancel()
    }
}
