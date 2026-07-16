package com.bugreel.pro.feature.report.data

import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.dao.AiOutputDao
import com.bugreel.pro.core.data.local.dao.DeviceProfileDao
import com.bugreel.pro.core.data.local.dao.EvidenceDao
import com.bugreel.pro.core.data.local.dao.QualityScoreDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import com.bugreel.pro.core.data.local.entity.AiOutputEntity
import com.bugreel.pro.core.data.local.entity.DeviceProfileEntity
import com.bugreel.pro.core.data.local.entity.EvidenceEntity
import com.bugreel.pro.core.data.local.entity.QualityScoreEntity
import com.bugreel.pro.core.data.local.entity.ReportEntity
import com.bugreel.pro.core.data.sync.SyncWorker
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.CoachingHint
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.repository.ReportRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CLAUDE.md Section 7 — Room first (in a single transaction across the report's side
// tables), then trigger SyncWorker. Nothing here calls FirestoreSource/StorageSource
// directly (Architecture Rule 2) — that only ever happens inside SyncWorker.
class ReportRepositoryImpl @Inject constructor(
    private val database: BugReelDatabase,
    private val reportDao: ReportDao,
    private val evidenceDao: EvidenceDao,
    private val deviceProfileDao: DeviceProfileDao,
    private val qualityScoreDao: QualityScoreDao,
    private val aiOutputDao: AiOutputDao,
    private val workManager: WorkManager
) : ReportRepository {

    override fun getAllReports(): Flow<List<BugReport>> =
        reportDao.getAllReports().map { entities -> entities.map { toDomain(it) } }

    override fun getReportById(id: String): Flow<BugReport?> =
        reportDao.observeById(id).map { entity -> entity?.let { toDomain(it) } }

    override suspend fun insertReport(report: BugReport) {
        persist(report)
        scheduleSync()
    }

    override suspend fun updateReport(report: BugReport) {
        persist(report)
        scheduleSync()
    }

    override suspend fun deleteReport(id: String) {
        reportDao.getById(id)?.let { reportDao.delete(it) }
    }

    private suspend fun persist(report: BugReport) = database.withTransaction {
        reportDao.insert(
            ReportEntity(
                id = report.id,
                title = report.title,
                summary = report.summary,
                reproSteps = report.reproSteps,
                severity = report.severity,
                frequency = report.frequency,
                platform = report.platform,
                status = report.status,
                createdAt = report.createdAt,
                updatedAt = report.updatedAt,
                syncedAt = report.syncedAt
            )
        )
        evidenceDao.insert(
            EvidenceEntity(
                reportId = report.id,
                recordingUri = report.evidenceBundle.recordingUri,
                screenshots = report.evidenceBundle.screenshots,
                logText = report.evidenceBundle.logText,
                eventLog = report.evidenceBundle.eventLog
            )
        )
        deviceProfileDao.insert(
            DeviceProfileEntity(
                reportId = report.id,
                brand = report.deviceProfile.brand,
                model = report.deviceProfile.model,
                osVersion = report.deviceProfile.osVersion,
                apiLevel = report.deviceProfile.apiLevel,
                ramMb = report.deviceProfile.ramMb,
                screenResolution = report.deviceProfile.screenResolution,
                networkType = report.deviceProfile.networkType
            )
        )
        report.qualityScore?.let { score ->
            qualityScoreDao.insert(
                QualityScoreEntity(
                    reportId = report.id,
                    total = score.total,
                    reproduction = score.reproduction,
                    evidence = score.evidence,
                    description = score.description,
                    severity = score.severity,
                    metadata = score.metadata,
                    coaching = Json.encodeToString(score.coaching)
                )
            )
        }
        report.aiOutput?.let { output ->
            aiOutputDao.insert(
                AiOutputEntity(
                    reportId = report.id,
                    generatedTitle = output.generatedTitle,
                    generatedSummary = output.generatedSummary,
                    formattedReproSteps = output.formattedReproSteps,
                    safetyPassed = output.safetyPassed,
                    blockedReason = output.blockedReason
                )
            )
        }
    }

    // Sprint 2 has no UI path yet that writes qualityScore/aiOutput before this session, so
    // their side-table rows are simply absent until Sprint 3/4 populate them — that's the
    // nullable contract BugReport itself already documents (Section 6).
    private suspend fun toDomain(entity: ReportEntity): BugReport {
        val evidence = evidenceDao.getByReportId(entity.id)
        val device = deviceProfileDao.getByReportId(entity.id)
        val quality = qualityScoreDao.getByReportId(entity.id)
        val aiOutput = aiOutputDao.getByReportId(entity.id)
        return BugReport(
            id = entity.id,
            title = entity.title,
            summary = entity.summary,
            reproSteps = entity.reproSteps,
            severity = entity.severity,
            frequency = entity.frequency,
            deviceProfile = device?.let {
                DeviceProfile(
                    brand = it.brand,
                    model = it.model,
                    osVersion = it.osVersion,
                    apiLevel = it.apiLevel,
                    ramMb = it.ramMb,
                    screenResolution = it.screenResolution,
                    networkType = it.networkType
                )
            } ?: EMPTY_DEVICE_PROFILE,
            evidenceBundle = evidence?.let {
                EvidenceBundle(
                    recordingUri = it.recordingUri,
                    screenshots = it.screenshots,
                    logText = it.logText,
                    eventLog = it.eventLog
                )
            } ?: EMPTY_EVIDENCE_BUNDLE,
            qualityScore = quality?.let {
                QualityScore(
                    total = it.total,
                    reproduction = it.reproduction,
                    evidence = it.evidence,
                    description = it.description,
                    severity = it.severity,
                    metadata = it.metadata,
                    coaching = Json.decodeFromString<List<CoachingHint>>(it.coaching)
                )
            },
            aiOutput = aiOutput?.let {
                AiOutput(
                    generatedTitle = it.generatedTitle,
                    generatedSummary = it.generatedSummary,
                    formattedReproSteps = it.formattedReproSteps,
                    safetyPassed = it.safetyPassed,
                    blockedReason = it.blockedReason
                )
            },
            platform = entity.platform,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            syncedAt = entity.syncedAt,
            outcomeFeedback = null
        )
    }

    private fun scheduleSync() {
        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            SyncWorker.buildWorkRequest()
        )
    }

    companion object {
        private const val SYNC_WORK_NAME = "report_sync"
        private val EMPTY_DEVICE_PROFILE = DeviceProfile("", "", "", 0, 0L, "", "")
        private val EMPTY_EVIDENCE_BUNDLE = EvidenceBundle(null, emptyList(), null, null)
    }
}
