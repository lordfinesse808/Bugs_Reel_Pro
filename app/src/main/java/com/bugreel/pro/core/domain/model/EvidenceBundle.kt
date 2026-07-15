package com.bugreel.pro.core.domain.model

data class EvidenceBundle(
    val recordingUri: String?,
    val screenshots: List<String>,
    val logText: String?,
    val eventLog: String?
)
