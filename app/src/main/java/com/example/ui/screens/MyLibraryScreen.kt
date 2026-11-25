package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.domain.LibraryCatalogHelper
import com.example.domain.LibraryCategory
import com.example.ui.components.AppRoute
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyLibraryScreen(
    bookmarks: List<BookmarkItem>,
    savedInterests: Set<String>,
    onToggleBookmark: (String, String, String, String?, String?) -> Unit,
    onToggleInterest: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(LibraryCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddTopic by remember { mutableStateOf(false) }
    var customTopicTitle by remember { mutableStateOf("") }
    var customTopicNotes by remember { mutableStateOf("") }
    var alsoFollowAlerts by remember { mutableStateOf(true) }

    val counts = remember(bookmarks) { LibraryCatalogHelper.countByCategory(bookmarks) }
    val filtered = remember(bookmarks, selectedCategory, searchQuery) {
        LibraryCatalogHelper.filter(bookmarks, selectedCategory, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "My Library",
            subtitle = "Saved scientific articles, pathogen profiles, viral spikes & immunology research topics.",
            badge = "${bookmarks.size} Saved"
        )

        // Category summary strip
        GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CollectionsBookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text("Personal Immunology Collection", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${counts[LibraryCategory.ARTICLES] ?: 0} Articles • ${counts[LibraryCategory.PATHOGENS] ?: 0} Pathogens • ${counts[LibraryCategory.TOPICS] ?: 0} Topics",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.mutedText
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showAddTopic = !showAddTopic },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("library_toggle_add_topic_btn")
                ) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showAddTopic) "Close" else "Save Topic")
                }
            }
        }

        if (showAddTopic) {
            GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                Text("Save a Research Topic to My Library", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customTopicTitle,
                    onValueChange = { customTopicTitle = it },
                    label = { Text("Research Topic Title (e.g. Broadly Neutralizing HIV bNAbs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("library_custom_topic_input")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customTopicNotes,
                    onValueChange = { customTopicNotes = it },
                    label = { Text("Optional focus notes or key targets") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = alsoFollowAlerts, onCheckedChange = { alsoFollowAlerts = it })
                    Text("Also track new literature alerts for this topic", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val clean = customTopicTitle.trim()
                        if (clean.isNotBlank()) {
                            val id = "topic_${clean.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(40)}"
                            val notes = customTopicNotes.trim().ifBlank { "Saved immunology research focus" }
                            if (bookmarks.none { it.itemId == id }) {
                                onToggleBookmark("topic", id, clean, "Research Topic", notes)
                            }
                            if (alsoFollowAlerts && clean !in savedInterests) {
                                onToggleInterest(clean)
                            }
                            customTopicTitle = ""
                            customTopicNotes = ""
                        }
                    },
                    enabled = customTopicTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("library_save_topic_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Research Topic")
                }

                Spacer(Modifier.height(8.dp))
                Text("Suggested Quick Topics", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LibraryCatalogHelper.SUGGESTED_RESEARCH_TOPICS.forEach { (topicTitle, desc) ->
                        val topicId = "topic_${topicTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(40)}"
                        val isSaved = bookmarks.any { it.itemId == topicId }
                        FilterChip(
                            selected = isSaved,
                            onClick = {
                                onToggleBookmark("topic", topicId, topicTitle, "Research Topic", desc)
                            },
                            label = { Text(topicTitle, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Category Filter Pills + Library Search
        val pillLabels = LibraryCategory.entries.map { "${it.label} (${counts[it] ?: 0})" }
        val activePill = "${selectedCategory.label} (${counts[selectedCategory] ?: 0})"
        PillSelectorRow(
            options = pillLabels,
            selected = activePill,
            onSelect = { lbl ->
                selectedCategory = LibraryCategory.entries.firstOrNull { lbl.startsWith(it.label) } ?: LibraryCategory.ALL
            },
            tagPrefix = "library_cat"
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filter saved items by title, pathogen, or DOI") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear library search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("library_search_input")
        )

        if (filtered.isEmpty()) {
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CollectionsBookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (bookmarks.isEmpty()) "Your Library is Empty" else "No Saved Items Match Filter",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (bookmarks.isEmpty()) {
                            "Bookmark peer-reviewed papers in Literature Hub, pathogens or viral spikes in Pathogen Lab, or concepts in the Glossary to access them here offline or across sessions."
                        } else {
                            "Try clearing your search filter or switching category tabs."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigate(AppRoute.LITERATURE) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Articles", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AppRoute.PATHOGEN_LAB) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Coronavirus, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pathogens", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AppRoute.GLOSSARY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Topics", maxLines = 1)
                        }
                    }
                }
            }
        } else {
            filtered.forEach { item ->
                val targetRoute = LibraryCatalogHelper.resolveRoute(item)
                val badgeLabel = LibraryCatalogHelper.displayBadge(item.itemType)
                val extUrl = LibraryCatalogHelper.extractHttpUrl(item.meta)

                GlassCard(
                    onClick = { onNavigate(targetRoute) },
                    modifier = Modifier.testTag("library_card_${item.itemId}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EvidencePill(badgeLabel)
                                item.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                onToggleBookmark(item.itemType, item.itemId, item.title, item.subtitle, item.meta)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("library_remove_${item.itemId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove from My Library",
                                tint = semantic.critical
                            )
                        }
                    }

                    item.meta?.takeIf { it.isNotBlank() }?.let { details ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onNavigate(targetRoute) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open in ${targetRoute.shortLabel}", style = MaterialTheme.typography.labelSmall)
                        }
                        if (extUrl != null) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(extUrl))) }
                                },
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Source Link", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
