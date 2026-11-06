package com.example.call.data.model

/** A phone contact read from the Android Contacts Provider. */
data class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val number: String,
    val photoUri: String?
)

enum class CallKind { INCOMING, OUTGOING, MISSED, OTHER }

/** One row of the system call log. */
data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val photoUri: String?,
    val kind: CallKind,
    val dateMillis: Long,
    val durationSeconds: Long
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class TextScale(val factor: Float, val label: String) {
    NORMAL(1.0f, "Normal"),
    LARGE(1.2f, "Large"),
    EXTRA_LARGE(1.4f, "Extra large")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val textScale: TextScale = TextScale.LARGE,
    val showPhotos: Boolean = true
)
