package com.unforgettable.memory

import android.app.Application
import com.unforgettable.memory.reminder.NotificationChannels

class UnforgettableApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.create(this)
    }
}

