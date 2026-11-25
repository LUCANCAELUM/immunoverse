package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.BioinformaticsEngine
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.ChartSeries
import com.example.ui.components.GlassCard
import com.example.ui.components.HeatmapCanvas
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.ScatterOlsCanvas
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper

@Composable
fun AnalyticsScreen(
    sharedCsv: String?,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var csvText by remember { mutableStateOf(sharedCsv ?: BioinformaticsEngine.DEMO_CYTOKINE_CSV) }
    LaunchedEffect(sharedCsv) {
        if (!sharedCsv.isNullOrBlank()) csvText = sharedCsv
    }

    var chartMode by remember { mutableStateOf("Line") }
    var xColIdx by remember { mutableIntStateOf(0) }
    var yColIdx by remember { mutableIntStateOf(1) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val txt = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (txt.isNotBlank()) {
                    csvText = txt.take(30_000)
                    onToast("Loaded CSV dataset")
                }
            }.onFailure { onToast("CSV error: ${it.localizedMessage}") }
        }
    }

    val table = remember(csvText) { BioinformaticsEngine.parseCsv(csvText) }
    val numCols = remember(table) {
        table.headers.indices.filter { idx -> BioinformaticsEngine.extractNumericColumn(table, idx).size >= 2 }
    }
    val safeX = if (xColIdx in table.headers.indices) xColIdx else numCols.firstOrNull() ?: 0
    val safeY = if (yColIdx in table.headers.indices) yColIdx else numCols.getOrElse(1) { safeX }

    val xs = remember(table, safeX) { BioinformaticsEngine.extractNumericColumn(table, safeX) }
    val ys = remember(table, safeY) { BioinformaticsEngine.extractNumericColumn(table, safeY) }
    val pairs = remember(xs, ys) { xs.zip(ys) }
    val fit = remember(xs, ys) { BioinformaticsEngine.olsFit(xs, ys) }
    val rVal = remember(xs, ys) { BioinformaticsEngine.pearson(xs, ys) }

    val statsList = remember(table, numCols) {
        numCols.mapNotNull { cIdx ->
            BioinformaticsEngine.describeColumn(table.headers[cIdx], BioinformaticsEngine.extractNumericColumn(table, cIdx))
        }
    }

    val corrMatrix = remember(table, numCols) {
        val cols = numCols.take(5)
        cols.map { i ->
            val a = BioinformaticsEngine.extractNumericColumn(table, i)
            cols.map { j -> BioinformaticsEngine.pearson(a, BioinformaticsEngine.extractNumericColumn(table, j)) }
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
            title = "Data & Statistical Analytics",
            subtitle = "CSV ingestion, descriptive stats, Pearson r, OLS regression & heatmaps.",
            badge = "Quantitative"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { csvPicker.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
                modifier = Modifier.weight(1f).testTag("ana_upload_csv_btn")
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Upload CSV")
            }
            OutlinedButton(
                onClick = { csvText = BioinformaticsEngine.DEMO_CYTOKINE_CSV },
                modifier = Modifier.weight(1f)
            ) { Text("Load Cytokine Demo") }
        }

        OutlinedTextField(
            value = csvText,
            onValueChange = { csvText = it },
            label = { Text("CSV / TSV Table (${table.rows.size} rows)") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth().testTag("ana_csv_input")
        )

        PillSelectorRow(
            options = listOf("Line", "Scatter + OLS", "Bar", "Heatmap", "Correlation"),
            selected = chartMode,
            onSelect = { chartMode = it },
            tagPrefix = "ana_mode"
        )

        if (table.headers.isNotEmpty()) {
            Text("X Axis Column: ${table.headers.getOrElse(safeX) { "?" }}", style = MaterialTheme.typography.labelMedium)
            PillSelectorRow(
                options = table.headers,
                selected = table.headers.getOrElse(safeX) { "" },
                onSelect = { h -> xColIdx = table.headers.indexOf(h).coerceAtLeast(0) },
                tagPrefix = "ana_xcol"
            )
            Text("Y Axis Column: ${table.headers.getOrElse(safeY) { "?" }}", style = MaterialTheme.typography.labelMedium)
            PillSelectorRow(
                options = table.headers,
                selected = table.headers.getOrElse(safeY) { "" },
                onSelect = { h -> yColIdx = table.headers.indexOf(h).coerceAtLeast(0) },
                tagPrefix = "ana_ycol"
            )
        }

        when (chartMode) {
            "Line" -> LineChartCanvas(
                series = listOf(ChartSeries(table.headers.getOrElse(safeY) { "Y" }, MaterialTheme.colorScheme.primary, pairs)),
                title = "${table.headers.getOrElse(safeY) { "Y" }} over ${table.headers.getOrElse(safeX) { "X" }}"
            )
            "Scatter + OLS" -> {
                ScatterOlsCanvas(
                    points = pairs,
                    fit = fit,
                    xLabel = table.headers.getOrElse(safeX) { "X" },
                    yLabel = table.headers.getOrElse(safeY) { "Y" }
                )
                if (fit != null) {
                    GlassCard {
                        Text("OLS Fit: y = ${"%.3f".format(fit.slope)}x + ${"%.3f".format(fit.intercept)} | Pearson r = ${"%.3f".format(rVal)} | R² = ${"%.3f".format(fit.rSquared)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            "Bar" -> BarChartCanvas(
                labels = xs.map { "%.0f".format(it) },
                values = ys,
                title = table.headers.getOrElse(safeY) { "Values" }
            )
            "Heatmap" -> {
                val hCols = numCols.take(5)
                val hLabels = hCols.map { table.headers[it] }
                val mat = table.rows.take(10).map { row -> hCols.map { ci -> row.getOrNull(ci)?.toDoubleOrNull() ?: 0.0 } }
                HeatmapCanvas(
                    rowLabels = table.rows.take(10).indices.map { "R${it + 1}" },
                    colLabels = hLabels,
                    matrix = mat,
                    title = "Dataset Matrix Heatmap"
                )
            }
            else -> {
                val cLabels = numCols.take(5).map { table.headers[it] }
                HeatmapCanvas(
                    rowLabels = cLabels,
                    colLabels = cLabels,
                    matrix = corrMatrix,
                    title = "Pearson Correlation Matrix (r)"
                )
            }
        }

        // Descriptive Stats Table
        GlassCard {
            Text("Descriptive Statistics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            statsList.forEach { st ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("${st.column} (n=${st.n})", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Mean: ${"%.2f".format(st.mean)} ± SD ${"%.2f".format(st.sd)} | Median: ${"%.2f".format(st.median)} [IQR ${"%.2f".format(st.q1)}–${"%.2f".format(st.q3)}] | Range: ${"%.2f".format(st.min)}..${"%.2f".format(st.max)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.mutedText
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val label = table.headers.getOrElse(safeY) { "Series" }
                    ExportHelper.exportChartPng(context, "analytics_chart.png", "ImmunoVerse Analytics: $label", listOf(label to pairs))
                        .onSuccess { onToast("Exported $it") }
                        .onFailure { onToast("PNG failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("ana_png_btn")
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export PNG")
            }
            OutlinedButton(
                onClick = {
                    val md = buildString {
                        appendLine("# ImmunoVerse Statistical Summary")
                        statsList.forEach { st ->
                            appendLine("- **${st.column}** (n=${st.n}): mean=${"%.3f".format(st.mean)}, SD=${"%.3f".format(st.sd)}, median=${"%.3f".format(st.median)}, Q1=${"%.3f".format(st.q1)}, Q3=${"%.3f".format(st.q3)}")
                        }
                        fit?.let { appendLine("\nOLS: y = ${"%.4f".format(it.slope)}x + ${"%.4f".format(it.intercept)} (R²=${"%.4f".format(it.rSquared)})") }
                    }
                    ExportHelper.shareTextFile(context, "analytics_summary.md", "text/markdown", md)
                        .onSuccess { onToast("Shared $it") }
                },
                modifier = Modifier.weight(1f).testTag("ana_md_btn")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Markdown")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
