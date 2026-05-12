package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.task.TaskFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFingerprintTest {
    @Test
    fun normalizesReminderWords() {
        assertEquals(
            TaskFingerprint.from("别忘了发合同"),
            TaskFingerprint.from("发合同"),
        )
    }

    @Test
    fun detectsContainmentDuplicates() {
        assertTrue(TaskFingerprint.isDuplicate("合同今天记得处理", "处理合同"))
    }
}

