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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BookmarkItem
import com.example.domain.VirtualLabEngine
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.ChartSeries
import com.example.ui.components.EvidencePill
import com.example.ui.components.FlowCytometryCanvas
import com.example.ui.components.GelElectrophoresisCanvas
import com.example.ui.components.GlassCard
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.MicroscopyCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper

@Composable
fun VirtualLabScreen(
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?) -> Unit,
    onRecordRecent: (String) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    val categories = listOf("Immunology", "Molecular Biology", "Microbiology")
    var selectedCategory by remember { mutableStateOf("Immunology") }
    val filteredExps = remember(selectedCategory) {
        VirtualLabEngine.EXPERIMENTS.filter { it.category == selectedCategory }
    }
    var selectedExpId by remember(selectedCategory) { mutableStateOf(filteredExps.first().id) }
    val exp = remember(selectedExpId) { VirtualLabEngine.EXPERIMENTS.first { it.id == selectedExpId } }

    val paramValues = remember { mutableStateMapOf<String, Float>() }
    var showAnswer by remember(selectedExpId) { mutableStateOf(false) }

    LaunchedEffect(selectedExpId) {
        paramValues.clear()
        exp.params.forEach { paramValues[it.key] = it.defaultVal }
    }

    val output = remember(selectedExpId, paramValues.toMap()) {
        VirtualLabEngine.runExperiment(selectedExpId, paramValues.toMap())
    }
    val isBookmarked = bookmarks.any { it.itemId == exp.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Virtual Laboratory",
            subtitle = "22 interactive simulated bench protocols with PDF & Markdown reports.",
            badge = "Simulated Lab",
            isBookmarked = isBookmarked,
            onToggleBookmark = { onToggleBookmark("lab", exp.id, exp.title, exp.category) }
        )

        BannerDisclaimer(stringResource(R.string.lab_disclaimer), isSimulation = true)

        PillSelectorRow(
            options = categories,
            selected = selectedCategory,
            onSelect = { selectedCategory = it },
            tagPrefix = "lab_cat"
        )

        PillSelectorRow(
            options = filteredExps.map { it.title },
            selected = exp.title,
            onSelect = { title ->
                val f = filteredExps.first { it.title == title }
                selectedExpId = f.id
                onRecordRecent(f.title)
            },
            tagPrefix = "lab_exp"
        )

        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exp.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                EvidencePill("Simulated")
            }
            Spacer(Modifier.height(4.dp))
            Text("Objective: ${exp.objective}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text("Theory: ${exp.theory}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("Equipment: ${exp.equipment.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            Spacer(Modifier.height(6.dp))
            Text("Procedure Steps:", style = MaterialTheme.typography.labelLarge)
            exp.procedure.forEachIndexed { idx, step ->
                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Interactive Bench Parameters
        GlassCard {
            Text("Experiment Parameters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            exp.params.forEach { p ->
                val cur = paramValues[p.key] ?: p.defaultVal
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.label, style = MaterialTheme.typography.labelMedium)
                        Text("${"%.2f".format(cur)} ${p.unit}".trim(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = cur.coerceIn(p.min, p.max),
                        onValueChange = { paramValues[p.key] = it },
                        valueRange = p.min..p.max,
                        modifier = Modifier.testTag("lab_param_${p.key}")
                    )
                }
            }
            Button(
                onClick = { onToast("Simulated assay updated live") },
                modifier = Modifier.fillMaxWidth().testTag("lab_run_btn")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Recalculate Observations")
            }
        }

        // Visual Output
        when (output.visual.mode) {
            "flow" -> FlowCytometryCanvas(points = output.visual.flowPoints)
            "gel" -> GelElectrophoresisCanvas(lanes = output.visual.gelLanes)
            "microscopy" -> {
                val mode = when {
                    exp.id == "gram_stain" -> "Gram"
                    exp.id == "immunofluorescence" -> "Immunofluorescence"
                    else -> "H&E"
                }
                MicroscopyCanvas(stainMode = mode, magnification = if (exp.id == "microscopy") 100f else 60f)
            }
            "curve" -> {
                val list = buildList {
                    if (output.visual.seriesA.isNotEmpty()) {
                        add(ChartSeries("Primary Trace", MaterialTheme.colorScheme.primary, output.visual.seriesA))
                    }
                    if (output.visual.seriesB.isNotEmpty()) {
                        add(ChartSeries("Comparison / Unknown", MaterialTheme.colorScheme.secondary, output.visual.seriesB))
                    }
                }
                if (list.isNotEmpty()) {
                    LineChartCanvas(series = list, title = "${exp.title} — Simulated Readout")
                }
            }
        }

        // Observations & Metrics
        GlassCard {
            Text("Observed Results", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            output.observations.forEach { obs ->
                Text("• $obs", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            output.metrics.forEach { (k, v) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                    Text(v, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Interpretation: ${output.interpretation}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Limitations: ${exp.limitations}", style = MaterialTheme.typography.labelSmall, color = semantic.illustrative)
        }

        // Pedagogical Check
        GlassCard {
            Text("Self-Check Question", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(exp.question, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { showAnswer = !showAnswer }, modifier = Modifier.testTag("lab_reveal_ans_btn")) {
                Text(if (showAnswer) "Hide Answer Key" else "Reveal Answer Key")
            }
            if (showAnswer) {
                Spacer(Modifier.height(6.dp))
                Text("Answer: ${exp.answerKey}", style = MaterialTheme.typography.bodySmall, color = semantic.established)
            }
        }

        // Exports
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val sections = listOf(
                        "Category & Objective" to "${exp.category}\n${exp.objective}",
                        "Theory & Equipment" to "${exp.theory}\nEquipment: ${exp.equipment.joinToString()}",
                        "Simulated Observations" to output.observations.joinToString("\n"),
                        "Calculated Metrics" to output.metrics.entries.joinToString("\n") { "${it.key}: ${it.value}" },
                        "Interpretation & Limitations" to "${output.interpretation}\nNote: ${exp.limitations}"
                    )
                    ExportHelper.exportAndSharePdf(
                        context = context,
                        filename = "lab_report_${exp.id}.pdf",
                        title = "Virtual Lab Report: ${exp.title}",
                        sections = sections
                    ).onSuccess { onToast("Shared PDF $it") }
                        .onFailure { onToast("PDF failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("lab_pdf_btn")
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("PDF Report")
            }
            OutlinedButton(
                onClick = {
                    val md = buildString {
                        appendLine("# Virtual Lab Report: ${exp.title}")
                        appendLine("_Simulated Educational Output — Not for Clinical Diagnostics_")
                        appendLine("\n## Objective\n${exp.objective}")
                        appendLine("\n## Observations")
                        output.observations.forEach { appendLine("- $it") }
                        appendLine("\n## Metrics")
                        output.metrics.forEach { (k, v) -> appendLine("- **$k**: $v") }
                        appendLine("\n## Interpretation\n${output.interpretation}")
                        appendLine("\n## Limitations\n${exp.limitations}")
                    }
                    ExportHelper.shareTextFile(context, "lab_${exp.id}.md", "text/markdown", md, "Export Lab Markdown")
                        .onSuccess { onToast("Shared Markdown $it") }
                        .onFailure { onToast("Export failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("lab_md_btn")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Markdown")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
