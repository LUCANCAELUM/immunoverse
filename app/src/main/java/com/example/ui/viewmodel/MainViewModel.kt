package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AcademyProgress
import com.example.data.model.AuditLogEntry
import com.example.data.model.BookmarkItem
import com.example.data.model.CustomContent
import com.example.data.model.ProjectEntry
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.network.LiteraturePaper
import com.example.data.network.ScientificApiClient
import com.example.data.repo.FirestoreRepository
import com.example.domain.ResearchAlertItem
import com.example.domain.ResearchAlertsEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repo = FirestoreRepository(application)

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    private val _recentItems = MutableStateFlow<List<String>>(listOf("CD4+ T Helper", "IgG (1IGT)", "JAK–STAT"))
    val recentItems = _recentItems.asStateFlow()

    private val _liveFeed = MutableStateFlow<UiState<List<LiteraturePaper>>>(UiState.Loading)
    val liveFeed = _liveFeed.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    private val _sharedAnalyticsCsv = MutableStateFlow<String?>(null)
    val sharedAnalyticsCsv = _sharedAnalyticsCsv.asStateFlow()

    private fun <T> Flow<T>.toSafeUiState(fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                emit(UiState.Error(root.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val profileState: StateFlow<UiState<UserProfile?>> = repo.observeProfile().toSafeUiState("Failed to load profile")
    val bookmarksState: StateFlow<UiState<List<BookmarkItem>>> = repo.observeBookmarks().toSafeUiState("Failed to load bookmarks")
    val simulationsState: StateFlow<UiState<List<SavedSimulation>>> = repo.observeSimulations().toSafeUiState("Failed to load simulations")
    val projectsState: StateFlow<UiState<List<ResearchProject>>> = repo.observeProjects().toSafeUiState("Failed to load projects")
    val academyProgressState: StateFlow<UiState<List<AcademyProgress>>> = repo.observeAcademyProgress().toSafeUiState("Failed to load progress")
    val customContentState: StateFlow<UiState<List<CustomContent>>> = repo.observeCustomContent().toSafeUiState("Failed to load custom content")
    val auditLogsState: StateFlow<UiState<List<AuditLogEntry>>> = repo.observeAuditLogs().toSafeUiState("Failed to load logs")

    val localProjectCount: StateFlow<Int> = repo.observeLocalProjectCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val activeProjectEntries: StateFlow<UiState<List<ProjectEntry>>> = _selectedProjectId
        .flatMapLatest { pid ->
            if (pid.isNullOrBlank()) flowOf(emptyList()) else repo.observeProjectEntries(pid)
        }
        .toSafeUiState("Failed to load project entries")

    private val alertPrefs = application.getSharedPreferences("immunoverse_alerts", Context.MODE_PRIVATE)
    private val _savedInterests = MutableStateFlow(
        alertPrefs.getStringSet("interests", null)?.toSet()
            ?: setOf("SARS-CoV-2 Spike & Variants", "CAR-T & Checkpoint Blockade", "Proteomics & Mass Spectrometry")
    )
    val savedInterests: StateFlow<Set<String>> = _savedInterests.asStateFlow()

    private val _readAlertIds = MutableStateFlow(
        alertPrefs.getStringSet("read_alert_ids", emptySet())?.toSet() ?: emptySet()
    )
    val readAlertIds: StateFlow<Set<String>> = _readAlertIds.asStateFlow()

    private val _interestPapers = MutableStateFlow<List<LiteraturePaper>>(emptyList())

    val effectiveInterests: StateFlow<List<String>> = combine(
        _savedInterests,
        bookmarksState,
        projectsState
    ) { explicit, bState, pState ->
        ResearchAlertsEngine.extractEffectiveInterests(
            explicitInterests = explicit,
            bookmarks = (bState as? UiState.Success)?.data ?: emptyList(),
            projects = (pState as? UiState.Success)?.data ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _savedInterests.value.toList())

    val researchAlerts: StateFlow<List<ResearchAlertItem>> = combine(
        effectiveInterests,
        _interestPapers,
        _liveFeed
    ) { interests, tailoredPapers, generalFeed ->
        val feedPapers = (generalFeed as? UiState.Success)?.data ?: emptyList()
        val merged = (tailoredPapers + feedPapers).distinctBy { it.id }
        ResearchAlertsEngine.buildAlerts(interests, merged)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ResearchAlertsEngine.buildAlerts(_savedInterests.value.toList(), emptyList()))

    init {
        viewModelScope.launch {
            repo.ensureUserProfile()
        }
        refreshLiveFeed()
        refreshInterestAlerts(silent = true)
    }

    fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    fun toggleResearchInterest(topic: String) {
        val clean = topic.trim()
        if (clean.isBlank()) return
        val next = _savedInterests.value.toMutableSet()
        val added = if (clean in next) {
            next.remove(clean)
            false
        } else {
            next.add(clean)
            true
        }
        _savedInterests.value = next
        alertPrefs.edit().putStringSet("interests", next).apply()
        emitToast(if (added) "Subscribed to alerts: $clean" else "Removed interest: $clean")
        if (added) refreshInterestAlerts(silent = true)
    }

    fun markAlertRead(alertId: String) {
        if (alertId in _readAlertIds.value) return
        val next = _readAlertIds.value + alertId
        _readAlertIds.value = next
        alertPrefs.edit().putStringSet("read_alert_ids", next).apply()
    }

    fun markAllAlertsRead() {
        val allIds = researchAlerts.value.map { it.id }.toSet()
        val next = _readAlertIds.value + allIds
        _readAlertIds.value = next
        alertPrefs.edit().putStringSet("read_alert_ids", next).apply()
    }

    fun refreshInterestAlerts(silent: Boolean = false) {
        viewModelScope.launch {
            val query = ResearchAlertsEngine.buildEuropePmcInterestQuery(effectiveInterests.value)
            ScientificApiClient.searchEuropePmc(query, yearFrom = 2024)
                .onSuccess { list ->
                    _interestPapers.value = list.take(8)
                    val unreadNew = list.count { "live_${it.id}" !in _readAlertIds.value }
                    if (!silent) {
                        emitToast("Fetched ${list.size} literature alerts ($unreadNew unread) for your research interests")
                    }
                    repo.logAudit("info", "Alerts", "Synced ${list.size} literature alerts for saved interests")
                }
                .onFailure { err ->
                    if (!silent) emitToast(err.localizedMessage ?: "Literature alert sync unavailable offline")
                }
        }
    }

    fun recordRecent(label: String) {
        val current = _recentItems.value.toMutableList()
        current.remove(label)
        current.add(0, label)
        _recentItems.value = current.take(8)
    }

    fun selectProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun sendCsvToAnalytics(csv: String) {
        _sharedAnalyticsCsv.value = csv
    }

    fun refreshLiveFeed() {
        viewModelScope.launch {
            _liveFeed.value = UiState.Loading
            ScientificApiClient.searchEuropePmc("immunology OR immunotherapy OR virology", yearFrom = 2024)
                .onSuccess { list -> _liveFeed.value = UiState.Success(list.take(6)) }
                .onFailure { err -> _liveFeed.value = UiState.Error(err.localizedMessage ?: "Europe PMC unavailable offline.") }
        }
    }

    fun toggleBookmark(itemType: String, itemId: String, title: String, subtitle: String? = null, meta: String? = null) {
        viewModelScope.launch {
            repo.toggleBookmark(itemType, itemId, title, subtitle, meta)
                .onSuccess { added ->
                    emitToast(if (added) "Saved bookmark: $title" else "Removed bookmark: $title")
                    repo.logAudit("info", "Bookmarks", "${if (added) "Saved" else "Removed"} $title")
                }
                .onFailure { err ->
                    emitToast("Bookmark error: ${err.localizedMessage}")
                }
        }
    }

    fun saveSimulation(sim: SavedSimulation) {
        viewModelScope.launch {
            repo.saveSimulation(sim)
                .onSuccess {
                    val dest = if (repo.isSignedIn) "Firestore" else "local device DB"
                    emitToast("Saved simulation '${sim.name}' to $dest")
                    repo.logAudit("info", "Simulator", "Saved simulation: ${sim.name}")
                }
                .onFailure { err -> emitToast("Save failed: ${err.localizedMessage}") }
        }
    }

    fun deleteSimulation(simId: String) {
        viewModelScope.launch {
            repo.deleteSimulation(simId)
                .onSuccess { emitToast("Deleted simulation") }
                .onFailure { err -> emitToast("Delete failed: ${err.localizedMessage}") }
        }
    }

    fun createProject(name: String, description: String, folder: String, collaborators: List<String>) {
        viewModelScope.launch {
            repo.createProject(name, description, folder, collaborators)
                .onSuccess { pid ->
                    _selectedProjectId.value = pid
                    val dest = if (repo.isSignedIn) "cloud" else "local storage"
                    emitToast("Saved project '$name' ($dest)")
                    repo.logAudit("info", "Workspace", "Created project $name")
                }
                .onFailure { err -> emitToast("Project error: ${err.localizedMessage}") }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repo.deleteProject(projectId)
                .onSuccess {
                    if (_selectedProjectId.value == projectId) _selectedProjectId.value = null
                    emitToast("Project deleted")
                }
                .onFailure { err -> emitToast("Delete error: ${err.localizedMessage}") }
        }
    }

    fun syncLocalProjectsToCloud() {
        viewModelScope.launch {
            repo.syncLocalProjectsToCloud()
                .onSuccess { count ->
                    emitToast(if (count > 0) "Synced $count local project(s) to Cloud Firestore" else "No local-only projects waiting to sync")
                    if (count > 0) repo.logAudit("info", "Workspace", "Synced $count local project(s) to Firestore")
                }
                .onFailure { err -> emitToast(err.localizedMessage ?: "Cloud sync failed") }
        }
    }

    fun addProjectEntry(projectId: String, entryType: String, title: String, content: String, status: String = "active", tag: String? = null) {
        viewModelScope.launch {
            repo.addProjectEntry(projectId, entryType, title, content, status, tag)
                .onSuccess {
                    emitToast("Saved $entryType: $title")
                    repo.logAudit("info", "Workspace", "Added $entryType '$title'")
                }
                .onFailure { err -> emitToast("Entry failed: ${err.localizedMessage}") }
        }
    }

    fun updateProjectEntry(projectId: String, entry: ProjectEntry, newTitle: String, newContent: String, newStatus: String) {
        viewModelScope.launch {
            val prevHistory = entry.history.toMutableList()
            if (entry.content != newContent) {
                prevHistory.add(0, entry.content.take(240))
            }
            repo.updateProjectEntry(projectId, entry.id, newTitle, newContent, newStatus, entry.tag, prevHistory.take(15))
                .onSuccess { emitToast("Updated '${newTitle}' (version saved)") }
                .onFailure { err -> emitToast("Update error: ${err.localizedMessage}") }
        }
    }

    fun deleteProjectEntry(projectId: String, entryId: String) {
        viewModelScope.launch {
            repo.deleteProjectEntry(projectId, entryId)
                .onSuccess { emitToast("Entry deleted") }
                .onFailure { err -> emitToast("Delete error: ${err.localizedMessage}") }
        }
    }

    fun markLessonCompleted(trackId: String, lessonId: String, existingCompleted: List<String>) {
        val next = (existingCompleted + lessonId).distinct()
        viewModelScope.launch {
            repo.saveAcademyProgress(trackId, next)
                .onSuccess { emitToast("Lesson recorded in progress") }
        }
    }

    fun recordQuizScore(trackId: String, scorePct: Int, existingCompleted: List<String>) {
        viewModelScope.launch {
            repo.saveAcademyProgress(trackId, existingCompleted, scorePct)
                .onSuccess {
                    emitToast("Quiz score $scorePct% saved")
                    repo.logAudit("info", "Academy", "Completed quiz on $trackId ($scorePct%)")
                }
        }
    }

    fun updatePreferences(
        userCategory: String? = null,
        reduceMotion: Boolean? = null,
        hiddenModules: List<String>? = null,
        dashboardWidgets: List<String>? = null,
        displayName: String? = null
    ) {
        viewModelScope.launch {
            repo.updateProfilePreferences(userCategory, reduceMotion, hiddenModules, dashboardWidgets, displayName)
                .onSuccess { emitToast("Workspace preferences updated") }
                .onFailure { err -> emitToast("Preference update failed: ${err.localizedMessage}") }
        }
    }

    fun addCustomContent(content: CustomContent) {
        viewModelScope.launch {
            repo.addCustomContent(content)
                .onSuccess {
                    emitToast("Added custom ${content.contentType}")
                    repo.logAudit("info", "Admin", "Created custom ${content.contentType}")
                }
                .onFailure { err -> emitToast("Save failed: ${err.localizedMessage}") }
        }
    }

    fun deleteCustomContent(id: String) {
        viewModelScope.launch {
            repo.deleteCustomContent(id).onSuccess { emitToast("Removed custom item") }
        }
    }

    fun logEvent(level: String, module: String, message: String) {
        viewModelScope.launch { repo.logAudit(level, module, message) }
    }
}
