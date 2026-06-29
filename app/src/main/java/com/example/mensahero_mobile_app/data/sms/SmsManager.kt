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
import com.example.mensahero_mobile_app.data.model.MessageStatus

class SmsManagerWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsManagerWrapper"
        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }
    
    fun sendMessage(
        message: Message,
        subscriptionId: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.d(TAG, "Attempting to send SMS")
        Log.d(TAG, "Message ID: ${message.id}")
        Log.d(TAG, "Receiver: ${message.receiver}")
        Log.d(TAG, "Message content: ${message.message}")
        Log.d(TAG, "Subscription ID: $subscriptionId")
        
        try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val subscription = subscriptionManager?.getActiveSubscriptionInfo(subscriptionId)
            
            Log.d(TAG, "Subscription info: $subscription")
            Log.d(TAG, "Subscription carrier: ${subscription?.carrierName}")
            Log.d(TAG, "Subscription number: ${subscription?.number}")
            
            if (subscription != null) {
                val smsManagerForSub = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                Log.d(TAG, "Got SmsManager for subscription ID: $subscriptionId")
                
                // Create sent intent
                val sentIntent = Intent(SMS_SENT_ACTION).apply {
                    putExtra("message_id", message.id)
                    putExtra("receiver", message.receiver)
                }
                val sentPendingIntent = PendingIntent.getBroadcast(
                    context,
                    message.id.hashCode(),
                    sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Create delivered intent
                val deliveredIntent = Intent(SMS_DELIVERED_ACTION).apply {
                    putExtra("message_id", message.id)
                    putExtra("receiver", message.receiver)
                }
                val deliveredPendingIntent = PendingIntent.getBroadcast(
                    context,
                    message.id.hashCode() + 1,
                    deliveredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Register receivers
                val sentReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val resultCode = resultCode
                        val messageId = intent?.getStringExtra("message_id")
                        val receiver = intent?.getStringExtra("receiver")
                        
                        when (resultCode) {
                            android.app.Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS sent successfully to $receiver (message ID: $messageId)")
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                Log.e(TAG, "Generic failure sending SMS to $receiver (message ID: $messageId)")
                                onFailure()
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                Log.e(TAG, "No service sending SMS to $receiver (message ID: $messageId)")
                                onFailure()
                            }
                            SmsManager.RESULT_ERROR_NULL_PDU -> {
                                Log.e(TAG, "Null PDU sending SMS to $receiver (message ID: $messageId)")
                                onFailure()
                            }
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                Log.e(TAG, "Radio off sending SMS to $receiver (message ID: $messageId)")
                                onFailure()
                            }
                            else -> {
                                Log.e(TAG, "Unknown error ($resultCode) sending SMS to $receiver (message ID: $messageId)")
                                onFailure()
                            }
                        }
                        context?.unregisterReceiver(this)
                    }
                }
                
                val deliveredReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val messageId = intent?.getStringExtra("message_id")
                        val receiver = intent?.getStringExtra("receiver")
                        
                        when (resultCode) {
                            android.app.Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS delivered to $receiver (message ID: $messageId)")
                                onSuccess()
                            }
                            else -> {
                                Log.e(TAG, "SMS delivery failed to $receiver (message ID: $messageId, result: $resultCode)")
                                onFailure()
                            }
                        }
                        context?.unregisterReceiver(this)
                    }
                }
                
                context.registerReceiver(
                    sentReceiver,
                    IntentFilter(SMS_SENT_ACTION),
                    Context.RECEIVER_NOT_EXPORTED
                )
                context.registerReceiver(
                    deliveredReceiver,
                    IntentFilter(SMS_DELIVERED_ACTION),
                    Context.RECEIVER_NOT_EXPORTED
                )
                
                // Format phone number with country code if needed
                val formattedNumber = formatPhoneNumber(message.receiver)
                Log.d(TAG, "Formatted phone number: $formattedNumber (original: ${message.receiver})")
                
                smsManagerForSub.sendTextMessage(
                    formattedNumber,
                    null,
                    message.message,
                    sentPendingIntent,
                    deliveredPendingIntent
                )
                Log.d(TAG, "SMS queued for sending to $formattedNumber")
            } else {
                Log.e(TAG, "Subscription is null for subscription ID: $subscriptionId")
                onFailure()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: SMS permission denied", e)
            onFailure()
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending SMS", e)
            onFailure()
        }
    }
    
    private fun formatPhoneNumber(phoneNumber: String): String {
        // Remove any non-digit characters
        val digits = phoneNumber.replace(Regex("[^0-9]"), "")
        
        // If it starts with 0 and has 11 digits (Philippian format), add country code
        return if (digits.startsWith("0") && digits.length == 11) {
            "+63" + digits.substring(1)
        } else if (!digits.startsWith("+") && digits.length == 10) {
            // If 10 digits without country code, assume Philippines
            "+63$digits"
        } else {
            phoneNumber
        }
    }
}
