package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repo.AiExecutionMode
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.ui.viewmodel.ImmunoAiViewModel

@Composable
fun ImmunoAiScreen(
    aiVm: ImmunoAiViewModel,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    val messages by aiVm.messages.collectAsStateWithLifecycle()
    val uploadedDocs by aiVm.uploadedDocs.collectAsStateWithLifecycle()
    val isGenerating by aiVm.isGenerating.collectAsStateWithLifecycle()
    val useLiveCitations by aiVm.useLiveCitations.collectAsStateWithLifecycle()
    val executionMode by aiVm.executionMode.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val expandedTraces = remember { mutableStateMapOf<String, Boolean>() }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                var displayName = "uploaded_document.txt"
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && nameIdx >= 0) displayName = c.getString(nameIdx)
                }
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (content.isNotBlank()) {
                    aiVm.addUploadedDocument(displayName, content)
                    onToast("Indexed '$displayName' into local RAG")
                } else {
                    onToast("Selected file was empty or unreadable")
                }
            }.onFailure { onToast("File read error: ${it.localizedMessage}") }
        }
    }

    val quickPrompts = listOf(
        "Explain V(D)J recombination across 4 levels",
        "Generate concise study notes on JAK-STAT",
        "Quiz me with 3 questions on HIV latency reservoirs",
        "Compare SARS-CoV-2 Delta vs Omicron JN.1 immune escape",
        "Compare Innate TLR4 vs Adaptive BCR recognition"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModuleHeader(
            title = "Immuno AI",
            subtitle = "Parallel multi-model Gemini + live Web, Wikipedia & Europe PMC search.",
            badge = executionMode.badge
        )

        BannerDisclaimer(stringResource(R.string.ai_disclaimer), isSimulation = false)

        PillSelectorRow(
            options = AiExecutionMode.entries.map { it.label },
            selected = executionMode.label,
            onSelect = { lbl ->
                aiVm.setExecutionMode(AiExecutionMode.entries.first { it.label == lbl })
            },
            tagPrefix = "ai_mode"
        )

        // Controls row: SAF Upload + Web/PMC Search Switch + Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { docPicker.launch(arrayOf("text/*", "application/octet-stream")) },
                modifier = Modifier.testTag("ai_upload_doc_btn")
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Upload File")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Web+PMC", style = MaterialTheme.typography.labelSmall)
                Switch(checked = useLiveCitations, onCheckedChange = { aiVm.setLiveCitations(it) }, modifier = Modifier.testTag("ai_live_cite_switch"))
                IconButton(onClick = { aiVm.clearChat() }, modifier = Modifier.size(40.dp).testTag("ai_clear_btn")) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear conversation", tint = semantic.mutedText)
                }
            }
        }

        if (uploadedDocs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uploadedDocs.forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { aiVm.removeUploadedDocument(doc.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(doc.name, style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${doc.name}", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Quick-Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPrompts.forEach { qp ->
                AssistChip(
                    onClick = { aiVm.sendQuestion(qp) },
                    label = { Text(qp.take(28), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Chat Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { turn ->
                if (turn.isUser) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                        ) {
                            Text(
                                text = turn.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    GlassCard(accentColor = if (turn.error != null) semantic.illustrative else MaterialTheme.colorScheme.primary) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Immuno AI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            EvidencePill(turn.activeModelLabel.ifBlank { "Multi-Model" })
                        }
                        Spacer(Modifier.height(6.dp))
                        if (turn.text.isBlank() && isGenerating) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Racing Flash-Lite + 3.8-Flash & live Web/PMC search...", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text(turn.text, style = MaterialTheme.typography.bodySmall)
                        }

                        if (turn.modelTraces.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            val showBreakdown = expandedTraces[turn.id] == true
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { expandedTraces[turn.id] = !showBreakdown }) {
                                    Text(
                                        if (showBreakdown) "Hide Model Comparison (${turn.modelTraces.size})"
                                        else "Model Comparison (${turn.modelTraces.size} models)"
                                    )
                                }
                                turn.googleSearchUrl?.let { gUrl ->
                                    TextButton(onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(gUrl))) }
                                    }) {
                                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open Google Search", modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Google Search")
                                    }
                                }
                            }
                            if (showBreakdown) {
                                turn.modelTraces.forEach { tr ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "${tr.modelLabel} • ${tr.status} (${tr.latencyMs} ms)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (tr.text.isNotBlank()) {
                                                Text(tr.text.take(420), style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (turn.webHits.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Live Web & Encyclopedia Search Hits:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            turn.webHits.forEach { w ->
                                Text(
                                    text = "• [${w.engine}] ${w.title} — ${w.snippet.take(120)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(w.url))) }
                                    }
                                )
                            }
                        }

                        if (turn.ragChunks.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Local KB / Document Sources:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            turn.ragChunks.forEach { ch ->
                                Text("• [${ch.source}] ${ch.title}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                            }
                        }

                        if (turn.livePapers.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Live Europe PMC / OpenAlex Citations:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = semantic.established)
                            turn.livePapers.forEach { p ->
                                Text(
                                    text = "• ${p.title} (${p.year}) ${p.doi?.let { "DOI:$it" } ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        p.url?.let { u ->
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
                                        }
                                    }
                                )
                            }
                        }

                        if (turn.verifiedDois.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Crossref DOI Verification:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            turn.verifiedDois.forEach { d ->
                                Text(
                                    text = "• ${d.doi}: ${if (d.verified) "Verified ✓ (${d.title ?: "Crossref record"})" else "Unverified DOI — check source"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (d.verified) semantic.established else semantic.critical
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask Immuno AI (multi-model + web search)...") },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .testTag("ai_prompt_input")
            )
            if (isGenerating) {
                IconButton(
                    onClick = { aiVm.stopGeneration() },
                    modifier = Modifier.size(48.dp).testTag("ai_stop_btn")
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop AI generation", tint = semantic.critical)
                }
            }
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        val q = input
                        input = ""
                        aiVm.sendQuestion(q)
                    }
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.size(48.dp).testTag("ai_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
