package com.example.call.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DialerViewModel : ViewModel() {

    private val _number = MutableStateFlow("")
    val number: StateFlow<String> = _number.asStateFlow()

    fun append(char: Char) {
        if (_number.value.length < 20) _number.update { it + char }
    }

    fun backspace() = _number.update { it.dropLast(1) }

    fun clear() {
        _number.value = ""
    }

    fun set(value: String) {
        _number.value = value.filter { it.isDigit() || it == '*' || it == '#' || it == '+' }.take(20)
    }
}
