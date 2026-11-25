package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.data.network.LiteraturePaper
import com.example.data.network.ScientificApiClient
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper
import kotlinx.coroutines.launch

@Composable
fun LiteratureScreen(
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?, String?) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()

    val sources = listOf("Europe PMC", "PubMed", "Crossref", "OpenAlex", "Semantic Scholar", "Preprints")
    var selectedSource by remember { mutableStateOf("Europe PMC") }
    var query by remember { mutableStateOf("HIV latent reservoir bNAbs") }
    var yearFrom by remember { mutableStateOf("2022") }
    var papers by remember { mutableStateOf<List<LiteraturePaper>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var compareIds by remember { mutableStateOf(setOf<String>()) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val topicChips = listOf("CAR-T exhaustion", "Checkpoint PD-1", "mRNA vaccine Tfh", "CRISPR SCID", "NLRP3 inflammasome")

    fun runSearch(q: String = query) {
        if (q.isBlank()) return
        scope.launch {
            isLoading = true
            errorMsg = null
            val yr = yearFrom.toIntOrNull()
            val res = when (selectedSource) {
                "Europe PMC" -> ScientificApiClient.searchEuropePmc(q, preprintsOnly = false, yearFrom = yr)
                "Preprints" -> ScientificApiClient.searchEuropePmc(q, preprintsOnly = true, yearFrom = yr)
                "PubMed" -> ScientificApiClient.searchPubMed(q)
                "Crossref" -> ScientificApiClient.searchCrossref(q)
                "OpenAlex" -> ScientificApiClient.searchOpenAlex(q)
                else -> ScientificApiClient.searchSemanticScholar(q)
            }
            res.onSuccess { papers = it }
                .onFailure { errorMsg = it.localizedMessage ?: "Search failed" }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Scientific Literature Hub",
            subtitle = "Real-time Europe PMC, PubMed, Crossref, OpenAlex, Semantic Scholar & Preprints.",
            badge = "Live APIs"
        )

        PillSelectorRow(
            options = sources,
            selected = selectedSource,
            onSelect = { selectedSource = it },
            tagPrefix = "lit_src"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Query") },
                singleLine = true,
                modifier = Modifier.weight(2f).testTag("lit_query_input")
            )
            OutlinedTextField(
                value = yearFrom,
                onValueChange = { yearFrom = it.filter(Char::isDigit).take(4) },
                label = { Text("Year ≥") },
                singleLine = true,
                modifier = Modifier.weight(0.8f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { runSearch() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f).testTag("lit_search_btn")
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isLoading) "Searching..." else "Search $selectedSource")
            }
            if (papers.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val bib = papers.joinToString("\n\n") { p ->
                            val key = p.id.replace(Regex("[^A-Za-z0-9]"), "")
                            """@article{$key,
  title = {${p.title}},
  author = {${p.authors}},
  journal = {${p.journal}},
  year = {${p.year}},
  doi = {${p.doi ?: ""}}
}"""
                        }
                        ExportHelper.shareTextFile(context, "immunoverse_refs.bib", "text/plain", bib, "Export BibTeX")
                            .onSuccess { onToast("Exported $it") }
                            .onFailure { onToast("Export failed: ${it.localizedMessage}") }
                    },
                    modifier = Modifier.testTag("lit_bibtex_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("BibTeX")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            topicChips.take(3).forEach { topic ->
                AssistChip(
                    onClick = { query = topic; runSearch(topic) },
                    label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isLoading) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Fetching peer-reviewed & preprint metadata from $selectedSource...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (errorMsg != null) {
            BannerDisclaimer("API Notice ($selectedSource): $errorMsg", isSimulation = false)
        }

        // Extractive summary + comparison card when 2 papers selected
        val cmpPapers = papers.filter { it.id in compareIds }
        if (cmpPapers.size == 2) {
            GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                Text("Side-by-Side Comparison & Extractive Summary", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                cmpPapers.forEachIndexed { idx, cp ->
                    Text("${idx + 1}. ${cp.title} (${cp.year}) — ${cp.journal}", style = MaterialTheme.typography.labelMedium)
                    Text("   Snippet: ${cp.abstractText.take(180)}...", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                }
            }
        }

        papers.forEach { p ->
            val saved = bookmarks.any { it.itemId == p.id }
            val inCmp = p.id in compareIds
            val expanded = p.id == expandedId
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("${p.authors} • ${p.journal} (${p.year})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            val paperMeta = listOfNotNull(
                                p.doi?.let { "DOI: $it" },
                                p.url
                            ).joinToString(" • ").ifBlank { p.abstractText.take(160) }
                            onToggleBookmark("paper", p.id, p.title, "${p.journal} (${p.year})", paperMeta)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save paper bookmark",
                            tint = if (saved) MaterialTheme.colorScheme.primary else semantic.mutedText
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    EvidencePill(p.source)
                    p.doi?.let { Text("DOI: $it", style = MaterialTheme.typography.labelSmall, color = semantic.established) }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (expanded) p.abstractText else p.abstractText.take(160) + if (p.abstractText.length > 160) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { expandedId = if (expanded) null else p.id }
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        compareIds = if (inCmp) compareIds - p.id else (compareIds + p.id).take(2).toSet()
                    }) {
                        Text(if (inCmp) "Selected ✓" else "Compare")
                    }
                    p.url?.let { u ->
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
                        }) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
