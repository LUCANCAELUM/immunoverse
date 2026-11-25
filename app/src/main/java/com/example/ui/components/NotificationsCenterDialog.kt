package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLogEntry
import com.example.domain.ResearchAlertItem
import com.example.domain.ResearchAlertsEngine
import com.example.ui.theme.LocalImmunoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsCenterDialog(
    alerts: List<ResearchAlertItem>,
    readAlertIds: Set<String>,
    savedInterests: Set<String>,
    effectiveInterests: List<String>,
    logs: List<AuditLogEntry>,
    onToggleInterest: (String) -> Unit,
    onRefreshAlerts: () -> Unit,
    onMarkAlertRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onSavePaperBookmark: (ResearchAlertItem) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalImmunoColors.current
    var tab by remember { mutableStateOf("Alerts") }
    var customTopicInput by remember { mutableStateOf("") }
    val unreadCount = alerts.count { it.id !in readAlertIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Research Alerts & Breakthroughs", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$unreadCount unread • ${effectiveInterests.size} active research interests",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.mutedText
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillSelectorRow(
                    options = listOf("Alerts ($unreadCount)", "Saved Interests", "Activity Logs"),
                    selected = when {
                        tab.startsWith("Alerts") -> "Alerts ($unreadCount)"
                        else -> tab
                    },
                    onSelect = { sel ->
                        tab = if (sel.startsWith("Alerts")) "Alerts" else sel
                    },
                    tagPrefix = "notif_tab"
                )

                when (tab) {
                    "Alerts" -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onRefreshAlerts,
                                modifier = Modifier.weight(1f).testTag("refresh_alerts_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Check Live Papers", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onMarkAllRead,
                                modifier = Modifier.weight(1f).testTag("mark_all_alerts_read_btn")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mark Read", maxLines = 1)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(alerts, key = { it.id }) { alert ->
                                val isUnread = alert.id !in readAlertIds
                                GlassCard(
                                    onClick = {
                                        onMarkAlertRead(alert.id)
                                        onNavigate(alert.route)
                                        onDismiss()
                                    },
                                    accentColor = if (isUnread) MaterialTheme.colorScheme.primary else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUnread) "NEW • ${alert.source}" else alert.source,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isUnread) MaterialTheme.colorScheme.primary else semantic.mutedText
                                        )
                                        EvidencePill(alert.matchedInterest.take(22))
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(alert.title, style = MaterialTheme.typography.titleSmall)
                                    Text(alert.summary, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                onMarkAlertRead(alert.id)
                                                onSavePaperBookmark(alert)
                                            }
                                        ) {
                                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Save Topic", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = {
                                                onMarkAlertRead(alert.id)
                                                onNavigate(alert.route)
                                                onDismiss()
                                            }
                                        ) {
                                            Text("Explore →", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Saved Interests" -> {
                        Text(
                            "Follow topics to receive tailored Europe PMC preprint & breakthrough notifications (also auto-synced with your bookmarks & projects).",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.mutedText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTopicInput,
                                onValueChange = { customTopicInput = it },
                                label = { Text("Add research topic (e.g. CRISPR, Ebola)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("custom_interest_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    val clean = customTopicInput.trim()
                                    if (clean.isNotBlank()) {
                                        if (clean !in savedInterests) onToggleInterest(clean)
                                        customTopicInput = ""
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("add_interest_btn")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add interest", modifier = Modifier.size(18.dp))
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            val allChoices = (ResearchAlertsEngine.PRESET_INTEREST_TOPICS + savedInterests).distinct()
                            allChoices.forEach { topic ->
                                val selected = topic in savedInterests
                                FilterChip(
                                    selected = selected,
                                    onClick = { onToggleInterest(topic) },
                                    label = { Text(topic, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    else -> {
                        if (logs.isEmpty()) {
                            Text("No recorded activity yet. Explore modules, save simulations, or create workspace entries.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(logs.take(15)) { entry ->
                                    GlassCard {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(entry.module, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                            Text(entry.level.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (entry.level == "error") semantic.critical else semantic.established)
                                        }
                                        Text(entry.message, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
