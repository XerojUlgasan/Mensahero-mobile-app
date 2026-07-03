package com.example.mensahero_mobile_app.data.fcm

import android.content.Intent
import android.util.Log
import com.example.mensahero_mobile_app.service.MessageProcessingService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MensaheroFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "onNewToken called: $token")
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message received — type: ${message.data["type"]}")

        if (message.data["type"] == "NEW_MESSAGE_JOB") {
            startForegroundService(
                Intent(applicationContext, MessageProcessingService::class.java)
            )
            Log.d("FCM", "MessageProcessingService started")
        }
    }
}
