package com.bugreel.pro.feature.capture.service

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// CaptureService runs in the :capture process (CLAUDE.md Section 8); the event log
// accumulated there must cross the process boundary as JSON when a capture stops
// (CaptureRepositoryImpl decodes it back), so every BugReelEvent variant is @Serializable.
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

// CLAUDE.md Section 8 — BugReel's OWN interaction events (taps, swipes, screen transitions),
// timestamped by BugReel itself. NOT the test app's logcat. This is the always-available
// evidence layer; it works on every device with zero permissions.
@Serializable
sealed class BugReelEvent {
    abstract val timestamp: Instant

    @Serializable
    data class AppLaunched(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class ScreenViewed(
        val screenName: String,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class UserTapped(
        val x: Float,
        val y: Float,
        val description: String,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class UserSwiped(
        val direction: SwipeDirection,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class RecordingStarted(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class RecordingStopped(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class ReportDraftOpened(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()

    @Serializable
    data class ReportDraftSaved(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant = Instant.now()
    ) : BugReelEvent()
}

@Serializable
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
