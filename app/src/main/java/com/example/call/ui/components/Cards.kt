package com.example.call.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.call.data.model.Contact

/**
 * Home-screen family card: big photo, name, big Call button.
 * Tap = call. Hold = actions (pin to top / unpin).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactGridCard(
    contact: Contact,
    pinned: Boolean,
    onCall: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier, onClick = onCall, onLongClick = onLongPress) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                SquareContactPhoto(
                    photoUri = contact.photoUri,
                    name = contact.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (pinned) PinBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            PillButton(
                text = "Call",
                onClick = onCall,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp
            )
        }
    }
}

/** Small purple pin shown on pinned contacts. */
@Composable
fun PinBadge(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 32.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PushPin,
            contentDescription = "Pinned",
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** Shown after holding a contact: big, simple choices. */
@Composable
fun ContactActionsDialog(
    contact: Contact,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(contact.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    text = if (pinned) "Unpin" else "Pin to top",
                    icon = Icons.Filled.PushPin,
                    onClick = {
                        onTogglePin()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 60.dp
                )
                PillButton(
                    text = "Cancel",
                    icon = null,
                    onClick = onDismiss,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    height = 60.dp
                )
            }
        },
        confirmButton = {}
    )
}

/** Shown in place of a list when a needed permission has not been granted. */
@Composable
fun PermissionCard(
    title: String,
    message: String,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            PillButton(text = "Allow", onClick = onAllow, icon = null, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onOpenSettings) {
                Text("Open phone settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Gentle prompt on the home screen until Call is the phone's default calling app. */
@Composable
fun DefaultDialerBanner(onSetUp: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Big photo for incoming calls",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Make Call the phone app so you can answer with big buttons.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
            PillButton(text = "Set up", onClick = onSetUp, icon = Icons.Filled.Settings, height = 52.dp)
        }
    }
}
