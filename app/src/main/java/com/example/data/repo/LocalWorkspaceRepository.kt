package com.example.data.repo

import com.example.data.local.LocalAcademyProgressEntity
import com.example.data.local.LocalAuditLogEntity
import com.example.data.local.LocalBookmarkEntity
import com.example.data.local.LocalCustomContentEntity
import com.example.data.local.LocalJsonCodec
import com.example.data.local.LocalProfileEntity
import com.example.data.local.LocalProjectEntity
import com.example.data.local.LocalProjectEntryEntity
import com.example.data.local.LocalSimulationEntity
import com.example.data.local.LocalWorkspaceDao
import com.example.data.local.toDomain
import com.example.data.model.AcademyProgress
import com.example.data.model.AuditLogEntry
import com.example.data.model.BookmarkItem
import com.example.data.model.CustomContent
import com.example.data.model.ProjectEntry
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalWorkspaceRepository(private val dao: LocalWorkspaceDao) {

    fun observeProfile(): Flow<UserProfile?> =
        dao.observeProfile().map { it?.toDomain() ?: defaultGuestProfile() }

    suspend fun ensureDefaultProfile() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(LocalProfileEntity())
        }
    }

    suspend fun updatePreferences(
        userCategory: String? = null,
        reduceMotion: Boolean? = null,
        hiddenModules: List<String>? = null,
        dashboardWidgets: List<String>? = null,
        displayName: String? = null
    ) {
        val current = dao.getProfileOnce() ?: LocalProfileEntity()
        val updated = current.copy(
            displayName = displayName?.trim()?.take(80)?.ifBlank { "Guest Researcher" } ?: current.displayName,
            userCategory = userCategory ?: current.userCategory,
            reduceMotion = reduceMotion ?: current.reduceMotion,
            hiddenModulesJson = hiddenModules?.let { LocalJsonCodec.encodeList(it.take(20)) } ?: current.hiddenModulesJson,
            dashboardWidgetsJson = dashboardWidgets?.let { LocalJsonCodec.encodeList(it.take(15)) } ?: current.dashboardWidgetsJson
        )
        dao.upsertProfile(updated)
    }

    fun observeProjects(): Flow<List<ResearchProject>> =
        dao.observeProjects().map { list -> list.map { it.toDomain() } }

    suspend fun getProjectsOnce(): List<Pair<ResearchProject, List<ProjectEntry>>> {
        return dao.getProjectsOnce().map { proj ->
            val entries = dao.getEntriesOnce(proj.id).map { it.toDomain() }
            proj.toDomain() to entries
        }
    }

    suspend fun createProject(
        name: String,
        description: String,
        folder: String,
        collaborators: List<String>
    ): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Project name cannot be empty." }
        val now = System.currentTimeMillis()
        val id = "proj_${UUID.randomUUID().toString().take(12)}"
        dao.upsertProject(
            LocalProjectEntity(
                id = id,
                name = cleanName.take(120),
                description = description.trim().take(1000),
                folder = folder.trim().take(80).ifBlank { "General" },
                status = "active",
                collaboratorsJson = LocalJsonCodec.encodeList(collaborators.take(20)),
                createdAtMs = now,
                updatedAtMs = now
            )
        )
        return id
    }

    suspend fun updateProject(
        projectId: String,
        name: String,
        description: String,
        folder: String,
        status: String,
        collaborators: List<String>
    ) {
        val existing = dao.getProjectById(projectId) ?: return
        dao.upsertProject(
            existing.copy(
                name = name.trim().take(120).ifBlank { "Untitled Project" },
                description = description.trim().take(1000),
                folder = folder.trim().take(80).ifBlank { "General" },
                status = if (status in listOf("active", "review", "archived")) status else "active",
                collaboratorsJson = LocalJsonCodec.encodeList(collaborators.take(20)),
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteProjectById(projectId)
    }

    fun observeProjectEntries(projectId: String): Flow<List<ProjectEntry>> =
        dao.observeEntries(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun addProjectEntry(
        projectId: String,
        entryType: String,
        title: String,
        content: String,
        status: String,
        tag: String?
    ): String {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Title cannot be blank." }
        val now = System.currentTimeMillis()
        val entryId = "entry_${UUID.randomUUID().toString().take(12)}"
        dao.upsertEntry(
            LocalProjectEntryEntity(
                id = entryId,
                projectId = projectId,
                entryType = entryType,
                title = cleanTitle.take(200),
                content = content.take(25000),
                status = status.take(40).ifBlank { "active" },
                tag = tag?.take(80),
                historyJson = "[]",
                createdAtMs = now,
                updatedAtMs = now
            )
        )
        dao.getProjectById(projectId)?.let { proj ->
            dao.upsertProject(proj.copy(updatedAtMs = now))
        }
        return entryId
    }

    suspend fun updateProjectEntry(
        projectId: String,
        entryId: String,
        title: String,
        content: String,
        status: String,
        tag: String?,
        history: List<String>
    ) {
        val existing = dao.getEntriesOnce(projectId).firstOrNull { it.id == entryId }
        val now = System.currentTimeMillis()
        dao.upsertEntry(
            LocalProjectEntryEntity(
                id = entryId,
                projectId = projectId,
                entryType = existing?.entryType ?: "note",
                title = title.trim().take(200).ifBlank { "Entry" },
                content = content.take(25000),
                status = status.take(40).ifBlank { "active" },
                tag = tag?.take(80),
                historyJson = LocalJsonCodec.encodeList(history.take(15)),
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now
            )
        )
    }

    suspend fun deleteProjectEntry(entryId: String) {
        dao.deleteEntryById(entryId)
    }

    fun observeBookmarks(): Flow<List<BookmarkItem>> =
        dao.observeBookmarks().map { list -> list.map { it.toDomain() } }

    suspend fun toggleBookmark(
        safeDocId: String,
        itemType: String,
        itemId: String,
        title: String,
        subtitle: String?,
        meta: String?
    ): Boolean {
        val existing = dao.getBookmarkById(safeDocId)
        return if (existing != null) {
            dao.deleteBookmarkById(safeDocId)
            false
        } else {
            dao.upsertBookmark(
                LocalBookmarkEntity(
                    id = safeDocId,
                    itemType = itemType,
                    itemId = itemId.take(128),
                    title = title.take(200),
                    subtitle = subtitle?.take(300),
                    meta = meta?.take(500),
                    createdAtMs = System.currentTimeMillis()
                )
            )
            true
        }
    }

    fun observeSimulations(): Flow<List<SavedSimulation>> =
        dao.observeSimulations().map { list -> list.map { it.toDomain() } }

    suspend fun saveSimulation(sim: SavedSimulation): String {
        val id = "sim_${UUID.randomUUID().toString().take(12)}"
        dao.upsertSimulation(
            LocalSimulationEntity(
                id = id,
                name = sim.name.take(100).ifBlank { "Simulation Run" },
                presetKey = sim.presetKey.take(40),
                antigenLoad = sim.antigenLoad,
                innateGain = sim.innateGain,
                tCellGain = sim.tCellGain,
                bCellGain = sim.bCellGain,
                abClear = sim.abClear,
                autoTolerance = sim.autoTolerance,
                rechallengeDay = sim.rechallengeDay,
                durationDays = sim.durationDays,
                peakAntigen = sim.peakAntigen,
                peakCytokine = sim.peakCytokine,
                peakAntibody = sim.peakAntibody,
                peakMemory = sim.peakMemory,
                createdAtMs = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deleteSimulation(simId: String) {
        dao.deleteSimulationById(simId)
    }

    fun observeAcademyProgress(): Flow<List<AcademyProgress>> =
        dao.observeAcademyProgress().map { list -> list.map { it.toDomain() } }

    suspend fun saveAcademyProgress(trackId: String, completedLessons: List<String>, score: Int?) {
        val existing = dao.getProgressOnce(trackId)
        val newBest = if (score != null) maxOf(existing?.bestScore ?: 0, score.coerceIn(0, 100)) else (existing?.bestScore ?: 0)
        val newAttempts = if (score != null) (existing?.attempts ?: 0) + 1 else (existing?.attempts ?: 0)
        dao.upsertAcademyProgress(
            LocalAcademyProgressEntity(
                trackId = trackId,
                completedLessonsJson = LocalJsonCodec.encodeList(completedLessons.distinct().take(30)),
                bestScore = newBest,
                attempts = newAttempts,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    fun observeCustomContent(): Flow<List<CustomContent>> =
        dao.observeCustomContent().map { list -> list.map { it.toDomain() } }

    suspend fun addCustomContent(content: CustomContent): String {
        val id = "cc_${UUID.randomUUID().toString().take(12)}"
        dao.upsertCustomContent(
            LocalCustomContentEntity(
                id = id,
                contentType = content.contentType,
                trackId = content.trackId,
                prompt = content.prompt,
                explanation = content.explanation,
                optionsJson = LocalJsonCodec.encodeList(content.options.take(6)),
                correctIndex = content.correctIndex,
                difficulty = content.difficulty,
                createdAtMs = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deleteCustomContent(id: String) {
        dao.deleteCustomContentById(id)
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> =
        dao.observeAuditLogs().map { list -> list.map { it.toDomain() } }

    suspend fun logAudit(level: String, module: String, message: String) {
        val id = "log_${UUID.randomUUID().toString().take(12)}"
        dao.insertAuditLog(
            LocalAuditLogEntity(
                id = id,
                level = level,
                module = module,
                message = message,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    private fun defaultGuestProfile() = UserProfile(
        id = "guest",
        userId = "guest",
        displayName = "Guest Researcher",
        email = "guest@local.immunoverse",
        userCategory = "Researcher",
        reduceMotion = false
    )
}
