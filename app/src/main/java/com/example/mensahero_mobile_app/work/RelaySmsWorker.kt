package com.example.mensahero_mobile_app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.model.ReceivedSmsBody
import kotlinx.coroutines.flow.first

/**
 * Relays a captured incoming SMS to the backend via POST /api/messages/gateway/received.
 *
 * WorkManager persists this job to disk, so the upload survives no-network (via the
 * CONNECTED constraint), process death, and reboots. Delivery is at-least-once and
 * best-effort — there is no server-side de-duplication today.
 *
 * Response handling (decided):
 *  - 204            => success.
 *  - 400/401/403/404 => terminal; retrying cannot fix these, so give up.
 *  - 5xx / network   => transient; let WorkManager back off and retry.
 */
class RelaySmsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RelaySmsWorker"

        const val KEY_FROM = "from"
        const val KEY_MESSAGE = "message"
    }

    private val apiService = MensaheroApiService()

    override suspend fun doWork(): Result {
        val from = inputData.getString(KEY_FROM) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()

        val prefs = PreferencesManager(applicationContext)
        val apiKey = prefs.apiKey.first()
        val deviceId = prefs.deviceId.first()

        if (apiKey.isNullOrBlank() || deviceId.isNullOrBlank()) {
            // Not registered — nothing we can do; don't loop forever.
            Log.e(TAG, "Missing apiKey/deviceId — dropping incoming SMS relay from $from")
            return Result.failure()
        }

        val body = ReceivedSmsBody(deviceId = deviceId, from = from, message = message)

        val result = apiService.relayReceivedSms(apiKey, body)
        val status = result.getOrNull()

        return when {
            // Network / IO failure — retry under WorkManager constraints.
            result.isFailure -> Result.retry()
            status == 204 -> Result.success()
            // Terminal: bad auth, device/key mismatch, unknown device, bad body.
            status == 400 || status == 401 || status == 403 || status == 404 -> {
                Log.e(TAG, "Terminal status=$status relaying SMS from $from — giving up")
                Result.failure()
            }
            // 5xx / anything else transient.
            else -> {
                Log.w(TAG, "Transient status=$status relaying SMS from $from — retrying")
                Result.retry()
            }
        }
    }
}
