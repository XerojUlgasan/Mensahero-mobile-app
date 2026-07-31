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
import com.example.mensahero_mobile_app.data.model.HistoryFailureReason
import com.example.mensahero_mobile_app.data.model.HistoryMessageDto
import com.example.mensahero_mobile_app.data.model.SubmitHistoryResultRequest
import com.example.mensahero_mobile_app.data.sms.SmsHistoryReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Handles an on-demand "SMS thread history" request (FCM `type == "SMS_HISTORY"`):
 * reads exactly the requested page of local SMS for an address and POSTs the result
 * back to the backend, which relays it to the browser over SSE.
 *
 * Design notes:
 *  - Any inability to read (missing credentials, no permission, read error) is
 *    reported to the server as `failed = true` with a fixed [HistoryFailureReason]
 *    so the browser resolves instead of hanging until the server-side timeout.
 *  - An empty result is a valid success, not a failure.
 *  - A non-2xx POST (410/403/401/400) is terminal and never retried.
 *  - Only a genuine transient network failure of the POST triggers a single retry;
 *    a long backoff is pointless given the ~45s server TTL.
 */
class SmsHistoryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsHistoryWorker"

        const val KEY_REQUEST_ID = "requestId"
        const val KEY_ADDRESS = "address"
        const val KEY_PAGE_SIZE = "pageSize"
        const val KEY_PAGE_NUMBER = "pageNumber"

        private const val MAX_PAGE_SIZE = 25
        private const val MAX_ATTEMPTS = 2
    }

    private val apiService = MensaheroApiService()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val requestId = inputData.getString(KEY_REQUEST_ID)
        val address = inputData.getString(KEY_ADDRESS)
        val pageSize = inputData.getInt(KEY_PAGE_SIZE, MAX_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val pageNumber = inputData.getInt(KEY_PAGE_NUMBER, 0).coerceAtLeast(0)

        if (requestId.isNullOrBlank() || address.isNullOrBlank()) {
            Log.e(TAG, "Missing requestId or address in input data — nothing to resolve")
            return@withContext Result.success()
        }

        val prefs = PreferencesManager(applicationContext)
        val apiKey = prefs.apiKey.first()
        val deviceId = prefs.deviceId.first()

        if (apiKey.isNullOrBlank() || deviceId.isNullOrBlank()) {
            Log.e(TAG, "Missing apiKey/deviceId — reporting failure for request $requestId")
            return@withContext post(
                apiKey.orEmpty(),
                failureBody(
                    deviceId = deviceId.orEmpty(),
                    requestId = requestId,
                    address = address,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    reason = HistoryFailureReason.GATEWAY_FAILURE,
                )
            )
        }

        if (!hasReadSmsPermission()) {
            Log.w(TAG, "READ_SMS not granted — reporting failure for request $requestId")
            return@withContext post(
                apiKey,
                failureBody(deviceId, requestId, address, pageSize, pageNumber, HistoryFailureReason.PERMISSION_DENIED)
            )
        }

        val messages: List<HistoryMessageDto> = try {
            SmsHistoryReader(applicationContext).readPage(address, pageSize, pageNumber)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading SMS for request $requestId", e)
            return@withContext post(
                apiKey,
                failureBody(deviceId, requestId, address, pageSize, pageNumber, HistoryFailureReason.PERMISSION_DENIED)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read SMS history for request $requestId", e)
            return@withContext post(
                apiKey,
                failureBody(deviceId, requestId, address, pageSize, pageNumber, HistoryFailureReason.GATEWAY_FAILURE)
            )
        }

        // Empty list is a valid success, not a failure.
        Log.d(TAG, "Read ${messages.size} messages for request $requestId — submitting")
        return@withContext post(
            apiKey,
            SubmitHistoryResultRequest(
                deviceId = deviceId,
                requestId = requestId,
                address = address,
                pageSize = pageSize,
                pageNumber = pageNumber,
                failed = false,
                reason = null,
                messages = messages,
            )
        )
    }

    private fun failureBody(
        deviceId: String,
        requestId: String,
        address: String,
        pageSize: Int,
        pageNumber: Int,
        reason: String,
    ) = SubmitHistoryResultRequest(
        deviceId = deviceId,
        requestId = requestId,
        address = address,
        pageSize = pageSize,
        pageNumber = pageNumber,
        failed = true,
        reason = reason,
        messages = emptyList(),
    )

    /**
     * POSTs the result. A [Result.failure] from the API means a transient network
     * error — retry once. Any HTTP response received (including terminal 410/403/
     * 401/400) resolves as success and stops the worker; those are never retried.
     */
    private suspend fun post(apiKey: String, request: SubmitHistoryResultRequest): Result {
        val result = apiService.submitHistoryResult(apiKey, request)
        return if (result.isSuccess) {
            Result.success()
        } else if (runAttemptCount + 1 < MAX_ATTEMPTS) {
            Log.w(TAG, "POST failed (attempt ${runAttemptCount + 1}) — retrying request ${request.requestId}")
            Result.retry()
        } else {
            Log.e(TAG, "POST failed after $MAX_ATTEMPTS attempts — giving up on request ${request.requestId}")
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
