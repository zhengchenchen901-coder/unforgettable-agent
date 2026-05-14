package com.unforgettable.memory

import android.app.Application
import com.unforgettable.memory.reminder.NotificationChannels
import com.unforgettable.memory.service.NotificationListenerHealth

class UnforgettableApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.create(this)
        NotificationListenerHealth.schedule(this)
        NotificationListenerHealth.enqueueRebindCheck(this)
    }
}
