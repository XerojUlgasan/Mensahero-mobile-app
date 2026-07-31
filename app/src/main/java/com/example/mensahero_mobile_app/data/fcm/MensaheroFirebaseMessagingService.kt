package com.example.mensahero_mobile_app.data.fcm

import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.service.MessageProcessingService
import com.example.mensahero_mobile_app.work.SmsHistoryWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MensaheroFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d("FCM", "onNewToken called: $token")
        scope.launch {
            PreferencesManager(applicationContext).saveFcmToken(token)
        }
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message received — type: ${message.data["type"]}")

        when (message.data["type"]) {
            "NEW_MESSAGE_JOB" -> {
                startForegroundService(
                    Intent(applicationContext, MessageProcessingService::class.java)
                )
                Log.d("FCM", "MessageProcessingService started")
            }

            "SMS_HISTORY" -> {
                val requestId = message.data["requestId"]
                val address = message.data["address"]
                if (requestId != null && address != null) {
                    // All FCM data values arrive as strings; parse the paging ints.
                    val pageSize = (message.data["pageSize"]?.toIntOrNull() ?: 25).coerceIn(1, 25)
                    val pageNumber = (message.data["pageNumber"]?.toIntOrNull() ?: 0).coerceAtLeast(0)

                    val work = OneTimeWorkRequestBuilder<SmsHistoryWorker>()
                        .setInputData(
                            workDataOf(
                                SmsHistoryWorker.KEY_REQUEST_ID to requestId,
                                SmsHistoryWorker.KEY_ADDRESS to address,
                                SmsHistoryWorker.KEY_PAGE_SIZE to pageSize,
                                SmsHistoryWorker.KEY_PAGE_NUMBER to pageNumber
                            )
                        )
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    WorkManager.getInstance(applicationContext).enqueue(work)
                    Log.d("FCM", "SmsHistoryWorker enqueued for request $requestId (page $pageNumber, size $pageSize)")
                } else {
                    Log.w("FCM", "SMS_HISTORY missing requestId/address — ignoring")
                }
            }
        }
    }
}
