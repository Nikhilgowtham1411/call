package com.example.call.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.call.data.model.TextScale
import com.example.call.data.model.ThemeMode
import com.example.call.system.DialerRole
import com.example.call.system.Permissions
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PillButton
import com.example.call.ui.components.RoundIconButton
import com.example.call.ui.components.SoftCard
import com.example.call.ui.rememberPermissionRequester
import com.example.call.ui.rememberResumeTick
import com.example.call.ui.theme.CallGreen
import com.example.call.ui.theme.DeclineRed
import com.example.call.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSetUpDialer: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val tick = rememberResumeTick()


    val askAll = rememberPermissionRequester(Permissions.allRuntime()) { }

    FloralBackground {
        Column(Modifier.fillMaxSize()) {
            FloralHeader(
                title = "Settings",
                leading = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Back",
                        onClick = onBack,
                        size = 52.dp,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                },
                trailing = { Box(Modifier.size(52.dp)) }
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Section("Theme") {
                    ThemeMode.entries.forEach { mode ->
                        ChoiceRow(
                            label = when (mode) {
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.SYSTEM -> "Same as phone"
                            },
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) }
                        )
                    }
                }

                Section("Text size") {
                    TextScale.entries.forEach { scale ->
                        ChoiceRow(
                            label = scale.label,
                            selected = settings.textScale == scale,
                            onClick = { viewModel.setTextScale(scale) }
                        )
                    }
                }

                Section("Contact photos") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Show contact photos",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.showPhotos,
                            onCheckedChange = { viewModel.setShowPhotos(it) }
                        )
                    }
                }

                Section("Answer calls with big buttons") {
                    val isDefault = remember(tick) { DialerRole.isDefaultDialer(context) }
                    StatusLine(ok = isDefault, text = if (isDefault) "Call is your phone app" else "Call is not your phone app yet")
                    if (!isDefault) {
                        Text(
                            "Android only lets the default phone app answer or decline calls and show the full-screen photo. " +
                                "Tap the button below, then choose \"Call\" and confirm. You can switch back at any time in " +
                                "Phone Settings → Apps → Default apps → Phone app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PillButton(
                            text = "Make Call my phone app",
                            onClick = onSetUpDialer,
                            icon = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val fsOk = remember(tick) { Permissions.canUseFullScreenIntent(context) }
                    if (!fsOk) {
                        StatusLine(ok = false, text = "Full-screen incoming calls are turned off")
                        PillButton(
                            text = "Allow full-screen calls",
                            onClick = { context.startActivity(Permissions.fullScreenIntentSettings(context)) },
                            icon = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Section("Permissions") {
                    val items = remember(tick) {
                        listOf(
                            "Contacts" to Permissions.canReadContacts(context),
                            "Edit contacts" to Permissions.canWriteContacts(context),
                            "Call history" to Permissions.canReadCallLog(context),
                            "Make calls" to Permissions.canCall(context),
                            "Notifications" to Permissions.canPostNotifications(context)
                        )
                    }
                    items.forEach { (label, ok) -> StatusLine(ok = ok, text = label) }
                    if (items.any { !it.second }) {
                        PillButton(
                            text = "Allow missing permissions",
                            onClick = askAll,
                            icon = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PillButton(
                            text = "Open phone settings",
                            onClick = { context.startActivity(Permissions.appDetailsSettings(context)) },
                            icon = null,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = if (ok) "Allowed" else "Not allowed",
            tint = if (ok) CallGreen else DeclineRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
