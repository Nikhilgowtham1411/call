package com.example.call.ui.viewmodel

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.call.CallApp
import com.example.call.data.model.Contact
import com.example.call.system.Permissions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val query: String = "",
    val isLoading: Boolean = false
) {
    /** Pinned contacts first (keeping name order), then everyone else. */
    val ordered: List<Contact>
        get() = if (pinned.isEmpty()) contacts
        else contacts.filter { it.lookupKey in pinned } + contacts.filter { it.lookupKey !in pinned }

    fun isPinned(contact: Contact) = contact.lookupKey in pinned

    val filtered: List<Contact>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return ordered
            val digits = q.filter { it.isDigit() }
            return ordered.filter { c ->
                c.name.contains(q, ignoreCase = true) ||
                    (digits.isNotEmpty() && c.number.filter { it.isDigit() }.contains(digits))
            }
        }
}

class ContactsViewModel(private val app: CallApp) : ViewModel() {

    private val repo = app.container.contactsRepository

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var refreshJob: Job? = null

    // Keeps the app in sync when contacts change anywhere on the phone.
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        runCatching {
            app.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, observer
            )
        }
        viewModelScope.launch {
            app.container.settingsRepository.pinned.collect { keys ->
                _state.update { it.copy(pinned = keys) }
            }
        }
        refresh()
    }

    fun togglePin(contact: Contact) {
        viewModelScope.launch {
            val wasPinned = contact.lookupKey in _state.value.pinned
            app.container.settingsRepository.togglePin(contact.lookupKey)
            _messages.emit(if (wasPinned) "${contact.name} unpinned" else "${contact.name} pinned to top")
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(150) // collapse bursts of change notifications
            _state.update { it.copy(isLoading = true) }
            val list = if (Permissions.canReadContacts(app)) repo.getContacts() else emptyList()
            _state.update { it.copy(contacts = list, isLoading = false) }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun contactById(id: Long): Contact? = _state.value.contacts.firstOrNull { it.id == id }

    /** Creates a new contact, or updates [existing]. [onDone] receives true on success. */
    fun save(existing: Contact?, name: String, number: String, photo: Uri?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!Permissions.canWriteContacts(app)) {
                _messages.emit("Please allow Contacts access to save")
                onDone(false)
                return@launch
            }
            val result = if (existing == null) {
                repo.addContact(name, number, photo)
            } else {
                repo.updateContact(existing.id, name, number, photo)
            }
            if (result.isSuccess) {
                _messages.emit(if (existing == null) "Contact saved" else "Contact updated")
                refresh()
            } else {
                _messages.emit("Could not save the contact")
            }
            onDone(result.isSuccess)
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch {
            if (!Permissions.canWriteContacts(app)) {
                _messages.emit("Please allow Contacts access to delete")
                return@launch
            }
            val result = repo.deleteContact(contact.lookupKey)
            if (result.isSuccess) {
                _messages.emit("${contact.name} deleted")
                refresh()
            } else {
                _messages.emit("Could not delete the contact")
            }
        }
    }

    override fun onCleared() {
        runCatching { app.contentResolver.unregisterContentObserver(observer) }
        super.onCleared()
    }
}
