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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.core.firebase.signOutUser
import com.example.data.model.AcademyProgress
import com.example.data.model.AuditLogEntry
import com.example.data.model.BookmarkItem
import com.example.data.model.CustomContent
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UserProfile
import com.example.data.network.ApiHealthStatus
import com.example.data.network.ScientificApiClient
import com.example.ui.components.AppRoute
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.DeveloperCreditsFooter
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper
import kotlinx.coroutines.launch

@Composable
fun ProfileAdminScreen(
    profile: UserProfile?,
    bookmarks: List<BookmarkItem>,
    simulations: List<SavedSimulation>,
    projects: List<ResearchProject>,
    progress: List<AcademyProgress>,
    customContent: List<CustomContent>,
    auditLogs: List<AuditLogEntry>,
    backupJsonBuilder: () -> String,
    onUpdatePreferences: (userCategory: String?, reduceMotion: Boolean?, hiddenModules: List<String>?, displayName: String?) -> Unit,
    onAddCustomContent: (CustomContent) -> Unit,
    onDeleteCustomContent: (String) -> Unit,
    onToast: (String) -> Unit,
    isSignedIn: Boolean = false,
    localProjectCount: Int = 0,
    onRequestSignIn: () -> Unit = {},
    onSyncLocalProjects: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val roles = listOf("Student", "Researcher", "Lecturer", "Laboratory scientist", "Research administrator", "Platform administrator")
    var nameInput by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Researcher") }
    var apiHealth by remember { mutableStateOf<List<ApiHealthStatus>>(emptyList()) }
    var probingApis by remember { mutableStateOf(false) }

    // Custom quiz/flashcard creator
    var ccType by remember { mutableStateOf("quiz") }
    var ccPrompt by remember { mutableStateOf("") }
    var ccAnswer by remember { mutableStateOf("") }
    var ccOpt1 by remember { mutableStateOf("True") }
    var ccOpt2 by remember { mutableStateOf("False") }

    val isAdminAccount = (profile?.email == "abuyabyron@gmail.com") || (profile?.userCategory == "Platform administrator")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Profile & System Admin",
            subtitle = "Google account, accessibility, backups, feature flags & live API telemetry.",
            badge = if (isAdminAccount) "Admin Console" else "User Workspace"
        )

        // Account & Role Card
        GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isSignedIn) (profile?.email ?: "Signed in with Google") else "Guest Mode (Local SQLite Storage)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Role: ${profile?.userCategory ?: "Researcher"} • ${if (isSignedIn) "Cloud Synced" else "Saved Locally"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                EvidencePill(if (!isSignedIn) "Local Mode" else if (isAdminAccount) "Platform Admin" else "Member")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("admin_display_name_input")
                )
                Button(onClick = { onUpdatePreferences(null, null, null, nameInput) }, modifier = Modifier.testTag("admin_save_name_btn")) {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("User Category Role:", style = MaterialTheme.typography.labelMedium)
            PillSelectorRow(
                options = roles,
                selected = profile?.userCategory ?: "Researcher",
                onSelect = { role -> onUpdatePreferences(role, null, null, null) },
                tagPrefix = "admin_role"
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce Motion (Accessibility)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = profile?.reduceMotion ?: false,
                    onCheckedChange = { onUpdatePreferences(null, it, null, null) },
                    modifier = Modifier.testTag("admin_reduce_motion_switch")
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        ExportHelper.shareTextFile(context, "immunoverse_backup.json", "application/json", backupJsonBuilder(), "Export Workspace JSON")
                            .onSuccess { onToast("Exported $it") }
                    },
                    modifier = Modifier.weight(1f).testTag("admin_backup_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("JSON Backup")
                }
                if (isSignedIn) {
                    Button(
                        onClick = { signOutUser(credentialManager, scope) },
                        colors = ButtonDefaults.buttonColors(containerColor = semantic.critical),
                        modifier = Modifier.weight(1f).testTag("sign_out_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sign Out")
                    }
                } else {
                    Button(
                        onClick = onRequestSignIn,
                        modifier = Modifier.weight(1f).testTag("profile_sign_in_btn")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sign In (Opt.)")
                    }
                }
            }
            if (isSignedIn && localProjectCount > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSyncLocalProjects,
                    modifier = Modifier.fillMaxWidth().testTag("profile_sync_local_btn")
                ) {
                    Text("Upload $localProjectCount Local Project(s) to Cloud")
                }
            }
        }

        // Workspace Usage Chart
        BarChartCanvas(
            labels = listOf("Bookmarks", "SimRuns", "Projects", "Lessons", "Custom", "Logs"),
            values = listOf(
                bookmarks.size.toDouble(),
                simulations.size.toDouble(),
                projects.size.toDouble(),
                progress.sumOf { it.completedLessons.size }.toDouble(),
                customContent.size.toDouble(),
                auditLogs.size.toDouble()
            ),
            title = if (isSignedIn) "Cloud Workspace Telemetry Counts" else "Local Device Workspace Counts",
            height = 160.dp
        )

        // Live API Health Probe
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Live Biomedical API Status", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        scope.launch {
                            probingApis = true
                            apiHealth = ScientificApiClient.checkAllApis()
                            probingApis = false
                        }
                    },
                    enabled = !probingApis,
                    modifier = Modifier.testTag("admin_probe_apis_btn")
                ) {
                    if (probingApis) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Check APIs")
                }
            }
            if (apiHealth.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                apiHealth.forEach { st ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${st.name} (${st.latencyMs}ms)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (st.ok) "HTTP ${st.code} OK" else "ERR (${st.code})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (st.ok) semantic.established else semantic.critical
                        )
                    }
                }
            }
        }

        // Feature Flags / Module Visibility
        GlassCard {
            Text("Module Feature Visibility Flags", style = MaterialTheme.typography.titleMedium)
            Text("Hide secondary modules from the Dashboard quick grid without deleting data.", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            Spacer(Modifier.height(6.dp))
            val hidden = profile?.hiddenModules ?: emptyList()
            val toggleable = AppRoute.entries.filter { it != AppRoute.DASHBOARD && it != AppRoute.PROFILE_ADMIN }
            toggleable.forEach { route ->
                val isVisible = route.route !in hidden
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(route.label, style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { show ->
                            val nextHidden = if (show) hidden - route.route else (hidden + route.route).distinct()
                            onUpdatePreferences(null, null, nextHidden, null)
                        }
                    )
                }
            }
        }

        // Custom Quiz / Flashcard Creator
        GlassCard {
            Text("Custom Academy Content Manager", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            PillSelectorRow(
                options = listOf("quiz", "flashcard"),
                selected = ccType,
                onSelect = { ccType = it },
                tagPrefix = "admin_cctype"
            )
            OutlinedTextField(
                value = ccPrompt,
                onValueChange = { ccPrompt = it },
                label = { Text(if (ccType == "quiz") "Question Prompt" else "Flashcard Front") },
                modifier = Modifier.fillMaxWidth().testTag("admin_cc_prompt")
            )
            if (ccType == "quiz") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ccOpt1, onValueChange = { ccOpt1 = it }, label = { Text("Correct Option") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ccOpt2, onValueChange = { ccOpt2 = it }, label = { Text("Distractor") }, modifier = Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                value = ccAnswer,
                onValueChange = { ccAnswer = it },
                label = { Text(if (ccType == "quiz") "Explanation" else "Flashcard Back") },
                modifier = Modifier.fillMaxWidth().testTag("admin_cc_answer")
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    if (ccPrompt.isNotBlank()) {
                        onAddCustomContent(
                            CustomContent(
                                contentType = ccType,
                                prompt = ccPrompt.trim(),
                                explanation = ccAnswer.trim(),
                                options = if (ccType == "quiz") listOf(ccOpt1.ifBlank { "Option A" }, ccOpt2.ifBlank { "Option B" }) else emptyList(),
                                correctIndex = 0
                            )
                        )
                        ccPrompt = ""
                        ccAnswer = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("admin_cc_add_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Publish to Academy")
            }
            customContent.forEach { cc ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("[${cc.contentType}] ${cc.prompt}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteCustomContent(cc.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete custom item", tint = semantic.critical)
                    }
                }
            }
        }

        // Audit Logs Viewer
        GlassCard {
            Text("System Audit Log (Latest ${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (auditLogs.isEmpty()) {
                Text("No audit entries recorded yet.", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
            } else {
                auditLogs.take(12).forEach { log ->
                    Text("[${log.module}] ${log.message}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                }
            }
        }
        DeveloperCreditsFooter(onCopied = onToast)
        Spacer(Modifier.height(24.dp))
    }
}
