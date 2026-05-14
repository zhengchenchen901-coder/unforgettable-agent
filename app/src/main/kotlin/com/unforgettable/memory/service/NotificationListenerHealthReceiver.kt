package com.unforgettable.memory.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationListenerHealthReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationListenerHealth.schedule(context)
        NotificationListenerHealth.enqueueRebindCheck(context)
    }
}
