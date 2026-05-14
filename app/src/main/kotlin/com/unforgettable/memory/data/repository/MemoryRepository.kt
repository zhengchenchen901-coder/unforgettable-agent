package com.unforgettable.memory.data.repository

import com.unforgettable.memory.data.storage.dao.MemoryAccessLogDao
import com.unforgettable.memory.data.storage.dao.MemoryEvidenceDao
import com.unforgettable.memory.data.storage.dao.MemoryItemDao
import com.unforgettable.memory.data.storage.entity.MemoryAccessLogEntity
import com.unforgettable.memory.data.storage.entity.MemoryEvidenceEntity
import com.unforgettable.memory.data.storage.entity.MemoryItemEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.memory.MemoryCandidate
import com.unforgettable.memory.domain.memory.MemoryCandidateFilter
import com.unforgettable.memory.domain.memory.MemoryContextItem
import com.unforgettable.memory.domain.memory.MemoryFingerprint
import com.unforgettable.memory.domain.memory.MemoryRanker
import com.unforgettable.memory.domain.memory.MemorySource
import com.unforgettable.memory.domain.memory.MemoryStatus
import com.unforgettable.memory.domain.notification.NotificationEvent

class MemoryRepository(
    private val memoryItemDao: MemoryItemDao,
    private val memoryEvidenceDao: MemoryEvidenceDao,
    private val memoryAccessLogDao: MemoryAccessLogDao,
) {
    suspend fun retrieveFor(
        event: NotificationEvent,
        limit: Int = 8,
        now: Long = System.currentTimeMillis(),
    ): List<MemoryContextItem> {
        return memoryItemDao.getActiveSnapshot()
            .map { item ->
                val contextItem = item.toContextItem()
                contextItem.copy(score = MemoryRanker.score(contextItem, event, now))
            }
            .filter { it.score > 0.18 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    suspend fun recordAccess(
        rawNotificationId: Long,
        requestText: String,
        memories: List<MemoryContextItem>,
    ) {
        memoryAccessLogDao.insert(
            MemoryAccessLogEntity(
                rawNotificationId = rawNotificationId,
                requestText = requestText,
                retrievedMemoryIds = memories.map { it.id },
            ),
        )
    }

    suspend fun upsertFromCandidates(
        raw: RawNotificationEntity,
        task: TaskEntity,
        candidates: List<MemoryCandidate>,
    ): Int {
        var stored = 0
        candidates.mapNotNull { MemoryCandidateFilter.sanitize(it) }.forEach { candidate ->
            val fingerprint = MemoryFingerprint.from(candidate.type, candidate.title, candidate.content)
            val existing = memoryItemDao.findByFingerprint(fingerprint)
            if (existing != null && existing.status in listOf(MemoryStatus.ARCHIVED, MemoryStatus.IGNORED)) {
                return@forEach
            }

            val now = System.currentTimeMillis()
            val memoryItemId = if (existing == null) {
                memoryItemDao.insert(
                    MemoryItemEntity(
                        type = candidate.type,
                        title = candidate.title,
                        content = candidate.content,
                        keywords = candidate.keywords,
                        sourcePackage = raw.packageName,
                        source = MemorySource.NOTIFICATION_INFERRED,
                        confidence = candidate.confidence,
                        importance = candidate.importance,
                        status = MemoryStatus.PENDING,
                        fingerprint = fingerprint,
                        evidenceCount = 1,
                        lastSeenAt = now,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                memoryItemDao.update(
                    existing.copy(
                        title = if (candidate.confidence >= existing.confidence) candidate.title else existing.title,
                        content = if (candidate.confidence >= existing.confidence) candidate.content else existing.content,
                        keywords = (existing.keywords + candidate.keywords).distinct().take(12),
                        confidence = maxOf(existing.confidence, candidate.confidence),
                        importance = maxOf(existing.importance, candidate.importance),
                        evidenceCount = existing.evidenceCount + 1,
                        lastSeenAt = now,
                        updatedAt = now,
                    ),
                )
                existing.id
            }

            memoryEvidenceDao.insert(
                MemoryEvidenceEntity(
                    memoryItemId = memoryItemId,
                    rawNotificationId = raw.id,
                    taskId = task.id,
                    sourceText = raw.content.take(500),
                    reason = candidate.reason,
                ),
            )
            stored += 1
        }
        return stored
    }

    suspend fun archiveMemory(id: Long) {
        memoryItemDao.updateStatus(id, MemoryStatus.ARCHIVED)
    }

    suspend fun confirmMemory(id: Long) {
        val existing = memoryItemDao.getById(id) ?: return
        memoryItemDao.update(
            existing.copy(
                source = MemorySource.USER_CONFIRMED,
                status = MemoryStatus.ACTIVE,
                confidence = maxOf(existing.confidence, 0.95),
                importance = maxOf(existing.importance, 0.7),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun ignoreMemory(id: Long) {
        memoryItemDao.updateStatus(id, MemoryStatus.IGNORED)
    }

    suspend fun createUserMemory(
        type: String,
        title: String,
        content: String,
        keywords: List<String>,
    ): Long? {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        if (cleanTitle.isBlank() || cleanContent.isBlank()) return null

        val cleanType = type.trim().lowercase().ifBlank { "preference" }
        val mergedKeywords = (keywords + com.unforgettable.memory.domain.memory.MemoryKeywordExtractor.fromText("$cleanTitle $cleanContent"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .take(12)
        val fingerprint = MemoryFingerprint.from(cleanType, cleanTitle, cleanContent)
        val existing = memoryItemDao.findByFingerprint(fingerprint)
        val now = System.currentTimeMillis()
        if (existing != null) {
            memoryItemDao.update(
                existing.copy(
                    type = cleanType,
                    title = cleanTitle.take(120),
                    content = cleanContent.take(500),
                    keywords = mergedKeywords,
                    source = MemorySource.USER_INPUT,
                    status = MemoryStatus.ACTIVE,
                    confidence = 1.0,
                    importance = maxOf(existing.importance, 0.85),
                    lastSeenAt = now,
                    updatedAt = now,
                ),
            )
            return existing.id
        }

        return memoryItemDao.insert(
            MemoryItemEntity(
                type = cleanType,
                title = cleanTitle.take(120),
                content = cleanContent.take(500),
                keywords = mergedKeywords,
                sourcePackage = null,
                source = MemorySource.USER_INPUT,
                confidence = 1.0,
                importance = 0.85,
                status = MemoryStatus.ACTIVE,
                fingerprint = fingerprint,
                evidenceCount = 0,
                lastSeenAt = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteMemory(id: Long) {
        memoryEvidenceDao.deleteForMemory(id)
        memoryItemDao.deleteById(id)
    }

    private fun MemoryItemEntity.toContextItem(): MemoryContextItem {
        return MemoryContextItem(
            id = id,
            type = type,
            title = title,
            content = content,
            keywords = keywords,
            sourcePackage = sourcePackage,
            source = source,
            confidence = confidence,
            importance = importance,
            evidenceCount = evidenceCount,
            lastSeenAt = lastSeenAt,
        )
    }
}
