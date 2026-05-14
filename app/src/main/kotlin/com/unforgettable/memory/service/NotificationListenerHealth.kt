package com.unforgettable.memory.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.unforgettable.memory.util.PermissionUtils
import java.util.concurrent.TimeUnit

data class NotificationListenerHealthSnapshot(
    val notificationAccessEnabled: Boolean,
    val lastEvent: String?,
    val lastEventAt: Long?,
    val lastCreatedAt: Long?,
    val lastConnectedAt: Long?,
    val lastDisconnectedAt: Long?,
    val lastDestroyedAt: Long?,
    val lastHealthCheckAt: Long?,
    val lastRebindRequestedAt: Long?,
    val lastRebindSkippedAt: Long?,
    val lastRebindFailedAt: Long?,
)

object NotificationListenerHealth {
    private const val TAG = "ListenerHealth"
    private const val PREFS_NAME = "notification_listener_health"
    private const val KEY_LAST_EVENT = "last_event"
    private const val KEY_LAST_EVENT_AT = "last_event_at"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_CONNECTED_AT = "connected_at"
    private const val KEY_DISCONNECTED_AT = "disconnected_at"
    private const val KEY_DESTROYED_AT = "destroyed_at"
    private const val KEY_HEALTH_CHECK_AT = "health_check_at"
    private const val KEY_REBIND_REQUESTED_AT = "rebind_requested_at"
    private const val KEY_REBIND_SKIPPED_AT = "rebind_skipped_at"
    private const val KEY_REBIND_FAILED_AT = "rebind_failed_at"
    private const val PERIODIC_WORK_NAME = "notification_listener_health"
    private const val ONE_TIME_WORK_NAME = "notification_listener_rebind"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val periodicCheck = PeriodicWorkRequestBuilder<NotificationListenerHealthWorker>(
            15,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicCheck,
        )
    }

    fun enqueueRebindCheck(context: Context) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<NotificationListenerHealthWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun markCreated(context: Context) {
        record(context, KEY_CREATED_AT, "listener_created")
    }

    fun markConnected(context: Context) {
        record(context, KEY_CONNECTED_AT, "listener_connected")
    }

    fun markDisconnected(context: Context) {
        record(context, KEY_DISCONNECTED_AT, "listener_disconnected")
    }

    fun markDestroyed(context: Context) {
        record(context, KEY_DESTROYED_AT, "listener_destroyed")
    }

    fun markHealthCheck(context: Context) {
        record(context, KEY_HEALTH_CHECK_AT, "health_check")
    }

    fun requestRebind(context: Context, reason: String): Boolean {
        if (!PermissionUtils.isNotificationListenerEnabled(context)) {
            record(context, KEY_REBIND_SKIPPED_AT, "rebind_skipped:$reason")
            Log.d(TAG, "Skip rebind for $reason because notification listener access is disabled")
            return false
        }

        val component = ComponentName(context, MemoryNotificationListener::class.java)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationListenerService.requestRebind(component)
                record(context, KEY_REBIND_REQUESTED_AT, "rebind_requested:$reason")
                Log.i(TAG, "Requested notification listener rebind for $reason")
                true
            } else {
                false
            }
        }.getOrElse { error ->
            record(context, KEY_REBIND_FAILED_AT, "rebind_failed:$reason")
            Log.w(TAG, "Failed to request notification listener rebind for $reason", error)
            false
        }
    }

    fun snapshot(context: Context): NotificationListenerHealthSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NotificationListenerHealthSnapshot(
            notificationAccessEnabled = PermissionUtils.isNotificationListenerEnabled(context),
            lastEvent = prefs.getString(KEY_LAST_EVENT, null),
            lastEventAt = prefs.getTime(KEY_LAST_EVENT_AT),
            lastCreatedAt = prefs.getTime(KEY_CREATED_AT),
            lastConnectedAt = prefs.getTime(KEY_CONNECTED_AT),
            lastDisconnectedAt = prefs.getTime(KEY_DISCONNECTED_AT),
            lastDestroyedAt = prefs.getTime(KEY_DESTROYED_AT),
            lastHealthCheckAt = prefs.getTime(KEY_HEALTH_CHECK_AT),
            lastRebindRequestedAt = prefs.getTime(KEY_REBIND_REQUESTED_AT),
            lastRebindSkippedAt = prefs.getTime(KEY_REBIND_SKIPPED_AT),
            lastRebindFailedAt = prefs.getTime(KEY_REBIND_FAILED_AT),
        )
    }

    private fun record(context: Context, timestampKey: String, event: String) {
        val now = System.currentTimeMillis()
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(timestampKey, now)
            .putString(KEY_LAST_EVENT, event)
            .putLong(KEY_LAST_EVENT_AT, now)
            .apply()
    }

    private fun android.content.SharedPreferences.getTime(key: String): Long? {
        val value = getLong(key, 0L)
        return value.takeIf { it > 0L }
    }
}
