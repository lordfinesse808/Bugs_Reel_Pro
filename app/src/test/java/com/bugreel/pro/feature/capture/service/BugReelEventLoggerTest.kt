package com.bugreel.pro.feature.capture.service

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BugReelEventLoggerTest {

    @Test
    fun `all event subtypes can be logged without error`() {
        val logger = BugReelEventLogger()

        logger.logEvent(BugReelEvent.AppLaunched())
        logger.logEvent(BugReelEvent.ScreenViewed("capture"))
        logger.logEvent(BugReelEvent.UserTapped(10f, 20f, "start button"))
        logger.logEvent(BugReelEvent.UserSwiped(SwipeDirection.LEFT))
        logger.logEvent(BugReelEvent.RecordingStarted())
        logger.logEvent(BugReelEvent.RecordingStopped())
        logger.logEvent(BugReelEvent.ReportDraftOpened())
        logger.logEvent(BugReelEvent.ReportDraftSaved())

        assertEquals(8, logger.getEvents().size)
    }

    @Test
    fun `getEvents returns events in chronological order`() {
        val logger = BugReelEventLogger()
        val first = BugReelEvent.AppLaunched(timestamp = Instant.parse("2026-01-01T00:00:00Z"))
        val second = BugReelEvent.RecordingStarted(timestamp = Instant.parse("2026-01-01T00:00:01Z"))
        val third = BugReelEvent.RecordingStopped(timestamp = Instant.parse("2026-01-01T00:00:02Z"))

        logger.logEvent(first)
        logger.logEvent(second)
        logger.logEvent(third)

        assertEquals(listOf(first, second, third), logger.getEvents())
    }

    @Test
    fun `clearEvents results in empty list`() {
        val logger = BugReelEventLogger()
        logger.logEvent(BugReelEvent.AppLaunched())

        logger.clearEvents()

        assertTrue(logger.getEvents().isEmpty())
    }

    @Test
    fun `concurrent logging does not lose events`() {
        val logger = BugReelEventLogger()
        val threadCount = 20
        val eventsPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                try {
                    repeat(eventsPerThread) {
                        logger.logEvent(BugReelEvent.UserTapped(0f, 0f, "concurrent"))
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(threadCount * eventsPerThread, logger.getEvents().size)
    }
}
