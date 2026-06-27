package com.example.mensahero_mobile_app.data.api

import com.example.mensahero_mobile_app.BuildConfig
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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

    suspend fun fetchMessages(apiKey: String): Result<List<Message>> {
        return try {
            val response: HttpResponse = client.get("${BuildConfig.API_URL}/api/messages/fetch") {
                url {
                    parameters.append("apiKey", apiKey)
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
}
