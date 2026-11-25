package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AcademyProgress
import com.example.data.model.AuditLogEntry
import com.example.data.model.BookmarkItem
import com.example.data.model.CustomContent
import com.example.data.model.ProjectEntry
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "local_profiles")
data class LocalProfileEntity(
    @PrimaryKey val id: String = "guest",
    val displayName: String = "Guest Researcher",
    val email: String = "guest@local.immunoverse",
    val userCategory: String = "Researcher",
    val reduceMotion: Boolean = false,
    val hiddenModulesJson: String = "[]",
    val dashboardWidgetsJson: String = "[\"3d\",\"daily\",\"quick\",\"activity\",\"feed\"]"
)

@Entity(tableName = "local_projects")
data class LocalProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val folder: String,
    val status: String,
    val collaboratorsJson: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(
    tableName = "local_project_entries",
    foreignKeys = [
        ForeignKey(
            entity = LocalProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class LocalProjectEntryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val entryType: String,
    val title: String,
    val content: String,
    val status: String,
    val tag: String?,
    val historyJson: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "local_bookmarks")
data class LocalBookmarkEntity(
    @PrimaryKey val id: String,
    val itemType: String,
    val itemId: String,
    val title: String,
    val subtitle: String?,
    val meta: String?,
    val createdAtMs: Long
)

@Entity(tableName = "local_simulations")
data class LocalSimulationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val presetKey: String,
    val antigenLoad: Double,
    val innateGain: Double,
    val tCellGain: Double,
    val bCellGain: Double,
    val abClear: Double,
    val autoTolerance: Double,
    val rechallengeDay: Double,
    val durationDays: Double,
    val peakAntigen: Double,
    val peakCytokine: Double,
    val peakAntibody: Double,
    val peakMemory: Double,
    val createdAtMs: Long
)

@Entity(tableName = "local_academy_progress")
data class LocalAcademyProgressEntity(
    @PrimaryKey val trackId: String,
    val completedLessonsJson: String,
    val bestScore: Int,
    val attempts: Int,
    val updatedAtMs: Long
)

@Entity(tableName = "local_custom_content")
data class LocalCustomContentEntity(
    @PrimaryKey val id: String,
    val contentType: String,
    val trackId: String,
    val prompt: String,
    val explanation: String,
    val optionsJson: String,
    val correctIndex: Int,
    val difficulty: String,
    val createdAtMs: Long
)

@Entity(tableName = "local_audit_logs")
data class LocalAuditLogEntity(
    @PrimaryKey val id: String,
    val level: String,
    val module: String,
    val message: String,
    val createdAtMs: Long
)

@Dao
interface LocalWorkspaceDao {
    @Query("SELECT * FROM local_profiles WHERE id = 'guest' LIMIT 1")
    fun observeProfile(): Flow<LocalProfileEntity?>

    @Query("SELECT * FROM local_profiles WHERE id = 'guest' LIMIT 1")
    suspend fun getProfileOnce(): LocalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: LocalProfileEntity)

    @Query("SELECT * FROM local_projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<LocalProjectEntity>>

    @Query("SELECT * FROM local_projects ORDER BY updatedAtMs DESC")
    suspend fun getProjectsOnce(): List<LocalProjectEntity>

    @Query("SELECT * FROM local_projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): LocalProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: LocalProjectEntity)

    @Query("DELETE FROM local_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("SELECT * FROM local_project_entries WHERE projectId = :projectId ORDER BY updatedAtMs DESC")
    fun observeEntries(projectId: String): Flow<List<LocalProjectEntryEntity>>

    @Query("SELECT * FROM local_project_entries WHERE projectId = :projectId ORDER BY updatedAtMs DESC")
    suspend fun getEntriesOnce(projectId: String): List<LocalProjectEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entity: LocalProjectEntryEntity)

    @Query("DELETE FROM local_project_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)

    @Query("SELECT * FROM local_bookmarks ORDER BY createdAtMs DESC")
    fun observeBookmarks(): Flow<List<LocalBookmarkEntity>>

    @Query("SELECT * FROM local_bookmarks WHERE id = :id LIMIT 1")
    suspend fun getBookmarkById(id: String): LocalBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(entity: LocalBookmarkEntity)

    @Query("DELETE FROM local_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("SELECT * FROM local_simulations ORDER BY createdAtMs DESC")
    fun observeSimulations(): Flow<List<LocalSimulationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSimulation(entity: LocalSimulationEntity)

    @Query("DELETE FROM local_simulations WHERE id = :simId")
    suspend fun deleteSimulationById(simId: String)

    @Query("SELECT * FROM local_academy_progress")
    fun observeAcademyProgress(): Flow<List<LocalAcademyProgressEntity>>

    @Query("SELECT * FROM local_academy_progress WHERE trackId = :trackId LIMIT 1")
    suspend fun getProgressOnce(trackId: String): LocalAcademyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAcademyProgress(entity: LocalAcademyProgressEntity)

    @Query("SELECT * FROM local_custom_content ORDER BY createdAtMs DESC")
    fun observeCustomContent(): Flow<List<LocalCustomContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomContent(entity: LocalCustomContentEntity)

    @Query("DELETE FROM local_custom_content WHERE id = :id")
    suspend fun deleteCustomContentById(id: String)

    @Query("SELECT * FROM local_audit_logs ORDER BY createdAtMs DESC LIMIT 50")
    fun observeAuditLogs(): Flow<List<LocalAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(entity: LocalAuditLogEntity)
}

@Database(
    entities = [
        LocalProfileEntity::class,
        LocalProjectEntity::class,
        LocalProjectEntryEntity::class,
        LocalBookmarkEntity::class,
        LocalSimulationEntity::class,
        LocalAcademyProgressEntity::class,
        LocalCustomContentEntity::class,
        LocalAuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LocalWorkspaceDatabase : RoomDatabase() {
    abstract fun dao(): LocalWorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: LocalWorkspaceDatabase? = null

        fun getInstance(context: Context): LocalWorkspaceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalWorkspaceDatabase::class.java,
                    "immunoverse_local_workspace.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

internal object LocalJsonCodec {
    fun encodeList(list: List<String>): String = JSONArray(list).toString()

    fun decodeList(json: String): List<String> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) add(arr.optString(i))
        }.filter { it.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

fun LocalProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    userId = "guest",
    displayName = displayName,
    email = email,
    userCategory = userCategory,
    reduceMotion = reduceMotion,
    hiddenModules = LocalJsonCodec.decodeList(hiddenModulesJson),
    dashboardWidgets = LocalJsonCodec.decodeList(dashboardWidgetsJson).ifEmpty {
        listOf("3d", "daily", "quick", "activity", "feed")
    }
)

fun LocalProjectEntity.toDomain(): ResearchProject = ResearchProject(
    id = id,
    userId = "guest",
    name = name,
    description = description,
    folder = folder,
    status = status,
    collaborators = LocalJsonCodec.decodeList(collaboratorsJson)
)

fun LocalProjectEntryEntity.toDomain(): ProjectEntry = ProjectEntry(
    id = id,
    userId = "guest",
    projectId = projectId,
    entryType = entryType,
    title = title,
    content = content,
    status = status,
    tag = tag,
    history = LocalJsonCodec.decodeList(historyJson)
)

fun LocalBookmarkEntity.toDomain(): BookmarkItem = BookmarkItem(
    id = id,
    userId = "guest",
    itemType = itemType,
    itemId = itemId,
    title = title,
    subtitle = subtitle,
    meta = meta
)

fun LocalSimulationEntity.toDomain(): SavedSimulation = SavedSimulation(
    id = id,
    userId = "guest",
    name = name,
    presetKey = presetKey,
    antigenLoad = antigenLoad,
    innateGain = innateGain,
    tCellGain = tCellGain,
    bCellGain = bCellGain,
    abClear = abClear,
    autoTolerance = autoTolerance,
    rechallengeDay = rechallengeDay,
    durationDays = durationDays,
    peakAntigen = peakAntigen,
    peakCytokine = peakCytokine,
    peakAntibody = peakAntibody,
    peakMemory = peakMemory
)

fun LocalAcademyProgressEntity.toDomain(): AcademyProgress = AcademyProgress(
    id = trackId,
    userId = "guest",
    trackId = trackId,
    completedLessons = LocalJsonCodec.decodeList(completedLessonsJson),
    bestScore = bestScore,
    attempts = attempts
)

fun LocalCustomContentEntity.toDomain(): CustomContent = CustomContent(
    id = id,
    userId = "guest",
    contentType = contentType,
    trackId = trackId,
    prompt = prompt,
    explanation = explanation,
    options = LocalJsonCodec.decodeList(optionsJson),
    correctIndex = correctIndex,
    difficulty = difficulty
)

fun LocalAuditLogEntity.toDomain(): AuditLogEntry = AuditLogEntry(
    id = id,
    userId = "guest",
    level = level,
    module = module,
    message = message
)
