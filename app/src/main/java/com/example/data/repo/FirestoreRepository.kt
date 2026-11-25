package com.example.data.repo

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.local.LocalWorkspaceDatabase
import com.example.data.model.AcademyProgress
import com.example.data.model.AuditLogEntry
import com.example.data.model.BookmarkItem
import com.example.data.model.CustomContent
import com.example.data.model.ProjectEntry
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val localRepo: LocalWorkspaceRepository? = null
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        LocalWorkspaceRepository(LocalWorkspaceDatabase.getInstance(context).dao())
    )

    private val auth get() = Firebase.auth
    val isSignedIn: Boolean get() = auth.currentUser != null

    private inline fun <T> observeSafe(
        path: String,
        op: OperationType = OperationType.LIST,
        crossinline localFlow: () -> Flow<T>,
        crossinline builder: (String) -> Flow<T>
    ): Flow<T> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emitAll(localFlow())
            return@flow
        }
        emitAll(
            builder(uid).catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val ex = fsErr ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, op, "users/$uid/$path")
                }
                throw error
            }
        )
    }

    fun observeProfile(): Flow<UserProfile?> = observeSafe(
        path = "",
        op = OperationType.GET,
        localFlow = { localRepo?.observeProfile() ?: flowOf(null) }
    ) { uid ->
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        val user = auth.currentUser
        if (user == null) {
            localRepo?.ensureDefaultProfile()
            return Result.success(Unit)
        }
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = user.displayName?.take(80)?.ifBlank { "Researcher" } ?: "Researcher",
                    email = user.email?.take(120)?.ifBlank { "user@immunoverse.app" } ?: "user@immunoverse.app",
                    userCategory = "Researcher",
                    reduceMotion = false
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        userCategory: String? = null,
        reduceMotion: Boolean? = null,
        hiddenModules: List<String>? = null,
        dashboardWidgets: List<String>? = null,
        displayName: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.updatePreferences(userCategory, reduceMotion, hiddenModules, dashboardWidgets, displayName)
            return Result.success(Unit)
        }
        val path = "users/$uid"
        return try {
            val updates = buildMap<String, Any> {
                displayName?.let { put("displayName", it.trim().take(80).ifBlank { "Researcher" }) }
                userCategory?.let { put("userCategory", it) }
                reduceMotion?.let { put("reduceMotion", it) }
                hiddenModules?.let { put("hiddenModules", it.take(20)) }
                dashboardWidgets?.let { put("dashboardWidgets", it.take(15)) }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeBookmarks(): Flow<List<BookmarkItem>> = observeSafe(
        path = "bookmarks",
        localFlow = { localRepo?.observeBookmarks() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("bookmarks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(BookmarkItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun toggleBookmark(
        itemType: String,
        itemId: String,
        title: String,
        subtitle: String? = null,
        meta: String? = null
    ): Result<Boolean> {
        val safeDocId = "${itemType}_${itemId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val added = localRepo?.toggleBookmark(safeDocId, itemType, itemId, title, subtitle, meta) ?: false
            return Result.success(added)
        }
        var path = "users/$uid/bookmarks/$safeDocId"
        return try {
            val ref = db.collection("users").document(uid).collection("bookmarks").document(safeDocId)
            path = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.delete().await()
                Result.success(false)
            } else {
                val item = BookmarkItem(
                    userId = uid,
                    itemType = itemType,
                    itemId = itemId.take(128),
                    title = title.take(200),
                    subtitle = subtitle?.take(300),
                    meta = meta?.take(500)
                )
                ref.set(item.toCreateMap()).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeSimulations(): Flow<List<SavedSimulation>> = observeSafe(
        path = "simulations",
        localFlow = { localRepo?.observeSimulations() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("simulations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(SavedSimulation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun saveSimulation(sim: SavedSimulation): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val id = localRepo?.saveSimulation(sim) ?: "local_sim"
            return Result.success(id)
        }
        var path = "users/$uid/simulations"
        return try {
            val ref = db.collection("users").document(uid).collection("simulations").document()
            path = ref.path
            ref.set(sim.copy(userId = uid).toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSimulation(simId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.deleteSimulation(simId)
            return Result.success(Unit)
        }
        var path = "users/$uid/simulations/$simId"
        return try {
            val ref = db.collection("users").document(uid).collection("simulations").document(simId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeProjects(): Flow<List<ResearchProject>> = observeSafe(
        path = "projects",
        localFlow = { localRepo?.observeProjects() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("projects")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(ResearchProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun createProject(name: String, description: String, folder: String, collaborators: List<String>): Result<String> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Project name cannot be empty."))
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val id = localRepo?.createProject(cleanName, description, folder, collaborators) ?: "local_proj"
            return Result.success(id)
        }
        var path = "users/$uid/projects"
        return try {
            val ref = db.collection("users").document(uid).collection("projects").document()
            path = ref.path
            val model = ResearchProject(
                userId = uid,
                name = cleanName,
                description = description.trim(),
                folder = folder.trim().ifBlank { "General" },
                status = "active",
                collaborators = collaborators
            )
            ref.set(model.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProject(projectId: String, name: String, description: String, folder: String, status: String, collaborators: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.updateProject(projectId, name, description, folder, status, collaborators)
            return Result.success(Unit)
        }
        var path = "users/$uid/projects/$projectId"
        return try {
            val ref = db.collection("users").document(uid).collection("projects").document(projectId)
            path = ref.path
            ref.update(
                mapOf(
                    "name" to name.trim().take(120).ifBlank { "Untitled Project" },
                    "description" to description.trim().take(1000),
                    "folder" to folder.trim().take(80).ifBlank { "General" },
                    "status" to if (status in listOf("active", "review", "archived")) status else "active",
                    "collaborators" to collaborators.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.deleteProject(projectId)
            return Result.success(Unit)
        }
        var path = "users/$uid/projects/$projectId"
        return try {
            val ref = db.collection("users").document(uid).collection("projects").document(projectId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeProjectEntries(projectId: String): Flow<List<ProjectEntry>> = observeSafe(
        path = "projects/$projectId/entries",
        localFlow = { localRepo?.observeProjectEntries(projectId) ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("projects").document(projectId)
            .collection("entries")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(ProjectEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun addProjectEntry(
        projectId: String,
        entryType: String,
        title: String,
        content: String,
        status: String = "active",
        tag: String? = null
    ): Result<String> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Title cannot be blank."))
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val id = localRepo?.addProjectEntry(projectId, entryType, cleanTitle, content, status, tag) ?: "local_entry"
            return Result.success(id)
        }
        var path = "users/$uid/projects/$projectId/entries"
        return try {
            val ref = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("entries").document()
            path = ref.path
            val model = ProjectEntry(
                userId = uid,
                projectId = projectId,
                entryType = entryType,
                title = cleanTitle,
                content = content,
                status = status,
                tag = tag?.take(80),
                history = emptyList()
            )
            ref.set(model.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectEntry(
        projectId: String,
        entryId: String,
        title: String,
        content: String,
        status: String,
        tag: String?,
        history: List<String>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.updateProjectEntry(projectId, entryId, title, content, status, tag, history)
            return Result.success(Unit)
        }
        var path = "users/$uid/projects/$projectId/entries/$entryId"
        return try {
            val ref = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("entries").document(entryId)
            path = ref.path
            val map = buildMap<String, Any> {
                put("title", title.trim().take(200).ifBlank { "Entry" })
                put("content", content.take(25000))
                put("status", status.take(40).ifBlank { "active" })
                tag?.take(80)?.let { put("tag", it) }
                put("history", history.take(15))
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(map).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProjectEntry(projectId: String, entryId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.deleteProjectEntry(entryId)
            return Result.success(Unit)
        }
        var path = "users/$uid/projects/$projectId/entries/$entryId"
        return try {
            val ref = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("entries").document(entryId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncLocalProjectsToCloud(): Result<Int> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in with Google to sync local projects to Cloud Firestore."))
        val local = localRepo ?: return Result.success(0)
        return try {
            val bundles = local.getProjectsOnce()
            if (bundles.isEmpty()) return Result.success(0)
            var count = 0
            for ((proj, entries) in bundles) {
                val pRef = db.collection("users").document(uid).collection("projects").document()
                pRef.set(proj.copy(userId = uid).toCreateMap()).await()
                for (entry in entries) {
                    val eRef = pRef.collection("entries").document()
                    eRef.set(entry.copy(userId = uid, projectId = pRef.id).toCreateMap()).await()
                }
                local.deleteProject(proj.id)
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/projects")
            Result.failure(e)
        }
    }

    fun observeLocalProjectCount(): Flow<Int> =
        localRepo?.observeProjects()?.map { it.size } ?: flowOf(0)

    fun observeAcademyProgress(): Flow<List<AcademyProgress>> = observeSafe(
        path = "academyProgress",
        localFlow = { localRepo?.observeAcademyProgress() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("academyProgress")
            .snapshots()
            .map { snap -> snap.toObjects(AcademyProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun saveAcademyProgress(trackId: String, completedLessons: List<String>, score: Int? = null): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.saveAcademyProgress(trackId, completedLessons, score)
            return Result.success(Unit)
        }
        var path = "users/$uid/academyProgress/$trackId"
        return try {
            val ref = db.collection("users").document(uid).collection("academyProgress").document(trackId)
            path = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                val curBest = existing.getLong("bestScore")?.toInt() ?: 0
                val curAttempts = existing.getLong("attempts")?.toInt() ?: 0
                val newBest = if (score != null) maxOf(curBest, score.coerceIn(0, 100)) else curBest
                val newAttempts = if (score != null) curAttempts + 1 else curAttempts
                ref.update(
                    mapOf(
                        "completedLessons" to completedLessons.distinct().take(30),
                        "bestScore" to newBest,
                        "attempts" to newAttempts,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val model = AcademyProgress(
                    userId = uid,
                    trackId = trackId,
                    completedLessons = completedLessons.distinct().take(30),
                    bestScore = (score ?: 0).coerceIn(0, 100),
                    attempts = if (score != null) 1 else 0
                )
                ref.set(model.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeCustomContent(): Flow<List<CustomContent>> = observeSafe(
        path = "customContent",
        localFlow = { localRepo?.observeCustomContent() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("customContent")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(CustomContent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun addCustomContent(content: CustomContent): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val id = localRepo?.addCustomContent(content) ?: "local_cc"
            return Result.success(id)
        }
        var path = "users/$uid/customContent"
        return try {
            val ref = db.collection("users").document(uid).collection("customContent").document()
            path = ref.path
            ref.set(content.copy(userId = uid).toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomContent(contentId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.deleteCustomContent(contentId)
            return Result.success(Unit)
        }
        var path = "users/$uid/customContent/$contentId"
        return try {
            val ref = db.collection("users").document(uid).collection("customContent").document(contentId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> = observeSafe(
        path = "auditLogs",
        localFlow = { localRepo?.observeAuditLogs() ?: flowOf(emptyList()) }
    ) { uid ->
        db.collection("users").document(uid).collection("auditLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap -> snap.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun logAudit(level: String, module: String, message: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            localRepo?.logAudit(level, module, message)
            return
        }
        val ref = db.collection("users").document(uid).collection("auditLogs").document()
        try {
            val entry = AuditLogEntry(userId = uid, level = level, module = module, message = message)
            ref.set(entry.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    fun buildBackupJson(
        profile: UserProfile?,
        bookmarks: List<BookmarkItem>,
        sims: List<SavedSimulation>,
        projects: List<ResearchProject>,
        progress: List<AcademyProgress>
    ): String {
        val root = JSONObject()
        root.put("platform", "ImmunoVerse 2.0")
        profile?.let {
            root.put("profile", JSONObject().apply {
                put("displayName", it.displayName)
                put("email", it.email)
                put("userCategory", it.userCategory)
                put("reduceMotion", it.reduceMotion)
            })
        }
        root.put("bookmarks", JSONArray().apply {
            bookmarks.forEach { b ->
                put(JSONObject().apply {
                    put("type", b.itemType)
                    put("id", b.itemId)
                    put("title", b.title)
                })
            }
        })
        root.put("simulations", JSONArray().apply {
            sims.forEach { s ->
                put(JSONObject().apply {
                    put("name", s.name)
                    put("preset", s.presetKey)
                    put("peakAb", s.peakAntibody)
                })
            }
        })
        root.put("projects", JSONArray().apply {
            projects.forEach { p ->
                put(JSONObject().apply {
                    put("name", p.name)
                    put("folder", p.folder)
                    put("status", p.status)
                })
            }
        })
        root.put("academyProgress", JSONArray().apply {
            progress.forEach { ap ->
                put(JSONObject().apply {
                    put("trackId", ap.trackId)
                    put("completedCount", ap.completedLessons.size)
                    put("bestScore", ap.bestScore)
                })
            }
        })
        return root.toString(2)
    }
}
