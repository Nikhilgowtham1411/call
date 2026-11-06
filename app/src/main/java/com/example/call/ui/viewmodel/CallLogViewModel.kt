package com.example.call.ui.viewmodel

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.call.CallApp
import com.example.call.data.model.CallKind
import com.example.call.data.model.CallLogEntry
import com.example.call.system.Permissions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CallFilter(val label: String) {
    ALL("All"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    MISSED("Missed")
}

data class CallLogUiState(
    val entries: List<CallLogEntry> = emptyList(),
    val filter: CallFilter = CallFilter.ALL,
    val isLoading: Boolean = false
) {
    val visible: List<CallLogEntry>
        get() = when (filter) {
            CallFilter.ALL -> entries
            CallFilter.INCOMING -> entries.filter { it.kind == CallKind.INCOMING }
            CallFilter.OUTGOING -> entries.filter { it.kind == CallKind.OUTGOING }
            CallFilter.MISSED -> entries.filter { it.kind == CallKind.MISSED }
        }
}

class CallLogViewModel(private val app: CallApp) : ViewModel() {

    private val repo = app.container.callLogRepository

    private val _state = MutableStateFlow(CallLogUiState())
    val state: StateFlow<CallLogUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        runCatching {
            app.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(150)
            _state.update { it.copy(isLoading = true) }
            val list = if (Permissions.canReadCallLog(app)) repo.getEntries() else emptyList()
            _state.update { it.copy(entries = list, isLoading = false) }
        }
    }

    fun setFilter(filter: CallFilter) = _state.update { it.copy(filter = filter) }

    override fun onCleared() {
        runCatching { app.contentResolver.unregisterContentObserver(observer) }
        super.onCleared()
    }
}
