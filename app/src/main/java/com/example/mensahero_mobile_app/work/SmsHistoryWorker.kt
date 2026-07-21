package com.example.mensahero_mobile_app.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.model.HistoryMessageDto
import com.example.mensahero_mobile_app.data.model.SubmitHistoryResultRequest
import com.example.mensahero_mobile_app.data.sms.SmsHistoryReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Handles an on-demand "SMS thread history" request: reads the last few local SMS
 * for an address and POSTs the result back to the backend.
 *
 * Design notes:
 *  - Any inability to read (missing credentials, no permission, read error) is
 *    reported to the server as `failed = true` so the dashboard resolves to FAILED
 *    instead of hanging until the server-side 30s TIMEOUT.
 *  - A non-2xx POST (job already resolved / not ours) is NOT retried.
 *  - Only a genuine transient network failure of the POST triggers a single retry;
 *    a long backoff is pointless given the 30s server timeout.
 */
class SmsHistoryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsHistoryWorker"

        const val KEY_JOB_ID = "jobId"
        const val KEY_ADDRESS = "address"

        private const val MAX_ATTEMPTS = 2
    }

    private val apiService = MensaheroApiService()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobId = inputData.getString(KEY_JOB_ID)
        val address = inputData.getString(KEY_ADDRESS)

        if (jobId.isNullOrBlank() || address.isNullOrBlank()) {
            Log.e(TAG, "Missing jobId or address in input data — nothing to resolve")
            return@withContext Result.success()
        }

        val prefs = PreferencesManager(applicationContext)
        val apiKey = prefs.apiKey.first()
        val deviceId = prefs.deviceId.first()

        if (apiKey.isNullOrBlank() || deviceId.isNullOrBlank()) {
            Log.e(TAG, "Missing apiKey/deviceId — reporting failure for job $jobId")
            return@withContext post(
                SubmitHistoryResultRequest(
                    apiKey = apiKey.orEmpty(),
                    deviceId = deviceId.orEmpty(),
                    jobId = jobId,
                    failed = true,
                    failureReason = "Device credentials not available"
                )
            )
        }

        if (!hasReadSmsPermission()) {
            Log.w(TAG, "READ_SMS not granted — reporting failure for job $jobId")
            return@withContext post(
                SubmitHistoryResultRequest(
                    apiKey = apiKey,
                    deviceId = deviceId,
                    jobId = jobId,
                    failed = true,
                    failureReason = "READ_SMS permission not granted"
                )
            )
        }

        val messages: List<HistoryMessageDto> = try {
            SmsHistoryReader(applicationContext).readHistory(address)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading SMS for job $jobId", e)
            return@withContext post(
                SubmitHistoryResultRequest(
                    apiKey = apiKey,
                    deviceId = deviceId,
                    jobId = jobId,
                    failed = true,
                    failureReason = "READ_SMS permission not granted"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read SMS history for job $jobId", e)
            return@withContext post(
                SubmitHistoryResultRequest(
                    apiKey = apiKey,
                    deviceId = deviceId,
                    jobId = jobId,
                    failed = true,
                    failureReason = "Failed to read local SMS: ${e.message}"
                )
            )
        }

        // Empty list is a valid COMPLETED result, not a failure.
        Log.d(TAG, "Read ${messages.size} messages for job $jobId — submitting")
        return@withContext post(
            SubmitHistoryResultRequest(
                apiKey = apiKey,
                deviceId = deviceId,
                jobId = jobId,
                failed = false,
                messages = messages
            )
        )
    }

    /**
     * POSTs the result. A [Result.failure] from the API means a transient network
     * error — retry once. Any HTTP response received (including non-2xx) resolves as
     * success and stops the worker.
     */
    private suspend fun post(request: SubmitHistoryResultRequest): Result {
        val result = apiService.submitHistoryResult(request)
        return if (result.isSuccess) {
            Result.success()
        } else if (runAttemptCount + 1 < MAX_ATTEMPTS) {
            Log.w(TAG, "POST failed (attempt ${runAttemptCount + 1}) — retrying job ${request.jobId}")
            Result.retry()
        } else {
            Log.e(TAG, "POST failed after $MAX_ATTEMPTS attempts — giving up on job ${request.jobId}")
            Result.success()
        }
    }

    private fun hasReadSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
