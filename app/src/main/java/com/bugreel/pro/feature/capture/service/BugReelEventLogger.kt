package com.bugreel.pro.feature.capture.service

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Section 8 — BugReel's OWN interaction events (taps, swipes, screen transitions),
// timestamped by BugReel itself. NOT the test app's logcat. This is the always-available
// evidence layer; it works on every device with zero permissions.
sealed class BugReelEvent {
    abstract val timestamp: Instant

    data class AppLaunched(override val timestamp: Instant = Instant.now()) : BugReelEvent()

    data class ScreenViewed(
        val screenName: String,
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    data class UserTapped(
        val x: Float,
        val y: Float,
        val description: String,
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    data class UserSwiped(
        val direction: SwipeDirection,
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    data class RecordingStarted(override val timestamp: Instant = Instant.now()) : BugReelEvent()

    data class RecordingStopped(override val timestamp: Instant = Instant.now()) : BugReelEvent()

    data class ReportDraftOpened(override val timestamp: Instant = Instant.now()) : BugReelEvent()

    data class ReportDraftSaved(override val timestamp: Instant = Instant.now()) : BugReelEvent()
}

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

@Singleton
class BugReelEventLogger @Inject constructor() {

    private val events = CopyOnWriteArrayList<BugReelEvent>()

    fun logEvent(event: BugReelEvent) {
        events.add(event)
    }

    fun getEvents(): List<BugReelEvent> = events.toList()

    fun clearEvents() {
        events.clear()
    }
}
