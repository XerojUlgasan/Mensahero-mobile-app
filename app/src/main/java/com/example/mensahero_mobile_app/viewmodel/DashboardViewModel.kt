package com.example.mensahero_mobile_app.viewmodel

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mensahero_mobile_app.BuildConfig
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.fcm.FcmTokenManager
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationRequest
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageStatus
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import com.example.mensahero_mobile_app.data.model.SimInfo
import com.example.mensahero_mobile_app.data.sms.SmsManagerWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

    private var fetchJob: Job? = null
    private var uptimeJob: Job? = null
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
                val isRegistered = !deviceId.isNullOrEmpty()
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
        viewModelScope.launch {
            delay(500)
            isInitialized = true
        }
    }

    private fun loadAvailableSims() {
        val sims = subscriptionManager?.activeSubscriptionInfoList?.map { info ->
            SimInfo(
                subscriptionId = info.subscriptionId,
                carrierName = info.carrierName?.toString() ?: "Unknown",
                phoneNumber = info.number,
                slotIndex = info.simSlotIndex
            )
        } ?: emptyList()
        _state.value = _state.value.copy(availableSims = sims)

        if (sims.size == 1 && _state.value.chosenSimId == null) {
            viewModelScope.launch { preferencesManager.saveChosenSimId(sims[0].subscriptionId) }
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
        val deviceId = preferencesManager.deviceId.first() ?: return
        val apiKey = _state.value.apiKey.trim().takeIf { it.isNotEmpty() } ?: return
        val deviceName = _state.value.deviceName.trim().takeIf { it.isNotEmpty() } ?: return
        val fcmToken = FcmTokenManager.getToken().getOrNull() ?: return

        apiService.updateDevice(
            DeviceUpdateRequest(
                device_id = deviceId,
                api_key = apiKey,
                fcm_token = fcmToken,
                isActive = isActive,
                device_name = deviceName
            )
        )
    }

    private fun startUptimeTracking() {
        activeSince = System.currentTimeMillis()
        uptimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_state.value.agentActive) {
                    _state.value = _state.value.copy(
                        uptimeSeconds = (System.currentTimeMillis() - activeSince) / 1000
                    )
                }
            }
        }
    }

    private fun startFetching() {
        if (_state.value.apiKey.isEmpty() || _state.value.chosenSimId == null || !_state.value.isDeviceRegistered) return

        fetchJob = viewModelScope.launch {
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
        if (!_state.value.canRefresh || _state.value.isFetching || _state.value.isProcessing || !_state.value.isDeviceRegistered) return

        viewModelScope.launch {
            _state.value = _state.value.copy(canRefresh = false)
            fetchAndProcessMessages()
            delay(3000)
            _state.value = _state.value.copy(canRefresh = true)
        }
    }

    private suspend fun fetchAndProcessMessages() {
        if (!_state.value.agentActive || _state.value.apiKey.isEmpty() || _state.value.chosenSimId == null || !_state.value.isDeviceRegistered) return

        val deviceId = preferencesManager.deviceId.first() ?: return

        _state.value = _state.value.copy(isFetching = true)
        val result = apiService.fetchMessages(_state.value.apiKey, deviceId)
        _state.value = _state.value.copy(isFetching = false)

        result.onSuccess { messages ->
            if (messages.isEmpty()) return

            viewModelScope.launch {
                messages.forEach { preferencesManager.incrementMessagesFetched() }
                preferencesManager.updateLastActivity()
            }

            processMessages(messages)
        }
    }

    private suspend fun processMessages(messages: List<Message>) {
        _state.value = _state.value.copy(isProcessing = true, processingMessageCount = messages.size)

        val updates = messages.map { message ->
            val sent = sendSms(message)
            val status = if (sent) MessageStatus.DELIVERED else MessageStatus.FAILED
            viewModelScope.launch {
                if (sent) preferencesManager.incrementDelivered() else preferencesManager.incrementFailed()
            }
            MessageUpdateRequest(messageId = message.id, status = status.value)
        }

        apiService.updateMessages(_state.value.apiKey, updates)
        _state.value = _state.value.copy(isProcessing = false, processingMessageCount = 0)
    }

    // Suspend wrapper so the coroutine waits for the SMS broadcast result
    private suspend fun sendSms(message: Message): Boolean =
        suspendCancellableCoroutine { continuation ->
            smsManager.sendMessage(
                message = message,
                subscriptionId = _state.value.chosenSimId ?: run {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                },
                onSuccess = { continuation.resume(true) },
                onFailure = { continuation.resume(false) }
            )
        }

    fun getDeliveryRate(): Double {
        val total = _state.value.totalDelivered + _state.value.totalFailed
        if (total == 0) return 0.0
        return (_state.value.totalDelivered.toDouble() / total) * 100.0
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

    fun getApiUrl(): String = BuildConfig.API_URL

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
            if (isUpdate) updateDeviceName(deviceName) else registerNewDevice(apiKey, deviceName)
        }
    }

    private suspend fun registerNewDevice(apiKey: String, deviceName: String) {
        val fcmToken = FcmTokenManager.getToken().getOrElse {
            _state.value = _state.value.copy(isRegisteringDevice = false, deviceRegistrationError = "Failed to get FCM token")
            return
        }

        val result = apiService.registerDevice(
            DeviceRegistrationRequest(
                fcm_token = fcmToken,
                api_key = apiKey,
                isActive = true,
                device_name = deviceName
            )
        )

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
        val deviceId = preferencesManager.deviceId.first() ?: run {
            _state.value = _state.value.copy(isRegisteringDevice = false, deviceRegistrationError = "Device not found")
            return
        }

        val fcmToken = FcmTokenManager.getToken().getOrElse {
            _state.value = _state.value.copy(isRegisteringDevice = false, deviceRegistrationError = "Failed to get FCM token")
            return
        }

        val result = apiService.updateDevice(
            DeviceUpdateRequest(
                device_id = deviceId,
                api_key = _state.value.apiKey,
                fcm_token = fcmToken,
                isActive = true,
                device_name = deviceName
            )
        )

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
        _state.value = _state.value.copy(showDeviceRegistration = true, inputDeviceName = "", inputApiKey = "")
    }

    private suspend fun autoUpdateDevice(newApiKey: String) {
        val deviceId = preferencesManager.deviceId.first() ?: return
        val fcmToken = FcmTokenManager.getToken().getOrNull() ?: return

        val result = apiService.updateDevice(
            DeviceUpdateRequest(
                device_id = deviceId,
                api_key = newApiKey,
                fcm_token = fcmToken,
                isActive = true,
                device_name = _state.value.deviceName
            )
        )
        if (result.isSuccess) preferencesManager.saveFcmToken(fcmToken)
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
        uptimeJob?.cancel()
    }
}
