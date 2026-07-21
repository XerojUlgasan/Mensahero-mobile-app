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
                val jobId = message.data["jobId"]
                val address = message.data["address"]
                if (jobId != null && address != null) {
                    val work = OneTimeWorkRequestBuilder<SmsHistoryWorker>()
                        .setInputData(
                            workDataOf(
                                SmsHistoryWorker.KEY_JOB_ID to jobId,
                                SmsHistoryWorker.KEY_ADDRESS to address
                            )
                        )
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    WorkManager.getInstance(applicationContext).enqueue(work)
                    Log.d("FCM", "SmsHistoryWorker enqueued for job $jobId")
                } else {
                    Log.w("FCM", "SMS_HISTORY missing jobId/address — ignoring")
                }
            }
        }
    }
}
