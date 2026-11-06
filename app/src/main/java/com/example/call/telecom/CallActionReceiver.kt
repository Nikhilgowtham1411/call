package com.example.call.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles Decline / Hang up taps on the call notification. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CallNotifications.ACTION_DECLINE -> CallManager.reject()
            CallNotifications.ACTION_HANGUP -> CallManager.hangUp()
        }
    }
}
