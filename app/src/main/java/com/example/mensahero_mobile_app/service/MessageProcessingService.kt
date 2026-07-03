package com.example.mensahero_mobile_app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.model.Message
import com.example.mensahero_mobile_app.data.model.MessageStatus
import com.example.mensahero_mobile_app.data.model.MessageUpdateRequest
import com.example.mensahero_mobile_app.data.sms.SmsManagerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessageProcessingService : Service() {

    private val tag = "MessageService"
    private val notificationId = 1001
    private val channelId = "mensahero_processing"

    private val api = MensaheroApiService()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null
    private lateinit var prefs: PreferencesManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()

        if (processingJob?.isActive == true) {
            Log.d(tag, "Already running — ignoring duplicate start")
            return START_NOT_STICKY
        }

        prefs = PreferencesManager(applicationContext)
        val smsManager = SmsManagerWrapper(applicationContext)

        processingJob = scope.launch {
            try {
                val apiKey = prefs.apiKey.first()
                val simId = prefs.chosenSimId.first()
                val deviceId = prefs.deviceId.first()

                if (apiKey.isNullOrEmpty() || simId == null || deviceId == null) {
                    Log.e(tag, "Missing apiKey, simId, or deviceId — stopping service")
                    return@launch
                }

                Log.d(tag, "Starting fetch/send loop")
                processUntilEmpty(apiKey, deviceId, simId, smsManager)
                Log.d(tag, "No more messages — stopping service")
            } catch (e: Exception) {
                Log.e(tag, "Error in processing loop: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processUntilEmpty(apiKey: String, deviceId: String, simId: Int, smsManager: SmsManagerWrapper) {
        while (true) {
            val messages = api.fetchMessages(apiKey, deviceId)
                .onFailure { Log.e(tag, "Fetch failed: ${it.message}", it) }
                .getOrNull() ?: break

            if (messages.isEmpty()) break

            Log.d(tag, "Fetched ${messages.size} messages — sending SMS")
            val updates = messages.map { message ->
                val sent = sendSms(message, simId, smsManager)
                Log.d(tag, "SMS id=${message.id}: ${if (sent) "SENT" else "FAILED"}")
                if (sent) {
                    prefs.incrementMessagesFetched()
                    prefs.incrementDelivered()
                } else {
                    prefs.incrementMessagesFetched()
                    prefs.incrementFailed()
                }
                prefs.updateLastActivity()
                MessageUpdateRequest(
                    messageId = message.id,
                    status = if (sent) MessageStatus.DELIVERED.value else MessageStatus.FAILED.value
                )
            }

            val updated = api.updateMessages(apiKey, updates)
                .onFailure { Log.e(tag, "Update failed: ${it.message}", it) }
                .isSuccess

            if (!updated) break
        }
    }

    private suspend fun sendSms(message: Message, simId: Int, smsManager: SmsManagerWrapper): Boolean =
        suspendCancellableCoroutine { continuation ->
            smsManager.sendMessage(
                message = message,
                subscriptionId = simId,
                onSuccess = { continuation.resume(true) },
                onFailure = { continuation.resume(false) }
            )
        }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Message Processing", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while MensaHERO is processing outgoing messages"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MensaHERO")
            .setContentText("Processing messages...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        else
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ServiceCompat.startForeground(this, notificationId, notification, serviceType)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
