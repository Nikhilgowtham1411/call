package com.example.call.data

import android.content.Context
import android.os.Bundle
import android.provider.CallLog
import com.example.call.data.model.CallKind
import com.example.call.data.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the system call log. Needs READ_CALL_LOG; returns an empty list without it. */
class CallLogRepository(
    private val context: Context,
    private val contacts: ContactsRepository
) {

    suspend fun getEntries(limit: Int = 300): List<CallLogEntry> = withContext(Dispatchers.IO) {
        runCatching { query(limit) }.getOrDefault(emptyList())
    }

    private suspend fun query(limit: Int): List<CallLogEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val args = Bundle().apply {
            putStringArray(
                android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(CallLog.Calls.DATE)
            )
            putInt(
                android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
        }

        // Resolve each distinct number only once; photos come from the real contact.
        val lookupCache = HashMap<String, com.example.call.data.model.Contact?>()
        val rows = ArrayList<CallLogEntry>()

        context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, args, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(CallLog.Calls._ID)
            val iNumber = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val iName = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val iType = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val iDate = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val iDuration = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (c.moveToNext()) {
                val number = c.getString(iNumber).orEmpty()
                val contact = if (number.isBlank()) null else {
                    if (!lookupCache.containsKey(number)) lookupCache[number] = contacts.findByNumber(number)
                    lookupCache[number]
                }
                rows += CallLogEntry(
                    id = c.getLong(iId),
                    number = number,
                    name = contact?.name?.takeIf { it.isNotBlank() } ?: c.getString(iName),
                    photoUri = contact?.photoUri,
                    kind = kindOf(c.getInt(iType)),
                    dateMillis = c.getLong(iDate),
                    durationSeconds = c.getLong(iDuration)
                )
            }
        }
        return rows
    }

    private fun kindOf(type: Int): CallKind = when (type) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallKind.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallKind.OUTGOING
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallKind.MISSED
        else -> CallKind.OTHER
    }
}
