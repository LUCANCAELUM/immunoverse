package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Researcher",
    val email: String = "",
    val userCategory: String = "Researcher",
    val reduceMotion: Boolean = false,
    val hiddenModules: List<String> = emptyList(),
    val dashboardWidgets: List<String> = listOf("3d", "daily", "quick", "activity", "feed"),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80).ifBlank { "Researcher" },
        "email" to email.take(120).ifBlank { "user@immunoverse.app" },
        "userCategory" to userCategory,
        "reduceMotion" to reduceMotion,
        "hiddenModules" to hiddenModules.take(20),
        "dashboardWidgets" to dashboardWidgets.take(15),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "topic",
    val itemId: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val meta: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("itemType", itemType)
        put("itemId", itemId.take(128))
        put("title", title.take(200).ifBlank { "Saved Item" })
        subtitle?.take(300)?.let { put("subtitle", it) }
        meta?.take(500)?.let { put("meta", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class SavedSimulation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val presetKey: String = "normal",
    val antigenLoad: Double = 1.0,
    val innateGain: Double = 1.0,
    val tCellGain: Double = 1.0,
    val bCellGain: Double = 1.0,
    val abClear: Double = 1.0,
    val autoTolerance: Double = 1.0,
    val rechallengeDay: Double = 0.0,
    val durationDays: Double = 30.0,
    val peakAntigen: Double = 0.0,
    val peakCytokine: Double = 0.0,
    val peakAntibody: Double = 0.0,
    val peakMemory: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(100).ifBlank { "Simulation Run" },
        "presetKey" to presetKey.take(40),
        "antigenLoad" to antigenLoad.coerceIn(0.0, 10.0),
        "innateGain" to innateGain.coerceIn(0.0, 5.0),
        "tCellGain" to tCellGain.coerceIn(0.0, 5.0),
        "bCellGain" to bCellGain.coerceIn(0.0, 5.0),
        "abClear" to abClear.coerceIn(0.0, 5.0),
        "autoTolerance" to autoTolerance.coerceIn(0.0, 5.0),
        "rechallengeDay" to rechallengeDay.coerceIn(0.0, 90.0),
        "durationDays" to durationDays.coerceIn(5.0, 90.0),
        "peakAntigen" to peakAntigen.coerceAtLeast(0.0),
        "peakCytokine" to peakCytokine.coerceAtLeast(0.0),
        "peakAntibody" to peakAntibody.coerceAtLeast(0.0),
        "peakMemory" to peakMemory.coerceAtLeast(0.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ResearchProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val folder: String = "General",
    val status: String = "active",
    val collaborators: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(120).ifBlank { "Untitled Project" },
        "description" to description.take(1000),
        "folder" to folder.take(80).ifBlank { "General" },
        "status" to if (status in listOf("active", "review", "archived")) status else "active",
        "collaborators" to collaborators.take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ProjectEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val entryType: String = "note",
    val title: String = "",
    val content: String = "",
    val status: String = "active",
    val tag: String? = null,
    val history: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("projectId", projectId)
        put("entryType", entryType)
        put("title", title.take(200).ifBlank { "Entry" })
        put("content", content.take(25000))
        put("status", status.take(40).ifBlank { "active" })
        tag?.take(80)?.let { put("tag", it) }
        put("history", history.take(15))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class AcademyProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val completedLessons: List<String> = emptyList(),
    val bestScore: Int = 0,
    val attempts: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "trackId" to trackId.take(60),
        "completedLessons" to completedLessons.take(30),
        "bestScore" to bestScore.coerceIn(0, 100),
        "attempts" to attempts.coerceIn(0, 10000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CustomContent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contentType: String = "quiz",
    val trackId: String = "intro_imm",
    val prompt: String = "",
    val explanation: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val difficulty: String = "Undergraduate",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "contentType" to if (contentType == "flashcard") "flashcard" else "quiz",
        "trackId" to trackId.take(60).ifBlank { "intro_imm" },
        "prompt" to prompt.take(500).ifBlank { "Question" },
        "explanation" to explanation.take(1500),
        "options" to options.take(6),
        "correctIndex" to correctIndex.coerceIn(0, 5),
        "difficulty" to if (difficulty in listOf("Beginner", "Undergraduate", "Advanced")) difficulty else "Undergraduate",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val level: String = "info",
    val module: String = "System",
    val message: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "level" to if (level in listOf("info", "warn", "error")) level else "info",
        "module" to module.take(60).ifBlank { "System" },
        "message" to message.take(500).ifBlank { "Event logged" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
