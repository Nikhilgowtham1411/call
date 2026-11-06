package com.example.call.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.call.CallApp

fun CreationExtras.callApp(): CallApp = this[APPLICATION_KEY] as CallApp

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { ContactsViewModel(callApp()) }
        initializer { CallLogViewModel(callApp()) }
        initializer { SettingsViewModel(callApp()) }
        initializer { DialerViewModel() }
    }
}
