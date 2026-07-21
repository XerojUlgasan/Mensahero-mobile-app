package com.example.mensahero_mobile_app.data.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.example.mensahero_mobile_app.data.model.HistoryMessageDto

/**
 * Reads the local SMS store for the last messages exchanged with a given address.
 *
 * Phone numbers may be stored in a different format than requested
 * (e.g. "09171234567" vs "+639171234567"), so we pre-filter the query on the
 * shared trailing digits and then confirm each candidate with
 * [PhoneNumberUtils.compare] for correctness.
 */
class SmsHistoryReader(private val context: Context) {

    companion object {
        private const val TAG = "SmsHistoryReader"
        private const val MAX_MESSAGES = 10

        // "SENT" / "RECEIVED" as required by the backend contract.
        private const val DIRECTION_SENT = "SENT"
        private const val DIRECTION_RECEIVED = "RECEIVED"
    }

    /**
     * Returns the newest [MAX_MESSAGES] messages (across inbox + sent) for [address],
     * newest first.
     *
     * @throws SecurityException if READ_SMS is not granted.
     * @throws Exception on any other read failure — the caller is expected to report
     *         a failure result rather than staying silent.
     */
    fun readHistory(address: String): List<HistoryMessageDto> {
        val received = queryBox(
            Telephony.Sms.Inbox.CONTENT_URI,
            address,
            DIRECTION_RECEIVED
        )
        val sent = queryBox(
            Telephony.Sms.Sent.CONTENT_URI,
            address,
            DIRECTION_SENT
        )

        return (received + sent)
            .sortedByDescending { it.timestamp }
            .take(MAX_MESSAGES)
    }

    private fun queryBox(
        uri: Uri,
        requestedAddress: String,
        direction: String
    ): List<HistoryMessageDto> {
        val results = mutableListOf<HistoryMessageDto>()

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        // Pre-filter on the shared trailing digits so different formats
        // (0917… vs +63917…) still match, then confirm precisely below.
        val suffix = trailingDigits(requestedAddress, 9)
        val selection: String?
        val selectionArgs: Array<String>?
        if (suffix.isNotEmpty()) {
            selection = "${Telephony.Sms.ADDRESS} LIKE ?"
            selectionArgs = arrayOf("%$suffix")
        } else {
            selection = null
            selectionArgs = null
        }

        // Fetch a small buffer beyond the cap since LIKE can over-match.
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT ${MAX_MESSAGES * 3}"

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

            while (cursor.moveToNext() && results.size < MAX_MESSAGES) {
                val storedAddress = if (addressIdx >= 0) cursor.getString(addressIdx) else null
                if (storedAddress == null) continue

                // Precise match regardless of formatting differences.
                if (!PhoneNumberUtils.compare(storedAddress, requestedAddress)) continue

                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) else null
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L

                results.add(
                    HistoryMessageDto(
                        body = body ?: "",
                        direction = direction,
                        timestamp = date
                    )
                )
            }
        }

        Log.d(TAG, "queryBox uri=$uri direction=$direction matched=${results.size}")
        return results
    }

    /** Returns the last [count] digits of [number], ignoring non-digit characters. */
    private fun trailingDigits(number: String, count: Int): String {
        val digits = number.filter { it.isDigit() }
        return if (digits.length <= count) digits else digits.takeLast(count)
    }
}
