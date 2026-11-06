package com.example.call

import android.app.Application
import android.content.Context
import com.example.call.data.CallLogRepository
import com.example.call.data.ContactsRepository
import com.example.call.data.SettingsRepository
import com.example.call.telecom.CallManager
import com.example.call.telecom.CallNotifications

/** Hand-rolled dependency container; small enough that a DI framework would be overkill. */
class AppContainer(context: Context) {
    val contactsRepository = ContactsRepository(context)
    val callLogRepository = CallLogRepository(context, contactsRepository)
    val settingsRepository = SettingsRepository(context)
}

class CallApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CallNotifications.createChannels(this)
        CallManager.init(this)
    }
}
