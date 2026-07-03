package com.example.mensahero_mobile_app.data.sms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.example.mensahero_mobile_app.data.model.Message

class SmsManagerWrapper(private val context: Context) {

    companion object {
        private const val TAG = "SmsManagerWrapper"
    }

    fun sendMessage(
        message: Message,
        subscriptionId: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        // Unique action per message prevents cross-message broadcast interference
        val sentAction = "com.example.mensahero_mobile_app.SMS_SENT.${message.id}"

        try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val subscription = subscriptionManager?.getActiveSubscriptionInfo(subscriptionId)

            if (subscription == null) {
                Log.e(TAG, "Subscription null for id=$subscriptionId — failing message ${message.id}")
                onFailure()
                return
            }

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        when (resultCode) {
                            android.app.Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS sent OK — id=${message.id}")
                                onSuccess()
                            }
                            else -> {
                                Log.e(TAG, "SMS send failed (resultCode=$resultCode) — id=${message.id}")
                                onFailure()
                            }
                        }
                    } finally {
                        // Always unregister to avoid leaking the receiver
                        runCatching { ctx?.unregisterReceiver(this) }
                    }
                }
            }

            context.registerReceiver(
                sentReceiver,
                IntentFilter(sentAction),
                Context.RECEIVER_NOT_EXPORTED
            )

            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                message.id.hashCode(),
                Intent(sentAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedNumber = formatPhoneNumber(message.receiver)
            Log.d(TAG, "Sending SMS to $formattedNumber — id=${message.id}")

            SmsManager.getSmsManagerForSubscriptionId(subscriptionId).sendTextMessage(
                formattedNumber,
                null,
                message.message,
                sentPendingIntent,
                null  // delivery report not needed — sent confirmation is sufficient
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission denied — id=${message.id}", e)
            onFailure()
        } catch (e: Exception) {
            Log.e(TAG, "SMS send exception — id=${message.id}", e)
            onFailure()
        }
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        val digits = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            digits.startsWith("0") && digits.length == 11 -> "+63" + digits.substring(1)
            !phoneNumber.startsWith("+") && digits.length == 10 -> "+63$digits"
            else -> phoneNumber
        }
    }
}
