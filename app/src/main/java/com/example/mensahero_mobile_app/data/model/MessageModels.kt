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
