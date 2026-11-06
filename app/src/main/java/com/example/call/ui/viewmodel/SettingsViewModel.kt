package com.example.call.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.call.CallApp
import com.example.call.data.model.AppSettings
import com.example.call.data.model.TextScale
import com.example.call.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: CallApp) : ViewModel() {

    private val repo = app.container.settingsRepository

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setTextScale(scale: TextScale) = viewModelScope.launch { repo.setTextScale(scale) }
    fun setShowPhotos(show: Boolean) = viewModelScope.launch { repo.setShowPhotos(show) }
}
