package com.unforgettable.memory.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.llm.TaskExtractionResult
import com.unforgettable.memory.domain.notification.NotificationEvent
import com.unforgettable.memory.domain.notification.NotificationRules
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.domain.task.DeadlineParser
import com.unforgettable.memory.domain.task.TaskFingerprint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class ExtractionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun doWork(): Result {
        val rawNotificationId = inputData.getLong(KEY_RAW_NOTIFICATION_ID, -1L)
        if (rawNotificationId <= 0L) return Result.failure()

        val container = (applicationContext as UnforgettableApp).container
        val database = container.database
        val raw = database.rawNotificationDao().getById(rawNotificationId) ?: return Result.success()
        val event = raw.toEvent()
        val requestText = event.combinedText

        return runCatching {
            val finalDecision = process(raw, event, requestText)
            database.aiExtractionLogDao().insert(finalDecision)
            Result.success()
        }.getOrElse { error ->
            database.aiExtractionLogDao().insert(
                AiExtractionLogEntity(
                    rawNotificationId = rawNotificationId,
                    requestText = requestText,
                    responseJson = null,
                    finalDecision = "worker_error",
                    error = error.message ?: error::class.java.simpleName,
                ),
            )
            Result.retry()
        }
    }

    private suspend fun process(
        raw: RawNotificationEntity,
        event: NotificationEvent,
        requestText: String,
    ): AiExtractionLogEntity {
        val container = (applicationContext as UnforgettableApp).container
        val database = container.database

        if (!SupportedApps.shouldRunAi(raw.packageName)) {
            return log(raw, requestText, null, "ignored_unsupported_app", null)
        }

        if (NotificationRules.shouldIgnore(raw.title, raw.content)) {
            return log(raw, requestText, null, "ignored_noise_rule", null)
        }

        val initialFingerprint = TaskFingerprint.from(raw.content)
        database.taskDao().findPendingByFingerprint(initialFingerprint)?.let { duplicate ->
            return log(raw, requestText, null, "duplicate_by_fingerprint:${duplicate.id}", null)
        }

        val activeTasks = database.taskDao().getActiveTasksSnapshot()
        val extraction = container.taskExtractor.extract(event, activeTasks)
        val responseJson = extraction.rawJson ?: json.encodeToString(extraction)
        val decision = decideAndMaybeCreateTask(raw, extraction)

        return log(raw, requestText, responseJson, decision, extraction.error)
    }

    private suspend fun decideAndMaybeCreateTask(
        raw: RawNotificationEntity,
        extraction: TaskExtractionResult,
    ): String {
        if (!extraction.isTask) return "rejected_not_task"
        extraction.duplicateOfTaskId?.let { return "duplicate_by_llm:$it" }
        if (extraction.confidence <= CONFIDENCE_THRESHOLD) return "rejected_below_threshold:${extraction.confidence}"
        val content = extraction.task?.trim().orEmpty()
        if (content.isBlank()) return "rejected_missing_task"

        val container = (applicationContext as UnforgettableApp).container
        val fingerprint = TaskFingerprint.from(content)
        container.database.taskDao().findPendingByFingerprint(fingerprint)?.let { duplicate ->
            return "duplicate_after_extraction:${duplicate.id}"
        }

        val deadline = DeadlineParser.resolve(
            deadlineLocal = extraction.deadlineLocal,
            deadlineText = extraction.deadlineText ?: raw.content,
            urgency = extraction.urgency,
        )
        val taskId = container.taskRepository.createTask(
            TaskEntity(
                content = content,
                deadlineAt = deadline.deadlineAt,
                reminderAt = deadline.reminderAt,
                sourcePackage = raw.packageName,
                sourceRawNotificationId = raw.id,
                confidence = extraction.confidence,
                urgency = extraction.urgency,
                fingerprint = fingerprint,
            ),
        )
        return "created_task:$taskId"
    }

    private fun log(
        raw: RawNotificationEntity,
        requestText: String,
        responseJson: String?,
        finalDecision: String,
        error: String?,
    ): AiExtractionLogEntity {
        return AiExtractionLogEntity(
            rawNotificationId = raw.id,
            requestText = requestText,
            responseJson = responseJson,
            finalDecision = finalDecision,
            error = error,
        )
    }

    private fun RawNotificationEntity.toEvent(): NotificationEvent {
        return NotificationEvent(
            packageName = packageName,
            title = title,
            content = content,
            timestamp = timestamp,
        )
    }

    companion object {
        const val KEY_RAW_NOTIFICATION_ID = "raw_notification_id"
        private const val CONFIDENCE_THRESHOLD = 0.8
        private const val EXTRACTION_QUEUE_NAME = "notification_extraction"

        fun enqueue(context: Context, rawNotificationId: Long) {
            val work = OneTimeWorkRequestBuilder<ExtractionWorker>()
                .setInputData(workDataOf(KEY_RAW_NOTIFICATION_ID to rawNotificationId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(EXTRACTION_QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }
    }
}
