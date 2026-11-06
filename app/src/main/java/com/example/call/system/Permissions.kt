package com.example.call.system

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Central place for runtime permission lists and checks. */
object Permissions {

    val contacts = arrayOf(Manifest.permission.READ_CONTACTS)

    val contactsWrite = arrayOf(Manifest.permission.WRITE_CONTACTS)

    val callLog = arrayOf(Manifest.permission.READ_CALL_LOG)

    val phone = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    /** Everything the app asks for in one go on first launch. */
    fun allRuntime(): Array<String> {
        val list = ArrayList<String>()
        list += Manifest.permission.READ_CONTACTS
        list += Manifest.permission.WRITE_CONTACTS
        list += Manifest.permission.READ_CALL_LOG
        list += Manifest.permission.CALL_PHONE
        list += Manifest.permission.READ_PHONE_STATE
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        return list.toTypedArray()
    }

    fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasAll(context: Context, permissions: Array<String>): Boolean =
        permissions.all { has(context, it) }

    fun canReadContacts(context: Context) = has(context, Manifest.permission.READ_CONTACTS)
    fun canWriteContacts(context: Context) = has(context, Manifest.permission.WRITE_CONTACTS)
    fun canReadCallLog(context: Context) = has(context, Manifest.permission.READ_CALL_LOG)
    fun canCall(context: Context) = has(context, Manifest.permission.CALL_PHONE)

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || has(context, Manifest.permission.POST_NOTIFICATIONS)

    /** Android 14+ lets the user revoke full-screen intents; older versions always allow them. */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.canUseFullScreenIntent()
    }

    fun fullScreenIntentSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.parse("package:${context.packageName}"))
        } else appDetailsSettings(context)

    fun appDetailsSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
}
