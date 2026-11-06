package com.example.call.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

/**
 * The app can only answer / decline calls and show its own incoming-call screen while it is
 * the device's default phone app (the DIALER role). This helper checks and requests that role.
 */
object DialerRole {

    fun isDefaultDialer(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = context.getSystemService(RoleManager::class.java)
            rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                rm.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            val tm = context.getSystemService(TelecomManager::class.java)
            tm?.defaultDialerPackage == context.packageName
        }

    /** Intent to launch with an activity-result launcher, or null if the device offers no way. */
    fun requestIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else null
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
}
