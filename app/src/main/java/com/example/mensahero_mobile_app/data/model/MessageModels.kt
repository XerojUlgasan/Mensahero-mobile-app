package com.example.mensahero_mobile_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val message: String,
    val receiver: String,
    val sender: String?,
    val api_id: String,
    val created_at: String,
    val id: String,
    val sent_at: String?,
    val status: String
)

@Serializable
data class MessageUpdateRequest(
    val messageId: String,
    val status: String
)

enum class MessageStatus(val value: String) {
    DELIVERED("DELIVERED"),
    FAILED("FAILED")
}

@Serializable
data class DeviceRegistrationRequest(
    val fcm_token: String,
    val api_key: String,
    val isActive: Boolean,
    val device_name: String
)

@Serializable
data class DeviceRegistrationResponse(
    val id: String
)

@Serializable
data class DeviceUpdateRequest(
    val device_id: String,
    val api_key: String,
    val fcm_token: String,
    val isActive: Boolean,
    val device_name: String
)

@Serializable
data class DeviceCheckResponse(
    val apiId: String,
    val created_at: String,
    val deviceName: String,
    val fcm_token: String,
    val id: String,
    val isActive: Boolean,
    val last_used: String,
    val ownerId: String,
    val updated_at: String
)

@Serializable
data class HistoryMessageDto(
    val body: String,
    val direction: String,   // "SENT" or "RECEIVED"
    val timestamp: Long      // epoch millis
)

@Serializable
data class SubmitHistoryResultRequest(
    val apiKey: String,
    val deviceId: String,
    val jobId: String,
    val failed: Boolean,
    val failureReason: String? = null,
    val messages: List<HistoryMessageDto>? = null
)
