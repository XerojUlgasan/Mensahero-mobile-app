package com.example.mensahero_mobile_app.data.api

import android.util.Log
import com.example.mensahero_mobile_app.BuildConfig
import com.example.mensahero_mobile_app.data.model.DeviceCheckResponse
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationRequest
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationResponse
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MensaheroApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchMessages(apiKey: String, deviceId: String): Result<List<Message>> {
        return try {
            val response: HttpResponse = client.get("${BuildConfig.API_URL}/api/messages/fetch") {
                url {
                    parameters.append("apiKey", apiKey)
                    parameters.append("deviceId", deviceId)
                }
            }
            val messages: List<Message> = response.body()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMessages(apiKey: String, updates: List<MessageUpdateRequest>): Result<Unit> {
        return try {
            client.post("${BuildConfig.API_URL}/api/messages/update") {
                url {
                    parameters.append("apiKey", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(updates)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pingServer(): Result<Unit> {
        return try {
            client.get("${BuildConfig.API_URL}/api/ping")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse> {
        return try {
            val response: HttpResponse = client.post("${BuildConfig.API_URL}/api/devices/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val result: DeviceRegistrationResponse = response.body()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDevice(request: DeviceUpdateRequest): Result<Unit> {
        return try {
            Log.d("FCMToken", "Sending update request: device_id=${request.device_id}, fcm_token=${request.fcm_token}")
            val response = client.patch("${BuildConfig.API_URL}/api/devices/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Log.d("FCMToken", "Update response status: ${response.status}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FCMToken", "Update device failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkDevice(apiKey: String, deviceId: String): Result<DeviceCheckResponse> {
        return try {
            val response: HttpResponse = client.get("${BuildConfig.API_URL}/api/devices/check") {
                url {
                    parameters.append("apiKey", apiKey)
                    parameters.append("deviceId", deviceId)
                }
            }
            val result: DeviceCheckResponse = response.body()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
