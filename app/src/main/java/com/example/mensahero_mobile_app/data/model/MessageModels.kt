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

/**
 * Wire body for POST /api/messages/history/result.
 *
 * The API key is sent in the `Authorization: Bearer mh_live_...` header (see
 * [com.example.mensahero_mobile_app.data.api.MensaheroApiService.submitHistoryResult]),
 * NOT in this body.
 */
@Serializable
data class SubmitHistoryResultRequest(
    val deviceId: String,
    val requestId: String,
    val address: String,
    val pageSize: Int,
    val pageNumber: Int,
    val failed: Boolean,
    val reason: String? = null,
    val messages: List<HistoryMessageDto> = emptyList()
)

/**
 * Fixed set of failure reasons accepted by the backend. Any unrecognized value is
 * normalized server-side to [GATEWAY_FAILURE], but we always send one of these.
 */
object HistoryFailureReason {
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND"
    const val DEVICE_BUSY = "DEVICE_BUSY"
    const val GATEWAY_FAILURE = "GATEWAY_FAILURE"
}
