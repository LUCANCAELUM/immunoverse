package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.PathwayInfo
import com.example.data.kb.PathwayNode
import com.example.data.model.BookmarkItem
import com.example.domain.NodePerturbation
import com.example.domain.SignalingEngine
import com.example.domain.SignalingSimulationResult
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.ChartSeries
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SignalingScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?) -> Unit,
    onRecordRecent: (String) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var pathwayId by remember { mutableStateOf(ImmunologyKb.PATHWAYS.first().id) }
    var cellId by remember { mutableStateOf(SignalingEngine.CELL_CONTEXTS.first().id) }
    var conditionId by remember { mutableStateOf(SignalingEngine.CONDITIONS[1].id) } // Viral IFN default
    var customCsvText by remember { mutableStateOf(SignalingEngine.DEMO_BIO_CSV) }
    var customFileLabel by remember { mutableStateOf<String?>(null) }
    var showCsvEditor by remember { mutableStateOf(false) }

    var stimulusDose by remember { mutableFloatStateOf(1.0f) }
    var feedbackStrength by remember { mutableFloatStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(!reduceMotion) }
    var perturbations by remember { mutableStateOf<Map<String, NodePerturbation>>(emptyMap()) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }

    val pathway = remember(pathwayId) { ImmunologyKb.PATHWAYS.first { it.id == pathwayId } }
    val cellContext = remember(cellId) { SignalingEngine.CELL_CONTEXTS.first { it.id == cellId } }
    val condition = remember(conditionId) { SignalingEngine.CONDITIONS.first { it.id == conditionId } }
    val csvMap = remember(customCsvText) { SignalingEngine.parseBiologicalCsv(customCsvText) }

    val simResult = remember(pathway, cellContext, condition, csvMap, stimulusDose, feedbackStrength, perturbations) {
        SignalingEngine.simulate(
            pathway = pathway,
            cellContext = cellContext,
            condition = condition,
            customCsvMap = csvMap,
            stimulusDose = stimulusDose.toDouble(),
            feedbackStrength = feedbackStrength.toDouble(),
            perturbations = perturbations
        )
    }

    val selectedNode = remember(pathway, selectedNodeId) {
        pathway.nodes.firstOrNull { it.id == selectedNodeId } ?: pathway.nodes.firstOrNull()
    }
    val isBookmarked = bookmarks.any { it.itemType == "pathway" && it.itemId == pathway.id }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.onSuccess { text ->
                if (text.isNotBlank()) {
                    customCsvText = text.take(25_000)
                    customFileLabel = uri.lastPathSegment ?: "custom_expression.csv"
                    conditionId = "custom_csv"
                    onToast("Loaded biological CSV (${SignalingEngine.parseBiologicalCsv(customCsvText).size} features)")
                } else {
                    onToast("Selected CSV file was empty")
                }
            }.onFailure { err ->
                onToast("CSV load failed: ${err.localizedMessage}")
            }
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
            title = "Dynamic Signaling Pathways",
            subtitle = "Interactive Canvas cascade driven by cell lineage, condition & biological CSV expression.",
            badge = "Dynamic Canvas",
            isBookmarked = isBookmarked,
            onToggleBookmark = {
                onToggleBookmark("pathway", pathway.id, pathway.name, "${cellContext.name} • ${condition.name}")
            }
        )

        BannerDisclaimer(
            text = "Educational signal transduction model combining curated pathways with user-selected cell contexts, drugs & expression fold-changes.",
            isSimulation = true
        )

        // 1. Pathway & Immune Cell Selectors
        Text("Signaling Network", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        PillSelectorRow(
            options = ImmunologyKb.PATHWAYS.map { it.name },
            selected = pathway.name,
            onSelect = { name ->
                val found = ImmunologyKb.PATHWAYS.first { it.name == name }
                pathwayId = found.id
                perturbations = emptyMap()
                selectedNodeId = found.nodes.firstOrNull()?.id
                onRecordRecent(found.name)
            },
            tagPrefix = "sig_path"
        )

        Text("Immune Cell Context", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        PillSelectorRow(
            options = SignalingEngine.CELL_CONTEXTS.map { it.name },
            selected = cellContext.name,
            onSelect = { name -> cellId = SignalingEngine.CELL_CONTEXTS.first { it.name == name }.id },
            tagPrefix = "sig_cell"
        )

        Text("Biological Condition / Perturbation Dataset", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        PillSelectorRow(
            options = SignalingEngine.CONDITIONS.map { it.name },
            selected = condition.name,
            onSelect = { name -> conditionId = SignalingEngine.CONDITIONS.first { it.name == name }.id },
            tagPrefix = "sig_cond"
        )

        // Custom Biological Data Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { csvLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                modifier = Modifier.weight(1f).testTag("sig_import_csv_btn")
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(customFileLabel?.take(14) ?: "Load Bio CSV")
            }
            OutlinedButton(
                onClick = { showCsvEditor = !showCsvEditor },
                modifier = Modifier.weight(1f).testTag("sig_edit_csv_btn")
            ) {
                Text(if (showCsvEditor) "Hide Data CSV" else "Edit Bio CSV (${csvMap.size})")
            }
        }

        if (showCsvEditor) {
            GlassCard {
                Text("Biological Expression Fold-Change Data (node_or_gene,fold)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = customCsvText,
                    onValueChange = {
                        customCsvText = it
                        if (conditionId != "custom_csv") conditionId = "custom_csv"
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("sig_csv_input")
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        customCsvText = SignalingEngine.DEMO_BIO_CSV
                        conditionId = "custom_csv"
                    }) { Text("Reset Sample Bio CSV") }
                    TextButton(onClick = { conditionId = "custom_csv" }) { Text("Apply to Canvas") }
                }
            }
        }

        // 2. Hero Dynamic Compose Canvas
        DynamicSignalingCanvasCard(
            pathway = pathway,
            simResult = simResult,
            perturbations = perturbations,
            selectedNodeId = selectedNode?.id,
            isPlaying = isPlaying && !reduceMotion,
            onTogglePlay = { isPlaying = !isPlaying },
            onSelectNode = { nid -> selectedNodeId = nid },
            onOpenFullscreen = { showFullscreen = true },
            height = 310.dp
        )

        // 3. Biological Stimulus & Feedback Sliders
        GlassCard {
            Text(simResult.summaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("${cellContext.receptorProfile} • ${condition.summary}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            Spacer(Modifier.height(8.dp))
            Text("Ligand / Stimulus Dose: ${(stimulusDose * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = stimulusDose,
                onValueChange = { stimulusDose = it },
                valueRange = 0f..2f,
                modifier = Modifier.testTag("sig_stimulus_slider")
            )
            Text("Negative Feedback Gain (SOCS / PTEN): ${(feedbackStrength * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = feedbackStrength,
                onValueChange = { feedbackStrength = it },
                valueRange = 0f..2f,
                modifier = Modifier.testTag("sig_feedback_slider")
            )
            if (perturbations.isNotEmpty()) {
                OutlinedButton(
                    onClick = { perturbations = emptyMap() },
                    modifier = Modifier.testTag("sig_reset_pert_btn")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Node Perturbations (${perturbations.size})")
                }
            }
        }

        // 4. Selected Node Inspector
        if (selectedNode != null) {
            val nodeAct = ((simResult.nodeActivations[selectedNode.id] ?: 0.0) * 100).toInt()
            val curPert = perturbations[selectedNode.id] ?: NodePerturbation.NORMAL
            val outgoing = pathway.edges.filter { it.from == selectedNode.id }
            val incoming = pathway.edges.filter { it.to == selectedNode.id }
            GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedNode.label, style = MaterialTheme.typography.titleMedium)
                        Text("Compartment / Role: ${selectedNode.role} • Activation: $nodeAct%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    EvidencePill(curPert.label)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NodePerturbation.entries.forEach { pert ->
                        FilterChip(
                            selected = curPert == pert,
                            onClick = {
                                perturbations = if (pert == NodePerturbation.NORMAL) {
                                    perturbations - selectedNode.id
                                } else {
                                    perturbations + (selectedNode.id to pert)
                                }
                            },
                            label = { Text(pert.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sig_pert_${pert.name.lowercase()}")
                        )
                    }
                }
                if (incoming.isNotEmpty() || outgoing.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    val inText = incoming.joinToString { "${it.from} (${((simResult.edgeFluxes["${it.from}->${it.to}"] ?: 0.0) * 100).toInt()}%)" }
                    val outText = outgoing.joinToString { "${it.action} ${it.to} (${((simResult.edgeFluxes["${it.from}->${it.to}"] ?: 0.0) * 100).toInt()}%)" }
                    if (inText.isNotEmpty()) Text("Upstream inputs: $inText", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                    if (outText.isNotEmpty()) Text("Downstream targets: $outText", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                }
            }
        }

        // 5. Real-time Kinetics & Downstream Gene/Cytokine Charts
        LineChartCanvas(
            series = listOf(
                ChartSeries("Receptor/Kinase", MaterialTheme.colorScheme.primary, simResult.timeSeries.map { it.minute to it.receptorKinase }),
                ChartSeries("Cytosolic Adaptor", MaterialTheme.colorScheme.secondary, simResult.timeSeries.map { it.minute to it.cytosolicAdaptor }),
                ChartSeries("Nuclear Effector", semantic.established, simResult.timeSeries.map { it.minute to it.nuclearEffector })
            ),
            title = "Signal Phosphorylation Kinetics (0–60 min, % max)",
            height = 190.dp
        )

        BarChartCanvas(
            labels = simResult.downstreamOutputs.map { it.first },
            values = simResult.downstreamOutputs.map { it.second },
            title = "Downstream Effector / Gene Signature (% Induction)",
            height = 175.dp
        )

        // 6. Export Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val report = buildString {
                        appendLine("# ImmunoVerse Signaling Pathway Report: ${pathway.name}")
                        appendLine("- Cell Context: ${cellContext.name} (${cellContext.lineage})")
                        appendLine("- Condition: ${condition.name} — ${condition.summary}")
                        appendLine("- Stimulus Dose: ${(stimulusDose * 100).toInt()}% | Feedback Gain: ${(feedbackStrength * 100).toInt()}%")
                        appendLine("\n## Node Activations")
                        pathway.nodes.forEach { n ->
                            val pct = ((simResult.nodeActivations[n.id] ?: 0.0) * 100).toInt()
                            val p = perturbations[n.id]?.label ?: "Normal"
                            appendLine("- ${n.label} [${n.role}]: $pct% ($p)")
                        }
                        appendLine("\n## Downstream Signature")
                        simResult.downstreamOutputs.forEach { (g, v) -> appendLine("- $g: ${"%.1f".format(v)}%") }
                    }
                    ExportHelper.shareTextFile(context, "pathway_${pathway.id}.md", "text/markdown", report)
                        .onSuccess { onToast("Exported $it") }
                        .onFailure { onToast("Export failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("sig_export_md_btn")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Report MD")
            }
            OutlinedButton(
                onClick = {
                    val series = listOf(
                        "Receptor/Kinase" to simResult.timeSeries.map { it.minute to it.receptorKinase },
                        "Cytosolic Adaptor" to simResult.timeSeries.map { it.minute to it.cytosolicAdaptor },
                        "Nuclear Effector" to simResult.timeSeries.map { it.minute to it.nuclearEffector }
                    )
                    ExportHelper.exportChartPng(context, "signaling_${pathway.id}", "${pathway.name} (${cellContext.name})", series)
                        .onSuccess { onToast("Exported $it") }
                        .onFailure { onToast("PNG failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("sig_export_png_btn")
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kinetics PNG")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${pathway.name} — ${cellContext.name}", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { showFullscreen = false }) { Text("Done") }
                    }
                    DynamicSignalingCanvasCard(
                        pathway = pathway,
                        simResult = simResult,
                        perturbations = perturbations,
                        selectedNodeId = selectedNode?.id,
                        isPlaying = isPlaying && !reduceMotion,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onSelectNode = { selectedNodeId = it },
                        onOpenFullscreen = null,
                        height = 520.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicSignalingCanvasCard(
    pathway: PathwayInfo,
    simResult: SignalingSimulationResult,
    perturbations: Map<String, NodePerturbation>,
    selectedNodeId: String?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelectNode: (String) -> Unit,
    onOpenFullscreen: (() -> Unit)?,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainerLowest
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    var pulsePhase by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember(pathway.id) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                pulsePhase = (pulsePhase + 0.022f) % 1f
                delay(28L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(surfaceBg)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.large)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${pathway.name} • Live Signal Canvas", style = MaterialTheme.typography.titleSmall, color = primary)
                Text("Tap any node to inspect/inhibit • Drag to pan", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(40.dp).testTag("sig_play_pause_btn")) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Toggle signal animation", tint = primary)
                }
                IconButton(onClick = { zoom = (zoom - 0.15f).coerceAtLeast(0.75f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                }
                IconButton(onClick = { zoom = (zoom + 0.15f).coerceAtMost(1.8f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                }
                IconButton(onClick = { zoom = 1f; panOffset = Offset.Zero }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset view")
                }
                if (onOpenFullscreen != null) {
                    IconButton(onClick = onOpenFullscreen, modifier = Modifier.size(40.dp).testTag("sig_fullscreen_btn")) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.cd_fullscreen))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primary.copy(alpha = 0.05f),
                            secondary.copy(alpha = 0.04f),
                            semantic.established.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pathway, zoom, panOffset) {
                        detectTapGestures { tap ->
                            val padX = size.width * 0.08f
                            val padY = size.height * 0.14f
                            val usableW = size.width - padX * 2f
                            val usableH = size.height - padY * 2f
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            fun nodePos(n: PathwayNode): Offset {
                                val raw = Offset(padX + n.x * usableW, padY + n.y * usableH)
                                return Offset(
                                    cx + (raw.x - cx) * zoom + panOffset.x,
                                    cy + (raw.y - cy) * zoom + panOffset.y
                                )
                            }

                            val hit = pathway.nodes.minByOrNull { n ->
                                val p = nodePos(n)
                                (p.x - tap.x) * (p.x - tap.x) + (p.y - tap.y) * (p.y - tap.y)
                            }
                            if (hit != null) {
                                val p = nodePos(hit)
                                val dist = sqrt((p.x - tap.x) * (p.x - tap.x) + (p.y - tap.y) * (p.y - tap.y))
                                if (dist < 68f * zoom) onSelectNode(hit.id)
                            }
                        }
                    }
                    .pointerInput(pathway.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            panOffset += drag
                        }
                    }
            ) {
                drawSubcellularBackdrop(textMeasurer, semantic.mutedText, primary, secondary, semantic.established)

                val padX = size.width * 0.08f
                val padY = size.height * 0.14f
                val usableW = size.width - padX * 2f
                val usableH = size.height - padY * 2f
                val cx = size.width / 2f
                val cy = size.height / 2f

                val pos = pathway.nodes.associate { n ->
                    val raw = Offset(padX + n.x * usableW, padY + n.y * usableH)
                    n.id to Offset(cx + (raw.x - cx) * zoom + panOffset.x, cy + (raw.y - cy) * zoom + panOffset.y)
                }

                // Draw dynamic edges with Bezier curves + arrows / T-bars + signal packets
                pathway.edges.forEachIndexed { idx, edge ->
                    val p1 = pos[edge.from] ?: return@forEachIndexed
                    val p2 = pos[edge.to] ?: return@forEachIndexed
                    val flux = (simResult.edgeFluxes["${edge.from}->${edge.to}"] ?: 0.2).toFloat()
                    val isInhibit = edge.action == "inhibits"
                    val edgeColor = when {
                        isInhibit -> semantic.critical.copy(alpha = 0.35f + 0.65f * flux)
                        flux > 0.65f -> semantic.established
                        flux > 0.3f -> primary
                        else -> semantic.mutedText.copy(alpha = 0.45f)
                    }
                    val ctrlOffset = if (isInhibit) 42f * zoom else -18f * zoom
                    val mid = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f + ctrlOffset)
                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        quadraticTo(mid.x, mid.y, p2.x, p2.y)
                    }
                    val strokeW = (2.2f + flux * 5.2f) * zoom
                    drawPath(
                        path = path,
                        color = edgeColor,
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            pathEffect = if (isInhibit) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
                        )
                    )

                    val angle = atan2(p2.y - mid.y, p2.x - mid.x)
                    val endR = 25f * zoom
                    val tip = Offset(p2.x - cos(angle) * endR, p2.y - sin(angle) * endR)
                    if (isInhibit) {
                        val perp = angle + (Math.PI / 2.0).toFloat()
                        drawLine(
                            edgeColor,
                            Offset(tip.x + cos(perp) * 10f * zoom, tip.y + sin(perp) * 10f * zoom),
                            Offset(tip.x - cos(perp) * 10f * zoom, tip.y - sin(perp) * 10f * zoom),
                            strokeWidth = 4f * zoom
                        )
                    } else {
                        val back = 11f * zoom
                        val spread = 0.48f
                        val arrow = Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(tip.x - cos(angle - spread) * back, tip.y - sin(angle - spread) * back)
                            lineTo(tip.x - cos(angle + spread) * back, tip.y - sin(angle + spread) * back)
                            close()
                        }
                        drawPath(arrow, edgeColor)
                    }

                    // Animated traveling signal packet scaled by edge flux
                    if (flux > 0.08f) {
                        val t = ((pulsePhase * (0.75f + flux * 1.15f) + idx * 0.19f) % 1f)
                        val inv = 1f - t
                        val bx = inv * inv * p1.x + 2f * inv * t * mid.x + t * t * p2.x
                        val by = inv * inv * p1.y + 2f * inv * t * mid.y + t * t * p2.y
                        drawCircle(edgeColor.copy(alpha = 0.45f), radius = (9f + flux * 5f) * zoom, center = Offset(bx, by))
                        drawCircle(Color.White, radius = (3.6f + flux * 2.5f) * zoom, center = Offset(bx, by))
                    }
                }

                // Draw dynamic biological nodes
                pathway.nodes.forEach { n ->
                    val c = pos[n.id] ?: return@forEach
                    val act = (simResult.nodeActivations[n.id] ?: 0.0).toFloat()
                    val pert = perturbations[n.id] ?: NodePerturbation.NORMAL
                    val isSel = n.id == selectedNodeId
                    val r = (22f + act * 6f) * zoom

                    val nodeColor = when (pert) {
                        NodePerturbation.INHIBITED -> semantic.critical
                        NodePerturbation.HYPERACTIVE -> semantic.hypothesis
                        NodePerturbation.NORMAL -> when {
                            act >= 0.65f -> semantic.established
                            act >= 0.35f -> primary
                            else -> semantic.mutedText
                        }
                    }

                    // Activation aura
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.14f + act * 0.28f),
                        radius = r + (8f + act * 14f) * zoom,
                        center = c
                    )
                    // Core fill & crisp border
                    drawCircle(color = surfaceBg.copy(alpha = 0.92f), radius = r, center = c)
                    drawCircle(color = nodeColor.copy(alpha = 0.28f + act * 0.42f), radius = r, center = c)
                    drawCircle(
                        color = if (isSel) Color.White else nodeColor,
                        radius = r,
                        center = c,
                        style = Stroke(width = (if (isSel) 4.2f else 2.6f) * zoom)
                    )
                    if (pert == NodePerturbation.INHIBITED) {
                        val d = r * 0.55f
                        drawLine(semantic.critical, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), strokeWidth = 3f * zoom)
                        drawLine(semantic.critical, Offset(c.x + d, c.y - d), Offset(c.x - d, c.y + d), strokeWidth = 3f * zoom)
                    }

                    val pctText = "${(act * 100).toInt()}%"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = pctText,
                        topLeft = Offset(c.x - 13f * zoom, c.y - 7f * zoom),
                        style = TextStyle(
                            color = onSurface,
                            fontSize = TextUnit(10f, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = n.label.take(18),
                        topLeft = Offset((c.x - 42f).coerceIn(4f, size.width - 108f), (c.y + r + 4f).coerceAtMost(size.height - 20f)),
                        style = TextStyle(
                            color = if (pert == NodePerturbation.INHIBITED) semantic.critical else onSurface,
                            fontSize = TextUnit(9.5f, TextUnitType.Sp),
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSubcellularBackdrop(
    textMeasurer: TextMeasurer,
    muted: Color,
    primary: Color,
    secondary: Color,
    established: Color
) {
    val w = size.width
    val h = size.height
    // Plasma membrane band (x ~ 0.24)
    val memX = w * 0.24f
    drawRoundRect(
        color = primary.copy(alpha = 0.06f),
        topLeft = Offset(memX - 8f, 8f),
        size = Size(16f, h - 16f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawLine(primary.copy(alpha = 0.35f), Offset(memX - 4f, 10f), Offset(memX - 4f, h - 10f), strokeWidth = 2f)
    drawLine(primary.copy(alpha = 0.35f), Offset(memX + 4f, 10f), Offset(memX + 4f, h - 10f), strokeWidth = 2f)

    // Nuclear envelope boundary (x ~ 0.76)
    val nucX = w * 0.76f
    drawLine(
        color = secondary.copy(alpha = 0.42f),
        start = Offset(nucX, 10f),
        end = Offset(nucX, h - 10f),
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
    )

    val labelStyle = TextStyle(color = muted.copy(alpha = 0.78f), fontSize = TextUnit(8.5f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold)
    drawText(textMeasurer, "EXTRACELLULAR", Offset(10f, 6f), labelStyle)
    drawText(textMeasurer, "CYTOSOL", Offset(w * 0.44f, 6f), labelStyle.copy(color = primary.copy(alpha = 0.7f)))
    drawText(textMeasurer, "NUCLEUS", Offset(w * 0.80f, 6f), labelStyle.copy(color = established.copy(alpha = 0.8f)))
}
