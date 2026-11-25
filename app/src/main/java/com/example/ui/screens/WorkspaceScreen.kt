package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.data.model.ProjectEntry
import com.example.data.model.ResearchProject
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper

@Composable
fun WorkspaceScreen(
    projects: List<ResearchProject>,
    selectedProjectId: String?,
    entries: List<ProjectEntry>,
    bookmarks: List<BookmarkItem>,
    onSelectProject: (String?) -> Unit,
    onCreateProject: (String, String, String, List<String>) -> Unit,
    onDeleteProject: (String) -> Unit,
    onAddEntry: (String, String, String, String, String, String?) -> Unit,
    onUpdateEntry: (String, ProjectEntry, String, String, String) -> Unit,
    onDeleteEntry: (String, String) -> Unit,
    onSendToAnalytics: (String) -> Unit,
    onToast: (String) -> Unit,
    isSignedIn: Boolean = false,
    localProjectCount: Int = 0,
    onSyncLocalToCloud: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    LaunchedEffect(projects, selectedProjectId) {
        if (selectedProjectId == null && projects.isNotEmpty()) {
            onSelectProject(projects.first().id)
        }
    }

    val activeProject = remember(projects, selectedProjectId) {
        projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    }

    val tabOptions = listOf("Notes", "Hypotheses", "Exp Logs", "Timeline", "References", "Datasets")
    var subTab by remember { mutableStateOf("Notes") }

    var showCreateProj by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var newProjFolder by remember { mutableStateOf("Immunology") }
    var newProjCollabs by remember { mutableStateOf("Lead Investigator (Owner), Co-Author (Editor)") }

    var editingEntry by remember { mutableStateOf<ProjectEntry?>(null) }
    var historyEntry by remember { mutableStateOf<ProjectEntry?>(null) }
    var entryTitle by remember { mutableStateOf("") }
    var entryContent by remember { mutableStateOf("") }
    var entryStatus by remember { mutableStateOf("active") }
    var showAddEntry by remember { mutableStateOf(false) }

    val entryTypeForTab = when (subTab) {
        "Notes" -> "note"
        "Hypotheses" -> "hypothesis"
        "Exp Logs" -> "exp_log"
        "Timeline" -> "milestone"
        "References" -> "reference"
        else -> "dataset"
    }

    val filteredEntries = remember(entries, entryTypeForTab) {
        entries.filter { it.entryType == entryTypeForTab }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Research Workspace",
            subtitle = if (isSignedIn) {
                "Cloud-synced projects, Markdown notes with version history, hypotheses & logs."
            } else {
                "Locally persisted projects (SQLite), Markdown notes, hypotheses & logs."
            },
            badge = if (isSignedIn) "Cloud Sync" else "Local Saved"
        )

        if (isSignedIn && localProjectCount > 0) {
            GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$localProjectCount local project(s) stored on device",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onSyncLocalToCloud, modifier = Modifier.testTag("ws_sync_local_btn")) {
                        Text("Sync to Cloud")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCreateProj = true },
                modifier = Modifier.weight(1f).testTag("ws_new_project_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Project")
            }
            if (activeProject != null) {
                OutlinedButton(
                    onClick = {
                        val secs = mutableListOf(
                            "Project Metadata" to "Folder: ${activeProject.folder}\nDescription: ${activeProject.description}\nCollaborators: ${activeProject.collaborators.joinToString()}"
                        )
                        tabOptions.forEach { t ->
                            val typeKey = when (t) {
                                "Notes" -> "note"; "Hypotheses" -> "hypothesis"; "Exp Logs" -> "exp_log"
                                "Timeline" -> "milestone"; "References" -> "reference"; else -> "dataset"
                            }
                            val items = entries.filter { it.entryType == typeKey }
                            if (items.isNotEmpty()) {
                                secs += t to items.joinToString("\n\n") { "${it.title} [${it.status}]\n${it.content}" }
                            }
                        }
                        ExportHelper.exportAndSharePdf(context, "project_${activeProject.name}.pdf", activeProject.name, secs)
                            .onSuccess { onToast("Shared PDF $it") }
                            .onFailure { onToast("PDF failed: ${it.localizedMessage}") }
                    },
                    modifier = Modifier.testTag("ws_export_pdf_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        val md = buildString {
                            appendLine("# ${activeProject.name}")
                            appendLine("Folder: ${activeProject.folder} | Status: ${activeProject.status}")
                            appendLine("Collaborators: ${activeProject.collaborators.joinToString()}")
                            appendLine("\n${activeProject.description}\n")
                            entries.forEach { e ->
                                appendLine("## [${e.entryType.uppercase()}] ${e.title} (${e.status})")
                                appendLine(e.content)
                                appendLine()
                            }
                        }
                        ExportHelper.shareTextFile(context, "project_${activeProject.name}.md", "text/markdown", md)
                            .onSuccess { onToast("Shared Markdown $it") }
                            .onFailure { onToast("Export failed: ${it.localizedMessage}") }
                    },
                    modifier = Modifier.testTag("ws_export_md_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MD")
                }
            }
        }

        if (projects.isEmpty()) {
            GlassCard {
                Text("No research projects yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isSignedIn) "Tap 'New Project' to create your first Firestore-synced immunology workspace."
                    else "Tap 'New Project' to create and persist your immunology project locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.mutedText
                )
            }
        } else {
            PillSelectorRow(
                options = projects.map { it.name },
                selected = activeProject?.name ?: "",
                onSelect = { name ->
                    val found = projects.firstOrNull { it.name == name }
                    if (found != null) onSelectProject(found.id)
                },
                tagPrefix = "ws_proj"
            )
        }

        if (activeProject != null) {
            GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeProject.name, style = MaterialTheme.typography.titleLarge)
                        Text("Folder: ${activeProject.folder} • ${entries.size} entries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteProject(activeProject.id) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete project", tint = semantic.critical)
                    }
                }
                if (activeProject.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(activeProject.description, style = MaterialTheme.typography.bodySmall)
                }
                if (activeProject.collaborators.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Team Roles: ${activeProject.collaborators.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                }
            }

            PillSelectorRow(
                options = tabOptions,
                selected = subTab,
                onSelect = { subTab = it },
                tagPrefix = "ws_tab"
            )

            Button(
                onClick = {
                    editingEntry = null
                    entryTitle = ""
                    entryContent = if (subTab == "Datasets") {
                        "Time_h,Condition_A,Condition_B\n0,1.0,1.1\n6,4.2,2.1\n12,8.5,3.4\n24,5.9,2.8"
                    } else ""
                    entryStatus = if (subTab == "Hypotheses") "Hypothesis" else "active"
                    showAddEntry = true
                },
                modifier = Modifier.fillMaxWidth().testTag("ws_add_entry_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add $subTab Entry")
            }

            if (filteredEntries.isEmpty()) {
                GlassCard {
                    Text("No $subTab entries in '${activeProject.name}'.", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                }
            } else {
                filteredEntries.forEach { entry ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            EvidencePill(entry.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(entry.content, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (entry.entryType == "dataset") {
                                OutlinedButton(onClick = { onSendToAnalytics(entry.content) }) {
                                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Analyze CSV")
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            if (entry.history.isNotEmpty()) {
                                IconButton(onClick = { historyEntry = entry }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.History, contentDescription = "View version history", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            IconButton(
                                onClick = {
                                    editingEntry = entry
                                    entryTitle = entry.title
                                    entryContent = entry.content
                                    entryStatus = entry.status
                                    showAddEntry = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit entry", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { onDeleteEntry(activeProject.id, entry.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = semantic.critical)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showCreateProj) {
        AlertDialog(
            onDismissRequest = { showCreateProj = false },
            title = { Text("Create Research Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ws_proj_name_input")
                    )
                    OutlinedTextField(
                        value = newProjFolder,
                        onValueChange = { newProjFolder = it },
                        label = { Text("Folder / Lab Group") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjDesc,
                        onValueChange = { newProjDesc = it },
                        label = { Text("Scope & Objective") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjCollabs,
                        onValueChange = { newProjCollabs = it },
                        label = { Text("Collaborators & Roles") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val collabs = newProjCollabs.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreateProject(newProjName.ifBlank { "Immunology Study" }, newProjDesc, newProjFolder, collabs)
                    newProjName = ""
                    newProjDesc = ""
                    showCreateProj = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateProj = false }) { Text("Cancel") } }
        )
    }

    if (showAddEntry && activeProject != null) {
        AlertDialog(
            onDismissRequest = { showAddEntry = false },
            title = { Text(if (editingEntry == null) "New $subTab Entry" else "Edit Entry (Saves Version)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryTitle,
                        onValueChange = { entryTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ws_entry_title_input")
                    )
                    OutlinedTextField(
                        value = entryStatus,
                        onValueChange = { entryStatus = it },
                        label = { Text("Status / Label (e.g. active, Hypothesis, Tested)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entryContent,
                        onValueChange = { entryContent = it },
                        label = { Text("Markdown Content / CSV / Citation") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("ws_entry_content_input")
                    )
                    if (bookmarks.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            val b = bookmarks.first()
                            entryContent += "\n- Citation: ${b.title} (${b.subtitle ?: b.itemType})"
                        }) {
                            Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Insert Latest Bookmark")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = entryTitle.ifBlank { "Untitled Entry" }
                    val cur = editingEntry
                    if (cur == null) {
                        onAddEntry(activeProject.id, entryTypeForTab, t, entryContent, entryStatus.ifBlank { "active" }, null)
                    } else {
                        onUpdateEntry(activeProject.id, cur, t, entryContent, entryStatus.ifBlank { "active" })
                    }
                    showAddEntry = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddEntry = false }) { Text("Cancel") } }
        )
    }

    historyEntry?.let { he ->
        AlertDialog(
            onDismissRequest = { historyEntry = null },
            title = { Text("Revision History: ${he.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    he.history.forEachIndexed { idx, snap ->
                        GlassCard {
                            Text("Previous Snapshot #${idx + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(snap, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyEntry = null }) { Text("Close") } }
        )
    }
}
