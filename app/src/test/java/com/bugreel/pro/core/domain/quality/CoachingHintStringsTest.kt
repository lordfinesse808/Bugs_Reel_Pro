package com.bugreel.pro.core.domain.quality

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoachingHintStringsTest {

    // CLAUDE.md Section 6 / Section 16 stop condition — every coaching hint message must be
    // <=120 chars (CoachingHint.message contract).
    @Test
    fun `every coaching hint string is at most 120 characters`() {
        CoachingHintStrings.ALL.forEach { message ->
            assertTrue(message.length <= 120, "\"$message\" is ${message.length} chars, exceeds 120")
        }
    }

    @Test
    fun `every coaching hint string is non-blank`() {
        CoachingHintStrings.ALL.forEach { message -> assertTrue(message.isNotBlank()) }
    }
}
