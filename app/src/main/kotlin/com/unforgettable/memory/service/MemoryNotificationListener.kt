package com.unforgettable.memory.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications.orEmpty().forEach { sbn ->
            persistNotification(sbn, source = "active")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        persistNotification(sbn, source = "posted")
    }

    private fun persistNotification(sbn: StatusBarNotification, source: String) {
        val event = parser.parse(sbn)
        if (event == null) {
            Log.d(TAG, "Skipped blank notification from ${sbn.packageName} via $source")
            return
        }

        scope.launch {
            val container = (application as UnforgettableApp).container
            val rawNotificationDao = container.database.rawNotificationDao()
            val existingId = rawNotificationDao.findExistingId(
                packageName = event.packageName,
                title = event.title,
                content = event.content,
                timestamp = event.timestamp,
            )
            if (existingId != null) {
                Log.d(TAG, "Skipped duplicate notification $existingId from ${event.packageName} via $source")
                return@launch
            }

            val rawId = rawNotificationDao.insert(
                RawNotificationEntity(
                    packageName = event.packageName,
                    title = event.title,
                    content = event.content,
                    timestamp = event.timestamp,
                ),
            )
            Log.d(TAG, "Stored notification $rawId from ${event.packageName} via $source")

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

    companion object {
        private const val TAG = "MemoryNotification"
    }
}
