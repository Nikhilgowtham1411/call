package com.example.call.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.call.data.model.CallKind
import com.example.call.data.model.CallLogEntry
import com.example.call.system.Permissions
import com.example.call.ui.components.CircleContactPhoto
import com.example.call.ui.components.EmptyState
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PermissionCard
import com.example.call.ui.components.RoundIconButton
import com.example.call.ui.components.SoftCard
import com.example.call.ui.rememberCaller
import com.example.call.ui.rememberPermissionRequester
import com.example.call.ui.theme.CallGreen
import com.example.call.ui.theme.DeclineRed
import com.example.call.ui.viewmodel.CallFilter
import com.example.call.ui.viewmodel.CallLogViewModel

@Composable
fun LogsScreen(viewModel: CallLogViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val call = rememberCaller()
    var permissionKey by remember { mutableIntStateOf(0) }
    val hasLog = remember(permissionKey, state.entries) { Permissions.canReadCallLog(context) }

    val askLog = rememberPermissionRequester(Permissions.callLog + Permissions.contacts) {
        permissionKey++
        viewModel.refresh()
    }

    FloralBackground {
        Column(Modifier.fillMaxSize()) {
            FloralHeader(title = "Call Logs")

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CallFilter.entries.forEach { filter ->
                    FilterTab(
                        label = filter.label,
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) }
                    )
                }
            }

            val list = state.visible
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    !hasLog -> item {
                        PermissionCard(
                            title = "See my calls",
                            message = "Allow Call to read your call history.",
                            onAllow = askLog,
                            onOpenSettings = { context.startActivity(Permissions.appDetailsSettings(context)) }
                        )
                    }
                    list.isEmpty() && !state.isLoading -> item {
                        EmptyState("No calls here yet.")
                    }
                    else -> items(list, key = { it.id }) { entry ->
                        LogRow(entry = entry, onCall = { if (entry.number.isNotBlank()) call(entry.number) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun LogRow(entry: CallLogEntry, onCall: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleContactPhoto(entry.photoUri, entry.title, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint, label) = kindStyle(entry.kind)
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$label • ${formatWhen(entry)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (entry.durationSeconds > 0 && entry.kind != CallKind.MISSED) {
                    Text(
                        "Talked ${formatDuration(entry.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RoundIconButton(
                icon = Icons.Filled.Call,
                description = "Call ${entry.title} back",
                onClick = onCall,
                size = 60.dp
            )
        }
    }
}

private data class KindStyle(val icon: ImageVector, val tint: Color, val label: String)

private fun kindStyle(kind: CallKind): KindStyle = when (kind) {
    CallKind.INCOMING -> KindStyle(Icons.AutoMirrored.Filled.CallReceived, CallGreen, "Incoming")
    CallKind.OUTGOING -> KindStyle(Icons.AutoMirrored.Filled.CallMade, Color(0xFF2E7DD1), "Outgoing")
    CallKind.MISSED -> KindStyle(Icons.AutoMirrored.Filled.CallMissed, DeclineRed, "Missed")
    CallKind.OTHER -> KindStyle(Icons.Filled.History, Color(0xFF7A6A94), "Call")
}

@Composable
private fun formatWhen(entry: CallLogEntry): String {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val flags = DateUtils.FORMAT_SHOW_TIME
    return when {
        DateUtils.isToday(entry.dateMillis) ->
            DateUtils.formatDateTime(context, entry.dateMillis, flags)
        DateUtils.isToday(entry.dateMillis + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        now - entry.dateMillis < 7 * DateUtils.DAY_IN_MILLIS ->
            DateUtils.formatDateTime(context, entry.dateMillis, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or flags)
        else ->
            DateUtils.formatDateTime(context, entry.dateMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or flags)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
