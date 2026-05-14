package com.unforgettable.memory.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.domain.notification.NotificationRules
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.notification.NotificationParser
import com.unforgettable.memory.reminder.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MemoryNotificationListener : NotificationListenerService() {
    private val parser = NotificationParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationListenerHealth.markCreated(this)
        Log.d(TAG, "Notification listener created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationListenerHealth.markConnected(this)
        Log.d(TAG, "Notification listener connected")
        runCatching { activeNotifications.orEmpty() }
            .getOrElse { error ->
                Log.w(TAG, "Failed to read active notifications on connect", error)
                emptyArray()
            }
            .forEach { sbn ->
                persistNotification(sbn, source = "active")
            }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationListenerHealth.markDisconnected(this)
        Log.w(TAG, "Notification listener disconnected; requesting rebind")
        NotificationListenerHealth.requestRebind(this, reason = "listener_disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        persistNotification(sbn, source = "posted")
    }

    private fun persistNotification(sbn: StatusBarNotification, source: String) {
        if (sbn.packageName == packageName && sbn.notification.channelId == NotificationChannels.DEBUG_SEED_CHANNEL_ID) {
            Log.d(TAG, "Skipped visible debug seed notification via $source")
            return
        }

        val event = parser.parse(sbn)
        if (event == null) {
            Log.d(TAG, "Skipped blank notification from ${sbn.packageName} via $source")
            return
        }

        scope.launch {
            runCatching {
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
            }.onFailure { error ->
                Log.w(TAG, "Failed to persist notification from ${event.packageName} via $source", error)
            }
        }
    }

    override fun onDestroy() {
        NotificationListenerHealth.markDestroyed(this)
        Log.d(TAG, "Notification listener destroyed")
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MemoryNotification"
    }
}
