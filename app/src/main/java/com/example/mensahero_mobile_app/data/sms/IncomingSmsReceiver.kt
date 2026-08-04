package com.example.mensahero_mobile_app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mensahero_mobile_app.work.RelaySmsWorker
import java.util.concurrent.TimeUnit

/**
 * Manifest-registered receiver for incoming SMS. Because it is declared in the
 * manifest (not context-registered), the OS starts our process to deliver the
 * broadcast even when the app UI is closed / the process was killed.
 *
 * onReceive runs on the main thread with a ~10s budget, so we do NO network here:
 * parse the SMS (joining multipart PDUs) and hand off to WorkManager, which persists
 * the upload across no-network, process death, and reboots.
 */
class IncomingSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // One SmsMessage per PDU. A long SMS is split into several parts that must be
        // joined, in order, per originating address.
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val from = parts[0].originatingAddress ?: return
        val body = buildString { parts.forEach { append(it.messageBody ?: "") } }
        if (body.isEmpty()) return

        val data = workDataOf(
            RelaySmsWorker.KEY_FROM to from,
            RelaySmsWorker.KEY_MESSAGE to body,
        )

        val request = OneTimeWorkRequestBuilder<RelaySmsWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
            // Expedited => run ASAP; fall back to normal work if quota is exhausted.
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d(TAG, "Incoming SMS from $from enqueued for relay (${body.length} chars)")
    }
}
