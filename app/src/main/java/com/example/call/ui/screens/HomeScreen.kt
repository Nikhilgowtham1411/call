package com.example.call.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.call.system.DialerRole
import com.example.call.system.Permissions
import com.example.call.ui.components.ContactActionsDialog
import com.example.call.ui.components.ContactGridCard
import com.example.call.ui.components.DefaultDialerBanner
import com.example.call.ui.components.EmptyState
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PermissionCard
import com.example.call.ui.components.RoundIconButton
import com.example.call.ui.rememberCaller
import com.example.call.ui.rememberPermissionRequester
import com.example.call.ui.rememberResumeTick
import com.example.call.ui.viewmodel.ContactsViewModel

@Composable
fun HomeScreen(
    viewModel: ContactsViewModel,
    onOpenSettings: () -> Unit,
    onSetUpDialer: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val call = rememberCaller()
    var actionsFor by remember { mutableStateOf<com.example.call.data.model.Contact?>(null) }

    val tick = rememberResumeTick()
    var refreshKey by remember { mutableIntStateOf(0) }
    val hasContacts = remember(tick, refreshKey) { Permissions.canReadContacts(context) }
    val isDefaultDialer = remember(tick, refreshKey) { DialerRole.isDefaultDialer(context) }

    val askContacts = rememberPermissionRequester(Permissions.contacts) {
        refreshKey++
        viewModel.refresh()
    }

    FloralBackground {
        Column(Modifier.fillMaxSize()) {
            FloralHeader(
                title = "❤️ Call",
                subtitle = "Tap a photo to call. Hold to pin.",
                trailing = {
                    RoundIconButton(
                        icon = Icons.Filled.Settings,
                        description = "Settings",
                        onClick = onOpenSettings,
                        size = 52.dp,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                },
                leading = { Box(Modifier.padding(26.dp)) }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isDefaultDialer) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        DefaultDialerBanner(onSetUp = onSetUpDialer)
                    }
                }
                when {
                    !hasContacts -> item(span = { GridItemSpan(maxLineSpan) }) {
                        PermissionCard(
                            title = "Show my family",
                            message = "Allow Call to see your contacts so their photos appear here.",
                            onAllow = askContacts,
                            onOpenSettings = { context.startActivity(Permissions.appDetailsSettings(context)) }
                        )
                    }
                    state.contacts.isEmpty() && !state.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState("No contacts with phone numbers yet.\nTap Contacts below to add one.")
                    }
                    else -> items(state.ordered, key = { it.id }) { contact ->
                        ContactGridCard(
                            contact = contact,
                            pinned = state.isPinned(contact),
                            onCall = { call(contact.number) },
                            onLongPress = { actionsFor = contact },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    actionsFor?.let { c ->
        ContactActionsDialog(
            contact = c,
            pinned = state.isPinned(c),
            onTogglePin = { viewModel.togglePin(c) },
            onDismiss = { actionsFor = null }
        )
    }
}
