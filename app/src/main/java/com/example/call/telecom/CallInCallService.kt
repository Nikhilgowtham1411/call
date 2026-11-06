package com.example.call.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Telecom binds to this service for every call once the app is the default dialer.
 * It forwards calls to [CallManager], shows notifications and opens the call screen.
 */
class CallInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastPhase: CallPhase? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            CallManager.state.collect { render(it) }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(this, call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.onCallRemoved(call)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioChanged()
    }

    override fun onDestroy() {
        scope.cancel()
        CallNotifications.cancel(this)
        CallManager.onServiceDestroyed(this)
        super.onDestroy()
    }

    private fun render(state: CallUiState?) {
        if (state == null) {
            lastPhase = null
            CallNotifications.cancel(this)
            return
        }
        CallNotifications.show(this, state)

        // First sight of a call: also try to bring the call screen up directly. Android may
        // block this from the background, in which case the full-screen notification does it.
        // Also when a second call starts dialing or rings while the first one is going on.
        val cameUp = state.phase != lastPhase &&
            (state.phase == CallPhase.RINGING || state.phase == CallPhase.DIALING || lastPhase == null)
        lastPhase = state.phase
        if (cameUp && state.phase != CallPhase.ENDED) {
            runCatching { startActivity(CallNotifications.activityIntent(this)) }
        }
    }
}
