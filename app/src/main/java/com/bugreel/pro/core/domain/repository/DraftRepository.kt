package com.bugreel.pro.core.domain.repository

import com.bugreel.pro.core.domain.model.BugReportDraft
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    fun getDraftById(id: String): Flow<BugReportDraft?>
    suspend fun insertDraft(draft: BugReportDraft)
    suspend fun updateDraft(draft: BugReportDraft)
    suspend fun deleteDraftById(id: String)
}
