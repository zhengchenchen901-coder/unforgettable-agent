package com.unforgettable.memory.domain.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskExtractionResult(
    @SerialName("is_task")
    val isTask: Boolean,
    val task: String?,
    @SerialName("deadline_local")
    val deadlineLocal: String?,
    @SerialName("deadline_text")
    val deadlineText: String?,
    val urgency: String,
    val confidence: Double,
    @SerialName("duplicate_of_task_id")
    val duplicateOfTaskId: Long?,
    val reason: String,
    val rawJson: String? = null,
    val error: String? = null,
)

