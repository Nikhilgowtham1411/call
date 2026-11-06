package com.example.call.telecom

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.call.ui.theme.CallGreen
import com.example.call.ui.theme.DeclineRed
import com.example.call.ui.theme.DeepPurple
import kotlinx.coroutines.delay

/**
 * Full-screen call screen. The caller's real contact photo fills the whole screen.
 * Answer and Decline are two separate large buttons; there are no swipe gestures.
 */
class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        handleIntent(intent)

        setContent {
            val state by CallManager.state.collectAsState()
            LaunchedEffect(state == null) {
                if (state == null) finish()
            }
            state?.let { s ->
                InCallScreen(
                    state = s,
                    onAnswer = { CallManager.answer() },
                    onDecline = { CallManager.reject() },
                    onHangUp = { CallManager.hangUp() },
                    onMute = { CallManager.setMuted(!s.isMuted) },
                    onSpeaker = { CallManager.setSpeaker(!s.isSpeaker) },
                    onAddCall = { CallManager.addCall(this@InCallActivity) },
                    onSwap = { CallManager.swap() },
                    onDtmf = { CallManager.sendDtmf(it) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == CallNotifications.ACTION_ANSWER) {
            CallManager.answer()
            intent.action = null
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun InCallScreen(
    state: CallUiState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onAddCall: () -> Unit,
    onSwap: () -> Unit,
    onDtmf: (Char) -> Unit
) {
    var showKeypad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    // A new incoming call replaces the keypad with Answer / Decline.
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.RINGING) showKeypad = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF8E5BD6), DeepPurple, Color(0xFF3A1D66))))
    ) {
        // Layer 1: the caller's contact photo, edge to edge. Letter avatar when there is none.
        if (state.photoUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo of ${state.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialOf(state.title),
                        color = Color.White,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Layer 2: darkening scrim so white text stays readable on any photo.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xB3000000),
                        0.35f to Color(0x33000000),
                        0.6f to Color(0x66000000),
                        1.0f to Color(0xE6000000)
                    )
                )
        )

        // Layer 3: text and buttons.
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = statusLabel(state),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.title,
                    color = Color.White,
                    fontSize = 46.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.name != null && state.number.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.number,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(36.dp))

                if (state.phase == CallPhase.RINGING) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        RoundCallButton(
                            color = DeclineRed,
                            icon = Icons.Filled.CallEnd,
                            label = "Decline",
                            description = "Decline call",
                            onClick = onDecline
                        )
                        RoundCallButton(
                            color = CallGreen,
                            icon = Icons.Filled.Call,
                            label = "Answer",
                            description = "Answer call",
                            onClick = onAnswer
                        )
                    }
                } else if (showKeypad) {
                    KeypadPanel(
                        typed = typed,
                        onKey = { c ->
                            typed += c
                            onDtmf(c)
                        },
                        onHide = { showKeypad = false },
                        onHangUp = onHangUp
                    )
                } else {
                    if (state.heldTitle != null) {
                        Text(
                            text = "On hold: ${state.heldTitle}",
                            color = Color.White,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        ToggleCallButton(
                            selected = state.isMuted,
                            icon = if (state.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (state.isMuted) "Unmute" else "Mute",
                            onClick = onMute
                        )
                        ToggleCallButton(
                            selected = false,
                            icon = Icons.Filled.Dialpad,
                            label = "Keypad",
                            onClick = { showKeypad = true }
                        )
                        ToggleCallButton(
                            selected = state.isSpeaker,
                            icon = Icons.Filled.VolumeUp,
                            label = "Speaker",
                            onClick = onSpeaker
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        ToggleCallButton(
                            selected = false,
                            icon = Icons.Filled.Add,
                            label = "Add call",
                            onClick = onAddCall
                        )
                        RoundCallButton(
                            color = DeclineRed,
                            icon = Icons.Filled.CallEnd,
                            label = "Hang up",
                            description = "End call",
                            onClick = onHangUp,
                            size = 88.dp
                        )
                        if (state.heldTitle != null) {
                            ToggleCallButton(
                                selected = false,
                                icon = Icons.Filled.SwapCalls,
                                label = "Swap",
                                onClick = onSwap
                            )
                        } else {
                            Box(Modifier.size(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadPanel(
    typed: String,
    onKey: (Char) -> Unit,
    onHide: () -> Unit,
    onHangUp: () -> Unit
) {
    val rows = listOf("123", "456", "789", "*0#")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = typed.takeLast(14),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.height(44.dp)
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { c ->
                    Box(
                        Modifier
                            .padding(vertical = 4.dp)
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable(role = Role.Button) { onKey(c) }
                            .semantics { contentDescription = "Key $c" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Hide",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(role = Role.Button, onClick = onHide)
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            )
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DeclineRed)
                    .clickable(role = Role.Button, onClick = onHangUp)
                    .semantics { contentDescription = "End call" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun statusLabel(state: CallUiState): String = when (state.phase) {
    CallPhase.RINGING -> "Incoming Call"
    CallPhase.DIALING -> "Calling…"
    CallPhase.HOLDING -> "On hold"
    CallPhase.ENDED -> "Call ended"
    CallPhase.ACTIVE -> {
        val seconds by produceState(initialValue = elapsedSeconds(state.connectedAtMillis), state.connectedAtMillis) {
            while (true) {
                value = elapsedSeconds(state.connectedAtMillis)
                delay(1000)
            }
        }
        formatDuration(seconds)
    }
}

private fun elapsedSeconds(connectedAt: Long): Long =
    if (connectedAt <= 0) 0 else ((System.currentTimeMillis() - connectedAt) / 1000).coerceAtLeast(0)

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun initialOf(title: String): String =
    title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

/** Big tap-only circular button. Responds immediately on tap. */
@Composable
private fun RoundCallButton(
    color: Color,
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    size: Dp = 104.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToggleCallButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.White.copy(alpha = 0.25f))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) DeepPurple else Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}
