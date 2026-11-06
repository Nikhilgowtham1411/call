package com.example.call.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.example.call.R
import com.example.call.system.Permissions

/** Incoming-call and ongoing-call notifications, including the full-screen intent. */
object CallNotifications {

    const val CHANNEL_INCOMING = "incoming_calls"
    const val CHANNEL_ONGOING = "ongoing_calls"
    private const val NOTIFICATION_ID = 4711

    const val ACTION_ANSWER = "com.example.call.action.ANSWER"
    const val ACTION_DECLINE = "com.example.call.action.DECLINE"
    const val ACTION_HANGUP = "com.example.call.action.HANGUP"

    private const val PRIMARY_COLOR = 0xFF6335A5.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val incoming = NotificationChannel(
            CHANNEL_INCOMING,
            context.getString(R.string.channel_incoming_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_incoming_desc)
            // Telecom itself plays the ringtone and vibrates; the channel stays silent.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            context.getString(R.string.channel_ongoing_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(incoming)
        nm.createNotificationChannel(ongoing)
    }

    fun activityIntent(context: Context, action: String? = null): Intent =
        Intent(context, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (action != null) this.action = action
        }

    private fun activityPending(context: Context, requestCode: Int, action: String? = null): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode, activityIntent(context, action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun broadcastPending(context: Context, requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    @SuppressLint("MissingPermission")
    fun show(context: Context, state: CallUiState) {
        if (!Permissions.canPostNotifications(context)) return
        val notification = when (state.phase) {
            CallPhase.RINGING -> incoming(context, state)
            CallPhase.ENDED -> {
                cancel(context)
                return
            }
            else -> ongoing(context, state)
        }
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun incoming(context: Context, state: CallUiState): android.app.Notification {
        val caller = Person.Builder().setName(state.title).setImportant(true).build()
        val fullScreen = activityPending(context, 1)
        return NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state.title)
            .setContentText("Incoming call")
            .setSubText(state.number.takeIf { state.name != null })
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(PRIMARY_COLOR)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    broadcastPending(context, 2, ACTION_DECLINE),
                    activityPending(context, 3, ACTION_ANSWER)
                )
            )
            .build()
    }

    private fun ongoing(context: Context, state: CallUiState): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state.title)
            .setContentText(if (state.phase == CallPhase.DIALING) "Calling…" else "Ongoing call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(PRIMARY_COLOR)
            .setContentIntent(activityPending(context, 4))
            .addAction(
                R.drawable.ic_notification,
                "Hang up",
                broadcastPending(context, 5, ACTION_HANGUP)
            )
        if (state.phase == CallPhase.DIALING) {
            builder.setFullScreenIntent(activityPending(context, 4), true)
        }
        if (state.phase == CallPhase.ACTIVE && state.connectedAtMillis > 0) {
            builder.setUsesChronometer(true).setWhen(state.connectedAtMillis).setShowWhen(true)
        }
        return builder.build()
    }
}
