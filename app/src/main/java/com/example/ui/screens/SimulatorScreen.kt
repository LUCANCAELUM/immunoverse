package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.SavedSimulation
import com.example.domain.ImmuneSimulatorEngine
import com.example.domain.SimParams
import com.example.domain.SimStep
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.ChartSeries
import com.example.ui.components.DetailedImmuneArenaCard
import com.example.ui.components.GlassCard
import com.example.ui.components.ImmunePresetBriefingCard
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.components.SimulationMetricsDetailCard
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper
import kotlinx.coroutines.delay

@Composable
fun SimulatorScreen(
    reduceMotion: Boolean,
    savedSimulations: List<SavedSimulation>,
    onSaveSimulation: (SavedSimulation) -> Unit,
    onDeleteSimulation: (String) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var params by remember { mutableStateOf(ImmuneSimulatorEngine.PRESETS.first().params) }
    var chartView by remember { mutableStateOf("Pathogen & Innate") }
    var compareSimId by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    val result = remember(params) { ImmuneSimulatorEngine.runSim(params) }
    val compareRun = remember(compareSimId, savedSimulations) {
        savedSimulations.firstOrNull { it.id == compareSimId }?.let { s ->
            ImmuneSimulatorEngine.runSim(
                SimParams(
                    presetKey = s.presetKey,
                    antigenLoad = s.antigenLoad,
                    innateGain = s.innateGain,
                    tCellGain = s.tCellGain,
                    bCellGain = s.bCellGain,
                    abClear = s.abClear,
                    autoTolerance = s.autoTolerance,
                    rechallengeDay = s.rechallengeDay,
                    durationDays = s.durationDays
                )
            )
        }
    }

    var replayDay by remember(params) { mutableFloatStateOf(params.durationDays.toFloat()) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, params.durationDays, reduceMotion) {
        if (isPlaying) {
            if (replayDay >= params.durationDays.toFloat()) replayDay = 0f
            while (isPlaying && replayDay < params.durationDays.toFloat()) {
                replayDay = (replayDay + 0.5f).coerceAtMost(params.durationDays.toFloat())
                delay(if (reduceMotion) 180L else 60L)
            }
            isPlaying = false
        }
    }

    val currentStep: SimStep = remember(result, replayDay) {
        result.series.minByOrNull { kotlin.math.abs(it.day - replayDay) } ?: result.series.last()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Immune ODE Simulator",
            subtitle = "Coupled 9-variable innate, adaptive, memory & immunopathology dynamics.",
            badge = "Simulated Model"
        )

        BannerDisclaimer(stringResource(R.string.sim_disclaimer), isSimulation = true)

        PillSelectorRow(
            options = ImmuneSimulatorEngine.PRESETS.map { it.label },
            selected = ImmuneSimulatorEngine.PRESETS.firstOrNull { it.key == params.presetKey }?.label ?: "Normal Response",
            onSelect = { label ->
                val p = ImmuneSimulatorEngine.PRESETS.first { it.label == label }
                params = p.params
                replayDay = p.params.durationDays.toFloat()
                isPlaying = false
            },
            tagPrefix = "sim_preset"
        )

        val activePreset = ImmuneSimulatorEngine.PRESETS.firstOrNull { it.key == params.presetKey }
        ImmunePresetBriefingCard(activePreset)

        DetailedImmuneArenaCard(
            currentStep = currentStep,
            params = params,
            reduceMotion = reduceMotion,
            replayDay = replayDay,
            isPlaying = isPlaying,
            onTogglePlay = { isPlaying = !isPlaying },
            onScrubDay = {
                isPlaying = false
                replayDay = it
            }
        )

        PillSelectorRow(
            options = listOf("Pathogen & Innate", "Effectors & Antibodies", "Cytokines & Tissue", "Derived Compartments & GC"),
            selected = chartView,
            onSelect = { chartView = it },
            tagPrefix = "sim_chart"
        )

        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val crit = semantic.critical
        val est = semantic.established
        val simCol = semantic.simulated

        val chartSeries = remember(result, compareRun, chartView) {
            when (chartView) {
                "Pathogen & Innate" -> buildList {
                    add(ChartSeries("Antigen", crit, result.series.map { it.day to it.antigen }))
                    add(ChartSeries("Innate", primary, result.series.map { it.day to it.innate }))
                    add(ChartSeries("Phagocytosis", est, result.series.map { it.day to it.phagocyticClearance }))
                    compareRun?.let { cmp ->
                        add(ChartSeries("Cmp Antigen", simCol, cmp.series.map { it.day to it.antigen }))
                    }
                }
                "Effectors & Antibodies" -> buildList {
                    add(ChartSeries("T Cells", primary, result.series.map { it.day to it.tCell }))
                    add(ChartSeries("Antibody", est, result.series.map { it.day to it.antibody }))
                    add(ChartSeries("Memory", secondary, result.series.map { it.day to it.memory }))
                    compareRun?.let { cmp ->
                        add(ChartSeries("Cmp Antibody", simCol, cmp.series.map { it.day to it.antibody }))
                    }
                }
                "Cytokines & Tissue" -> buildList {
                    add(ChartSeries("Cytokine", tertiary, result.series.map { it.day to it.cytokine }))
                    add(ChartSeries("Tissue Damage", crit, result.series.map { it.day to it.damage }))
                    add(ChartSeries("B Cells", primary, result.series.map { it.day to it.bCell }))
                }
                else -> buildList {
                    add(ChartSeries("APC Activation", tertiary, result.series.map { it.day to it.apcActivation }))
                    add(ChartSeries("GC Affinity (x)", est, result.series.map { it.day to it.affinityMaturation }))
                    add(ChartSeries("Complement MAC", crit, result.series.map { it.day to it.complementFlux }))
                    add(ChartSeries("Treg Suppression", secondary, result.series.map { it.day to it.tregSuppression }))
                }
            }
        }

        LineChartCanvas(
            series = chartSeries,
            title = "$chartView (Normalized Units)",
            markerX = currentStep.day
        )

        // Action Buttons (Save, CSV, PNG)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    saveName = "${params.presetKey.uppercase()} Run"
                    showSaveDialog = true
                },
                modifier = Modifier.weight(1f).testTag("sim_save_btn")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
            OutlinedButton(
                onClick = {
                    ExportHelper.shareTextFile(
                        context = context,
                        filename = "immunoverse_sim_${params.presetKey}.csv",
                        mimeType = "text/csv",
                        content = ImmuneSimulatorEngine.toCsv(result),
                        chooserTitle = "Export Simulation CSV"
                    ).onSuccess { onToast("Exported $it") }
                        .onFailure { onToast("Export failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("sim_export_csv_btn")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CSV")
            }
            OutlinedButton(
                onClick = {
                    ExportHelper.exportChartPng(
                        context = context,
                        filename = "sim_${params.presetKey}.png",
                        title = "ImmunoVerse ODE: $chartView",
                        series = chartSeries.map { it.label to it.points }
                    ).onSuccess { onToast("Shared PNG $it") }
                        .onFailure { onToast("PNG failed: ${it.localizedMessage}") }
                },
                modifier = Modifier.weight(1f).testTag("sim_export_png_btn")
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("PNG")
            }
        }

        // Parameter Sliders
        GlassCard {
            Text("ODE Parameters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            SimSliderRow("Initial Antigen Load", params.antigenLoad.toFloat(), 0.2f..3.0f) {
                params = params.copy(antigenLoad = it.toDouble())
            }
            SimSliderRow("Innate Gain", params.innateGain.toFloat(), 0.2f..2.2f) {
                params = params.copy(innateGain = it.toDouble())
            }
            SimSliderRow("T-Cell Priming Gain", params.tCellGain.toFloat(), 0.1f..2.2f) {
                params = params.copy(tCellGain = it.toDouble())
            }
            SimSliderRow("B-Cell / Ab Gain", params.bCellGain.toFloat(), 0.1f..2.2f) {
                params = params.copy(bCellGain = it.toDouble())
            }
            SimSliderRow("Ab Clearance Efficacy", params.abClear.toFloat(), 0.1f..2.2f) {
                params = params.copy(abClear = it.toDouble())
            }
            SimSliderRow("Self-Tolerance Threshold", params.autoTolerance.toFloat(), 0.1f..1.6f) {
                params = params.copy(autoTolerance = it.toDouble())
            }
            SimSliderRow("Rechallenge Day (0 = off)", params.rechallengeDay.toFloat(), 0f..40f) {
                params = params.copy(rechallengeDay = it.toDouble())
            }
            SimSliderRow("Horizon Days", params.durationDays.toFloat(), 15f..60f) {
                params = params.copy(durationDays = it.toDouble())
                if (replayDay > it) replayDay = it
            }
        }

        SimulationMetricsDetailCard(result = result)
        Simulation3DSyllabusCard(unit = Simulation3DSyllabusKb.byId("immune_kinetics_3d"))

        // Saved Simulations (Cloud Replay & Compare)
        if (savedSimulations.isNotEmpty()) {
            GlassCard {
                Text("Saved Cloud Runs (Tap to Load or Compare)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                savedSimulations.forEach { sim ->
                    val isCmp = sim.id == compareSimId
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sim.name, style = MaterialTheme.typography.titleSmall)
                            Text("Preset: ${sim.presetKey} • Peak Ab: ${"%.2f".format(sim.peakAntibody)}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = {
                                params = SimParams(
                                    presetKey = sim.presetKey,
                                    antigenLoad = sim.antigenLoad,
                                    innateGain = sim.innateGain,
                                    tCellGain = sim.tCellGain,
                                    bCellGain = sim.bCellGain,
                                    abClear = sim.abClear,
                                    autoTolerance = sim.autoTolerance,
                                    rechallengeDay = sim.rechallengeDay,
                                    durationDays = sim.durationDays
                                )
                                replayDay = sim.durationDays.toFloat()
                            }) { Text("Load") }
                            OutlinedButton(onClick = {
                                compareSimId = if (isCmp) null else sim.id
                            }) { Text(if (isCmp) "Unpin" else "Overlay") }
                            IconButton(onClick = { onDeleteSimulation(sim.id) }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete simulation", tint = semantic.critical)
                            }
                        }
                    }
                }
            }
        }

        // Documented Equations & Limitations
        GlassCard {
            Text("Mathematical Model & Limitations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(ImmuneSimulatorEngine.MODEL_EQUATIONS.trim(), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(ImmuneSimulatorEngine.MODEL_ASSUMPTIONS.trim(), style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Simulation Run") },
            text = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text("Run Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sim_save_name_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSaveSimulation(
                        SavedSimulation(
                            name = saveName.ifBlank { "Simulation Run" },
                            presetKey = params.presetKey,
                            antigenLoad = params.antigenLoad,
                            innateGain = params.innateGain,
                            tCellGain = params.tCellGain,
                            bCellGain = params.bCellGain,
                            abClear = params.abClear,
                            autoTolerance = params.autoTolerance,
                            rechallengeDay = params.rechallengeDay,
                            durationDays = params.durationDays,
                            peakAntigen = result.peakAntigen,
                            peakCytokine = result.peakCytokine,
                            peakAntibody = result.peakAntibody,
                            peakMemory = result.peakMemory
                        )
                    )
                    showSaveDialog = false
                }) { Text("Save to Cloud") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SimSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("%.2f".format(value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}
