package com.example.mensahero_mobile_app.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    suspend fun getToken(retries: Int = 3): Result<String> {
        repeat(retries) { attempt ->
            try {
                Log.d("FCMToken", "Fetching FCM token from Firebase (attempt ${attempt + 1}/$retries)...")
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNotEmpty()) {
                    Log.d("FCMToken", "FCM token retrieved successfully: $token")
                    return Result.success(token)
                } else {
                    Log.w("FCMToken", "FCM token is empty, retrying...")
                    if (attempt < retries - 1) delay(1000)
                }
            } catch (e: Exception) {
                Log.e("FCMToken", "Failed to get FCM token (attempt ${attempt + 1}/$retries): ${e.message}", e)
                if (attempt < retries - 1) delay(1000)
            }
        }
        Log.e("FCMToken", "Failed to get FCM token after $retries attempts")
        return Result.failure(Exception("Failed to get FCM token after $retries attempts"))
    }

    suspend fun deleteAndRefreshToken(): Result<String> {
        return try {
            Log.d("FCMToken", "Deleting stale FCM token to force refresh...")
            FirebaseMessaging.getInstance().deleteToken().await()
            Log.d("FCMToken", "Token deleted, fetching new token...")
            getToken()
        } catch (e: Exception) {
            Log.e("FCMToken", "Failed to delete FCM token: ${e.message}", e)
            Result.failure(e)
        }
    }
}
