package com.bugreel.pro.core.domain.model

// Sprint 4 bookkeeping fields (preAiQualityScore..aiSuggestionsRejected) default to their
// no-AI-assist values on purpose: they exist only to shuttle data from AiAssistViewModel,
// through BuildReportUseCase.buildOrNull()'s in-memory BugReport, to
// BuildReportUseCase.recordOutcomeFeedback() at submission time -- never persisted to
// AiOutputEntity (no Room migration needed), since OutcomeFeedbackEntity is their durable home.
data class AiOutput(
    val generatedTitle: String?,
    val generatedSummary: String?,
    val formattedReproSteps: List<String>,
    val safetyPassed: Boolean,
    val blockedReason: String?,
    val preAiQualityScore: Int = 0,
    val postAiQualityScore: Int = 0,
    val testerOverrodeSuggestion: Boolean = false,
    val aiSuggestionsAccepted: List<String> = emptyList(),
    val aiSuggestionsRejected: List<String> = emptyList()
)
