package com.unforgettable.memory.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationListenerHealthWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationListenerHealth.markHealthCheck(applicationContext)
        NotificationListenerHealth.requestRebind(applicationContext, reason = "health_check")
        return Result.success()
    }
}
