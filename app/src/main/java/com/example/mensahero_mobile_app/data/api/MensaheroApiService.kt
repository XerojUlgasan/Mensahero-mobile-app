package com.example.mensahero_mobile_app.data.api

import android.util.Log
import com.example.mensahero_mobile_app.BuildConfig
import com.example.mensahero_mobile_app.data.model.DeviceCheckResponse
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationRequest
import com.example.mensahero_mobile_app.data.model.DeviceRegistrationResponse
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import com.example.mensahero_mobile_app.data.model.ReceivedSmsBody
import com.example.mensahero_mobile_app.data.model.SubmitHistoryResultRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                url {
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
                header(HttpHeaders.Authorization, "Bearer $apiKey")
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
                header(HttpHeaders.Authorization, "Bearer ${request.api_key}")
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
                header(HttpHeaders.Authorization, "Bearer ${request.api_key}")
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

    /**
     * Submits the SMS thread-history result for a job.
     *
     * Note: the client is not configured with expectSuccess, so a non-2xx response
     * (e.g. 403/404/409 meaning the job was already resolved or isn't ours) does NOT
     * throw — it resolves as [Result.success]. Only genuine network/IO failures throw
     * and surface as [Result.failure], which the caller may treat as transient.
     */
    suspend fun submitHistoryResult(apiKey: String, request: SubmitHistoryResultRequest): Result<Unit> {
        return try {
            val response = client.post("${BuildConfig.API_URL}/api/messages/history/result") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Log.d("SmsHistory", "history/result status=${response.status} requestId=${request.requestId} failed=${request.failed}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SmsHistory", "history/result POST failed requestId=${request.requestId}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Relays an incoming SMS to the backend.
     *
     * The client is not configured with expectSuccess, so a non-2xx response does NOT
     * throw — it resolves as [Result.success] carrying the HTTP status code, letting the
     * caller decide whether to retry (5xx) or give up (400/401/403/404). Only genuine
     * network/IO failures resolve as [Result.failure], which the caller retries.
     */
    suspend fun relayReceivedSms(apiKey: String, body: ReceivedSmsBody): Result<Int> {
        return try {
            val response = client.post("${BuildConfig.API_URL}/api/messages/gateway/received") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            Log.d("SmsRelay", "gateway/received status=${response.status.value} from=${body.from}")
            Result.success(response.status.value)
        } catch (e: Exception) {
            Log.e("SmsRelay", "gateway/received POST failed from=${body.from}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkDevice(apiKey: String, deviceId: String): Result<DeviceCheckResponse> {
        return try {
            val response: HttpResponse = client.get("${BuildConfig.API_URL}/api/devices/check") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                url {
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
