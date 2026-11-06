package com.example.call.telecom

import android.content.Context
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.call.data.ContactsRepository
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallPhase { RINGING, DIALING, ACTIVE, HOLDING, ENDED }

/** Everything the call screen and the notifications need to know about the current call. */
data class CallUiState(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val phase: CallPhase,
    val connectedAtMillis: Long,
    val isMuted: Boolean,
    val isSpeaker: Boolean,
    /** Name or number of a second call that is on hold, if any. */
    val heldTitle: String? = null
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
}

/**
 * Bridge between Android's Telecom framework (via [CallInCallService]) and our UI.
 * All real answering / rejecting / hanging up goes through [Call] objects supplied by Telecom,
 * which only happens while this app holds the default dialer role.
 */
object CallManager {

    private data class Identity(val number: String, val name: String?, val photoUri: String?)

    private lateinit var contacts: ContactsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val calls = ArrayList<Call>()
    private val identities = HashMap<Call, Identity>()
    private var service: InCallService? = null

    private val _state = MutableStateFlow<CallUiState?>(null)
    val state: StateFlow<CallUiState?> = _state.asStateFlow()

    fun init(context: Context) {
        contacts = ContactsRepository(context.applicationContext)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = refresh()
        override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
    }

    // ---- Events from the InCallService -------------------------------------------------------

    fun onCallAdded(owner: InCallService, call: Call) {
        service = owner
        calls += call
        call.registerCallback(callback)
        val number = numberOf(call)
        identities[call] = Identity(number, null, null)
        refresh()

        // Look up the caller in the real phone contacts to get their name and photo.
        scope.launch {
            val found = contacts.findByNumber(number)
            if (found != null && calls.contains(call)) {
                identities[call] = Identity(number, found.name, found.photoUri)
                refresh()
            }
        }
    }

    fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        calls.remove(call)
        identities.remove(call)
        // The other call was waiting on hold: bring it back automatically.
        val onlyHeld = calls.singleOrNull()
        if (onlyHeld != null && onlyHeld.state == Call.STATE_HOLDING) onlyHeld.unhold()
        refresh()
    }

    fun onAudioChanged() = refresh()

    fun onServiceDestroyed(owner: InCallService) {
        if (service === owner) service = null
        calls.toList().forEach { it.unregisterCallback(callback) }
        calls.clear()
        identities.clear()
        _state.value = null
    }

    // ---- Actions from the UI -----------------------------------------------------------------

    /** Answers the ringing call. Returns false if there is nothing to answer. */
    fun answer(): Boolean {
        val call = ringingCall() ?: return false
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
        return true
    }

    /** Declines the ringing call. */
    fun reject(): Boolean {
        val call = ringingCall() ?: return false
        call.reject(false, null)
        return true
    }

    /** Ends the current call (active, dialing or on hold). */
    fun hangUp(): Boolean {
        val call = currentCall() ?: return false
        if (call.state == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
        return true
    }

    /** Puts the current call on hold and opens the dialer so a second number can be called. */
    fun addCall(context: Context) {
        val call = currentCall()
        if (call != null && call.state == Call.STATE_ACTIVE) call.hold()
        val intent = Intent(context, com.example.call.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(com.example.call.MainActivity.EXTRA_OPEN_DIALER, true)
        runCatching { context.startActivity(intent) }
    }

    /** Switches between the active call and the call on hold. */
    fun swap(): Boolean {
        val held = heldCall() ?: return false
        val active = currentCall()
        if (active != null && active !== held && active.state == Call.STATE_ACTIVE) active.hold()
        held.unhold()
        return true
    }

    /** Sends a keypad tone (for menus like "press 1 for ...") on the current call. */
    fun sendDtmf(digit: Char) {
        val call = currentCall() ?: return
        call.playDtmfTone(digit)
        scope.launch {
            delay(160)
            call.stopDtmfTone()
        }
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    fun setSpeaker(enabled: Boolean) {
        service?.setAudioRoute(
            if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
        )
    }

    // ---- Internals ---------------------------------------------------------------------------

    private fun ringingCall(): Call? = calls.firstOrNull { it.state == Call.STATE_RINGING }

    private fun currentCall(): Call? =
        ringingCall()
            ?: calls.firstOrNull {
                it.state != Call.STATE_DISCONNECTED &&
                    it.state != Call.STATE_DISCONNECTING &&
                    it.state != Call.STATE_HOLDING
            }
            ?: calls.firstOrNull { it.state == Call.STATE_HOLDING }
            ?: calls.firstOrNull()

    private fun heldCall(): Call? {
        val current = currentCall()
        return calls.firstOrNull { it !== current && it.state == Call.STATE_HOLDING }
    }

    private fun numberOf(call: Call): String =
        call.details?.handle?.schemeSpecificPart.orEmpty()

    @Suppress("DEPRECATION")
    private fun refresh() {
        val call = currentCall()
        if (call == null) {
            _state.value = null
            return
        }
        val id = identities[call] ?: Identity(numberOf(call), null, null)
        val audio = service?.callAudioState
        _state.value = CallUiState(
            number = id.number,
            name = id.name,
            photoUri = id.photoUri,
            phase = phaseOf(call.state),
            connectedAtMillis = call.details?.connectTimeMillis ?: 0L,
            isMuted = audio?.isMuted ?: false,
            isSpeaker = audio?.route == CallAudioState.ROUTE_SPEAKER,
            heldTitle = heldCall()?.let { h ->
                val hid = identities[h]
                hid?.name?.takeIf { it.isNotBlank() } ?: hid?.number?.ifBlank { null } ?: "Unknown"
            }
        )
    }

    private fun phaseOf(state: Int): CallPhase = when (state) {
        Call.STATE_RINGING -> CallPhase.RINGING
        Call.STATE_ACTIVE -> CallPhase.ACTIVE
        Call.STATE_HOLDING -> CallPhase.HOLDING
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> CallPhase.ENDED
        else -> CallPhase.DIALING
    }
}
