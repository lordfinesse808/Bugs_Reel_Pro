package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.feature.ai.domain.SafetyValidator
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// CLAUDE.md Section 17 — uses MockWebServer; never hits the real OpenAI API.
class OpenAIServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OpenAIService
    private val promptRepository = mockk<PromptRepository> {
        every { titlePrompt(any()) } returns "title prompt"
        every { summaryPrompt(any()) } returns "summary prompt"
        every { reproStepsPrompt(any()) } returns "repro steps prompt"
    }

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = OpenAIService(
            okHttpClient = OkHttpClient(),
            baseUrl = server.url("/").toString(),
            modelStandard = "gpt-4.1-mini",
            promptRepository = promptRepository,
            safetyValidator = SafetyValidator()
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `generateTitle returns the trimmed completion content on success`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(successBody("  Login button unresponsive  "))
        )

        val result = service.generateTitle(context())

        assertEquals(AIResult.Success("Login button unresponsive"), result)
    }

    @Test
    fun `generateSummary parses the response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(successBody("Tapping login does nothing.")))

        val result = service.generateSummary(context())

        assertEquals(AIResult.Success("Tapping login does nothing."), result)
    }

    @Test
    fun `formatReproSteps strips numbering and blank lines`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(successBody("1. Cold start the app\n2. Tap login\n\n"))
        )

        val result = service.formatReproSteps(context())

        assertEquals(AIResult.Success(listOf("Cold start the app", "Tap login")), result)
    }

    @Test
    fun `HTTP 429 maps to RateLimitExceeded with the Retry-After header`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429).addHeader("Retry-After", "15").setBody("{}")
        )

        val result = service.generateTitle(context())

        assertEquals(AIResult.RateLimitExceeded(15), result)
    }

    @Test
    fun `HTTP error response maps to Error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = service.generateTitle(context())

        assertTrue(result is AIResult.Error)
    }

    @Test
    fun `validateSafety never makes a network call`() = runTest {
        val output = com.bugreel.pro.core.domain.model.AiOutput(
            generatedTitle = "Login button unresponsive",
            generatedSummary = null,
            formattedReproSteps = emptyList(),
            safetyPassed = false,
            blockedReason = null
        )

        service.validateSafety(context(), output)

        assertEquals(0, server.requestCount)
    }

    private fun successBody(content: String) = """
        {"choices":[{"message":{"content":"${content.replace("\"", "\\\"").replace("\n", "\\n")}"}}]}
    """.trimIndent()

    private fun context() = ReportContext(
        draft = BugReportDraft(
            id = "draft-1",
            title = null,
            summary = null,
            reproSteps = listOf("Cold start the app", "Tap login"),
            severity = Severity.HIGH,
            frequency = Frequency.SOMETIMES,
            deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
            evidenceBundle = EvidenceBundle(null, emptyList(), null, "tap login"),
            qualityScore = null,
            aiOutput = null,
            platform = TestPlatform.UTEST,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = null
        ),
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(null, emptyList(), null, "tap login")
    )
}
