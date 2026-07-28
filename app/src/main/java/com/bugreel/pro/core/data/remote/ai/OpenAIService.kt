package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.data.remote.ai.dto.ChatMessage
import com.bugreel.pro.core.data.remote.ai.dto.ChatRequest
import com.bugreel.pro.core.data.remote.ai.dto.ChatResponse
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.feature.ai.domain.SafetyValidator
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// CLAUDE.md Section 9 / Anti-Pattern 8 — the ONLY class making HTTP calls to the AI backend.
// Model routing goes through the injected @Named constants (sourced from BuildConfig in
// NetworkModule) only, never a hardcoded "gpt-*" string.
//
// validateSafety() delegates entirely to the local, deterministic SafetyValidator rather than a
// second paid round-trip through BuildConfig.AI_MODEL_SAFETY: ADR-004's four criteria (Section
// 9) are objective rules checkable against the evidence bundle directly, not a judgment call
// that benefits from asking the model to self-certify. @Named("aiModelSafety") stays available
// (provided by NetworkModule) for a future vision-based check without re-plumbing DI.
class OpenAIService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("aiBaseUrl") private val baseUrl: String,
    @Named("aiModelStandard") private val modelStandard: String,
    private val promptRepository: PromptRepository,
    private val safetyValidator: SafetyValidator
) : AIService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateTitle(context: ReportContext): AIResult<String> =
        complete(modelStandard, promptRepository.titlePrompt(context)).mapSuccess { it.trim() }

    override suspend fun generateSummary(context: ReportContext): AIResult<String> =
        complete(modelStandard, promptRepository.summaryPrompt(context)).mapSuccess { it.trim() }

    override suspend fun formatReproSteps(context: ReportContext): AIResult<List<String>> =
        complete(modelStandard, promptRepository.reproStepsPrompt(context)).mapSuccess { text ->
            text.lines()
                .map { STEP_NUMBER_PREFIX.replace(it.trim(), "").trim() }
                .filter { it.isNotBlank() }
        }

    override suspend fun validateSafety(context: ReportContext, output: AiOutput): SafetyResult =
        safetyValidator.validate(context, output)

    private suspend fun complete(model: String, prompt: String): AIResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val requestJson = json.encodeToString(
                    ChatRequest(model = model, messages = listOf(ChatMessage(role = "user", content = prompt)))
                )
                val request = Request.Builder()
                    .url("${baseUrl}chat/completions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                    .apply {
                        if (BuildConfig.OPENAI_ORG_ID.isNotBlank()) {
                            addHeader("OpenAI-Organization", BuildConfig.OPENAI_ORG_ID)
                        }
                    }
                    .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.code == RATE_LIMIT_STATUS_CODE -> {
                            val retryAfter = response.header("Retry-After")?.toIntOrNull()
                                ?: DEFAULT_RETRY_AFTER_SECONDS
                            AIResult.RateLimitExceeded(retryAfter)
                        }
                        !response.isSuccessful -> AIResult.Error("OpenAI request failed: HTTP ${response.code}")
                        else -> {
                            val bodyString = response.body?.string()
                            val content = bodyString
                                ?.let { json.decodeFromString<ChatResponse>(it) }
                                ?.choices?.firstOrNull()?.message?.content
                            if (content != null) {
                                AIResult.Success(content)
                            } else {
                                AIResult.Error("OpenAI response had no completion content")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                AIResult.Error(e.message ?: "Network error calling OpenAI")
            }
        }

    private fun <T> AIResult<String>.mapSuccess(transform: (String) -> T): AIResult<T> = when (this) {
        is AIResult.Success -> AIResult.Success(transform(value))
        is AIResult.Error -> this
        is AIResult.RateLimitExceeded -> this
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val STEP_NUMBER_PREFIX = Regex("^([\\d]+[.).]|-)\\s*")
        const val RATE_LIMIT_STATUS_CODE = 429
        const val DEFAULT_RETRY_AFTER_SECONDS = 30
    }
}
