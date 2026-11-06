package com.example.call.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.call.data.model.Contact
import com.example.call.system.Permissions
import com.example.call.ui.components.CircleContactPhoto
import com.example.call.ui.components.ContactActionsDialog
import com.example.call.ui.components.PinBadge
import com.example.call.ui.components.EmptyState
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PermissionCard
import com.example.call.ui.components.RoundIconButton
import com.example.call.ui.components.SoftCard
import com.example.call.ui.rememberCaller
import com.example.call.ui.rememberPermissionRequester
import com.example.call.ui.viewmodel.ContactsViewModel

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onAdd: () -> Unit,
    onEdit: (Contact) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val call = rememberCaller()
    var searching by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Contact?>(null) }
    var actionsFor by remember { mutableStateOf<Contact?>(null) }
    var permissionKey by remember { mutableIntStateOf(0) }
    val hasContacts = remember(permissionKey, state.contacts) { Permissions.canReadContacts(context) }

    val askContacts = rememberPermissionRequester(Permissions.contacts + Permissions.contactsWrite) {
        permissionKey++
        viewModel.refresh()
    }

    FloralBackground {
        Column(Modifier.fillMaxSize()) {
            FloralHeader(
                title = "Contacts",
                leading = {
                    RoundIconButton(
                        icon = Icons.Filled.Add,
                        description = "Add contact",
                        onClick = onAdd,
                        size = 52.dp,
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                },
                trailing = {
                    RoundIconButton(
                        icon = if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        description = if (searching) "Close search" else "Search contacts",
                        onClick = {
                            searching = !searching
                            if (!searching) viewModel.onQueryChange("")
                        },
                        size = 52.dp,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                }
            )

            if (searching) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Search name or number", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }

            val list = state.filtered
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    !hasContacts -> item {
                        PermissionCard(
                            title = "See my contacts",
                            message = "Allow Call to read and change your contacts.",
                            onAllow = askContacts,
                            onOpenSettings = { context.startActivity(Permissions.appDetailsSettings(context)) }
                        )
                    }
                    list.isEmpty() && !state.isLoading -> item {
                        EmptyState(
                            if (state.query.isBlank()) "No contacts yet.\nTap + to add one."
                            else "No contact found."
                        )
                    }
                    else -> items(list, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            pinned = state.isPinned(contact),
                            onCall = { call(contact.number) },
                            onLongPress = { actionsFor = contact },
                            onEdit = { onEdit(contact) },
                            onDelete = { toDelete = contact }
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

    toDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${contact.name}?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "This removes the contact from your phone.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(contact)
                    toDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text("Keep", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    pinned: Boolean,
    onCall: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onCall, onLongClick = onLongPress) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleContactPhoto(contact.photoUri, contact.name, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        contact.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (pinned) {
                        Spacer(Modifier.width(8.dp))
                        PinBadge(size = 24.dp)
                    }
                }
                Text(
                    contact.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${contact.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${contact.name}", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            RoundIconButton(
                icon = Icons.Filled.Call,
                description = "Call ${contact.name}",
                onClick = onCall,
                size = 60.dp
            )
        }
    }
}

@Suppress("unused")
private val unusedBox: @Composable () -> Unit = { Box {} }
