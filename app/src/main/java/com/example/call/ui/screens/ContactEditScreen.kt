package com.example.call.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.call.data.model.Contact
import com.example.call.system.Permissions
import com.example.call.ui.components.FloralBackground
import com.example.call.ui.components.FloralHeader
import com.example.call.ui.components.PillButton
import com.example.call.ui.components.RoundIconButton
import com.example.call.ui.rememberPermissionRequester
import com.example.call.ui.toast
import com.example.call.ui.viewmodel.ContactsViewModel

/** Add a new contact (existing == null) or edit one. Writes straight into the phone's contacts. */
@Composable
fun ContactEditScreen(
    viewModel: ContactsViewModel,
    existing: Contact?,
    initialNumber: String,
    onClose: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var number by remember { mutableStateOf(existing?.number ?: initialNumber) }
    var newPhoto by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) newPhoto = uri
    }

    fun doSave() {
        nameError = name.isBlank()
        numberError = number.isBlank()
        if (nameError || numberError) return
        saving = true
        viewModel.save(existing, name, number, newPhoto) { ok ->
            saving = false
            if (ok) onClose()
        }
    }

    val askWrite = rememberPermissionRequester(Permissions.contacts + Permissions.contactsWrite) { granted ->
        if (granted) doSave() else toast(context, "Please allow Contacts access to save")
    }

    FloralBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            FloralHeader(
                title = if (existing == null) "Add Contact" else "Edit Contact",
                leading = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Back",
                        onClick = onClose,
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // Photo picker circle. Shows the chosen photo, else the current contact photo.
                Box(
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val shown: Any? = newPhoto ?: existing?.photoUri
                    if (shown != null) {
                        AsyncImage(
                            model = shown,
                            contentDescription = "Contact photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                "Add Photo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (newPhoto != null || existing?.photoUri != null) {
                    Text(
                        "Tap the photo to change it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name", style = MaterialTheme.typography.bodyMedium) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Please type a name") }
                    } else null,
                    shape = RoundedCornerShape(50)
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() || c in "+*#- ()" }; numberError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone Number", style = MaterialTheme.typography.bodyMedium) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    singleLine = true,
                    isError = numberError,
                    supportingText = if (numberError) {
                        { Text("Please type a phone number") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(50)
                )

                PillButton(
                    text = if (saving) "Saving…" else "Save Contact",
                    icon = Icons.Filled.Save,
                    onClick = {
                        if (saving) return@PillButton
                        if (Permissions.canWriteContacts(context)) doSave() else askWrite()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 64.dp
                )
            }
        }
    }
}
