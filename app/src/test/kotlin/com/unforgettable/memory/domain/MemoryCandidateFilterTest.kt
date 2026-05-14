package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.memory.MemoryCandidate
import com.unforgettable.memory.domain.memory.MemoryCandidateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCandidateFilterTest {
    @Test
    fun keepsStableHighConfidenceMemory() {
        val sanitized = MemoryCandidateFilter.sanitize(
            MemoryCandidate(
                type = "Project",
                title = "Alpha launch",
                content = "Alpha launch tasks usually need a same-day follow-up.",
                keywords = listOf("alpha", "launch"),
                confidence = 0.86,
                importance = 0.7,
            ),
        )

        assertNotNull(sanitized)
        assertEquals("project", sanitized!!.type)
        assertTrue(sanitized.keywords.contains("alpha"))
    }

    @Test
    fun rejectsLowConfidenceAndSensitiveMemory() {
        assertNull(
            MemoryCandidateFilter.sanitize(
                MemoryCandidate(
                    title = "Maybe useful",
                    content = "Not stable enough",
                    confidence = 0.3,
                ),
            ),
        )

        assertNull(
            MemoryCandidateFilter.sanitize(
                MemoryCandidate(
                    title = "Login code",
                    content = "Verification code is 123456",
                    confidence = 0.95,
                ),
            ),
        )
    }
}
