package com.unforgettable.memory.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.domain.notification.NotificationRules
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.notification.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MemoryNotificationListener : NotificationListenerService() {
    private val parser = NotificationParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val event = parser.parse(sbn) ?: return
        scope.launch {
            val container = (application as UnforgettableApp).container
            val rawId = container.database.rawNotificationDao().insert(
                RawNotificationEntity(
                    packageName = event.packageName,
                    title = event.title,
                    content = event.content,
                    timestamp = event.timestamp,
                ),
            )

            val shouldExtract = SupportedApps.shouldRunAi(event.packageName) &&
                !NotificationRules.shouldIgnore(event.title, event.content)
            if (shouldExtract) {
                ExtractionWorker.enqueue(this@MemoryNotificationListener, rawId)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

