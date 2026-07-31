package com.example.mensahero_mobile_app.data.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.example.mensahero_mobile_app.data.model.HistoryMessageDto

/**
 * Reads a single page of the local SMS store for a given address.
 *
 * Pagination happens at the source (SQLite `LIMIT`/`OFFSET`) rather than reading
 * everything and slicing in memory. Address matching uses the provider's
 * `PHONE_NUMBERS_EQUAL` so formatting differences (e.g. "09171234567" vs
 * "+639171234567") don't cause misses.
 */
class SmsHistoryReader(private val context: Context) {

    companion object {
        private const val TAG = "SmsHistoryReader"

        // "SENT" / "RECEIVED" as required by the backend contract.
        private const val DIRECTION_SENT = "SENT"
        private const val DIRECTION_RECEIVED = "RECEIVED"
    }

    /**
     * Returns the requested page of messages for [address], newest first.
     *
     * Only `LIMIT [pageSize] OFFSET ([pageNumber] * [pageSize])` rows are fetched,
     * so the result never exceeds [pageSize]. [pageNumber] is 0-based.
     *
     * @throws SecurityException if READ_SMS is not granted.
     * @throws Exception on any other provider error — the caller reports a failure
     *         result rather than staying silent.
     */
    fun readPage(address: String, pageSize: Int, pageNumber: Int): List<HistoryMessageDto> {
        val offset = pageNumber * pageSize
        val out = ArrayList<HistoryMessageDto>(pageSize)

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )

        // PHONE_NUMBERS_EQUAL tolerates +63 / 0 / spacing differences.
        val selection = "PHONE_NUMBERS_EQUAL(${Telephony.Sms.ADDRESS}, ?)"
        val selectionArgs = arrayOf(address)

        // The SMS provider is SQLite-backed, so LIMIT/OFFSET in sortOrder works
        // across all supported API levels.
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $pageSize OFFSET $offset"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            ?.use { c ->
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (c.moveToNext()) {
                    val type = c.getInt(typeIdx)
                    val direction =
                        if (type == Telephony.Sms.MESSAGE_TYPE_SENT) DIRECTION_SENT
                        else DIRECTION_RECEIVED

                    out.add(
                        HistoryMessageDto(
                            body = c.getString(bodyIdx) ?: "",
                            direction = direction,
                            timestamp = c.getLong(dateIdx), // Telephony DATE is already epoch millis
                        )
                    )
                }
            }

        Log.d(TAG, "readPage address=$address pageSize=$pageSize pageNumber=$pageNumber matched=${out.size}")
        return out
    }
}
