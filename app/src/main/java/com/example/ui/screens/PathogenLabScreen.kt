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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.domain.PathogenClass
import com.example.domain.PathogenDesign
import com.example.domain.PathogenSimulatorEngine
import com.example.domain.TreatmentStrategy
import com.example.domain.TropismTarget
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.ChartSeries
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.MultiKingdomPathogenExplorerPanel
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.SpikeHostReceptorComparePanel
import com.example.ui.components.buildCustomPathogen3DScene
import com.example.ui.components.buildVirtualHostInfection3DScene
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay

@Composable
fun PathogenLabScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onOpenD3Genomics: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("3D Kingdom Explorer") }
    var design by remember { mutableStateOf(PathogenSimulatorEngine.REFERENCE_STRAINS[2]) } // Omicron JN.1 default
    var host by remember { mutableStateOf(PathogenSimulatorEngine.HOST_PROFILES.first()) }
    var treatment by remember { mutableStateOf(TreatmentStrategy()) }
    var chartMode by remember { mutableStateOf("Pathogen & Burden") }

    val simResult = remember(design, host, treatment) {
        PathogenSimulatorEngine.runHostInfectionSim(design, host, treatment)
    }

    var replayDay by remember { mutableFloatStateOf(5f) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, reduceMotion) {
        if (isPlaying) {
            if (replayDay >= 28f) replayDay = 0f
            while (isPlaying && replayDay < 28f) {
                replayDay = (replayDay + 0.5f).coerceAtMost(28f)
                delay(if (reduceMotion) 180L else 65L)
            }
            isPlaying = false
        }
    }

    val currentStep = remember(simResult, replayDay) {
        simResult.series.minByOrNull { kotlin.math.abs(it.day - replayDay) } ?: simResult.series.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Pathogen Dashboard & Virtual Host Lab",
            subtitle = "Categorized 3D pathogen kingdoms, evolutionary variants, viral spike vs host receptors & host kinetics.",
            badge = "In Silico Host Model",
            isBookmarked = bookmarks.any { it.itemId == design.id },
            onToggleBookmark = {
                onToggleBookmark(
                    "pathogen",
                    design.id,
                    design.name,
                    "${design.pathogenClass.label} • ${design.tropism.label}",
                    design.referenceNotes.ifBlank { "Receptor affinity ${design.receptorAffinity}× • Evasion ${design.immuneEvasion}×" }
                )
            }
        )

        BannerDisclaimer(
            text = "Educational computational host-pathogen simulation only. Models theoretical viral/microbial kinetics & receptor tropism—never actionable wet-lab engineering or clinical dosing.",
            isSimulation = true
        )

        PillSelectorRow(
            options = listOf(
                "3D Kingdom Explorer",
                "Spike & Host Receptors",
                "Pathogen Architect",
                "Virtual Human Test",
                "Treatment Strategy"
            ),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "path_tab"
        )

        when (subTab) {
            "3D Kingdom Explorer" -> {
                MultiKingdomPathogenExplorerPanel(
                    reduceMotion = reduceMotion,
                    bookmarks = bookmarks,
                    onToggleBookmark = onToggleBookmark,
                    onLoadVariantToHost = { variantDesign ->
                        design = variantDesign
                        subTab = "Virtual Human Test"
                    }
                )
            }

            "Spike & Host Receptors" -> {
                SpikeHostReceptorComparePanel(
                    reduceMotion = reduceMotion,
                    bookmarks = bookmarks,
                    onToggleBookmark = onToggleBookmark,
                    onSimulateSpikeInHost = { spikeDesign ->
                        design = spikeDesign
                        subTab = "Virtual Human Test"
                    }
                )
            }

            "Pathogen Architect" -> {
                Text("Real-World Reference Strains (Load & Modify)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                PillSelectorRow(
                    options = PathogenSimulatorEngine.REFERENCE_STRAINS.map { it.name },
                    selected = PathogenSimulatorEngine.REFERENCE_STRAINS.firstOrNull { it.id == design.id }?.name ?: "",
                    onSelect = { name ->
                        design = PathogenSimulatorEngine.REFERENCE_STRAINS.first { it.name == name }
                    },
                    tagPrefix = "path_ref"
                )

                Interactive3DScene(
                    scene = buildCustomPathogen3DScene(design),
                    reduceMotion = reduceMotion,
                    height = 225.dp
                )

                GlassCard {
                    OutlinedTextField(
                        value = design.name,
                        onValueChange = { design = design.copy(id = "custom", name = it) },
                        label = { Text("Custom Pathogen / Variant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("path_name_input")
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Pathogen Class", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                    PillSelectorRow(
                        options = PathogenClass.entries.map { it.label },
                        selected = design.pathogenClass.label,
                        onSelect = { lbl ->
                            val cls = PathogenClass.entries.first { it.label == lbl }
                            design = design.copy(id = "custom", pathogenClass = cls)
                        },
                        tagPrefix = "path_cls"
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Target Receptor / Tissue Tropism", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                    PillSelectorRow(
                        options = TropismTarget.entries.map { it.label },
                        selected = design.tropism.label,
                        onSelect = { lbl ->
                            val tr = TropismTarget.entries.first { it.label == lbl }
                            design = design.copy(id = "custom", tropism = tr)
                        },
                        tagPrefix = "path_trop"
                    )
                    if (design.referenceNotes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(design.referenceNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                GlassCard {
                    Text("Virulence, Infectivity & Escape Parameters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    PathogenSlider("Replication Rate", design.replicationRate, 0.4f..2.2f) {
                        design = design.copy(id = "custom", replicationRate = it)
                    }
                    PathogenSlider("Receptor Affinity (Entry / R₀)", design.receptorAffinity, 0.4f..2.2f) {
                        design = design.copy(id = "custom", receptorAffinity = it)
                    }
                    PathogenSlider("Innate IFN Evasion", design.immuneEvasion, 0.0f..1.5f) {
                        design = design.copy(id = "custom", immuneEvasion = it)
                    }
                    PathogenSlider("Antigenic Drift / Ab Escape", design.antigenicDrift, 0.0f..1.5f) {
                        design = design.copy(id = "custom", antigenicDrift = it)
                    }
                    PathogenSlider("Cytokine Storm Trigger (IL-6)", design.cytokineTrigger, 0.1f..1.8f) {
                        design = design.copy(id = "custom", cytokineTrigger = it)
                    }
                    PathogenSlider("Tissue Invasiveness", design.tissueInvasiveness, 0.1f..1.6f) {
                        design = design.copy(id = "custom", tissueInvasiveness = it)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { subTab = "Virtual Human Test" },
                        modifier = Modifier.weight(1f).testTag("path_test_host_btn")
                    ) {
                        Text("Infect Virtual Human →")
                    }
                    OutlinedButton(
                        onClick = onOpenD3Genomics,
                        modifier = Modifier.weight(1f).testTag("path_open_d3_btn")
                    ) {
                        Icon(Icons.Filled.Biotech, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("D3 Strain Map")
                    }
                }
            }

            "Virtual Human Test" -> {
                Text("Select Virtual Human Cohort", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                PillSelectorRow(
                    options = PathogenSimulatorEngine.HOST_PROFILES.map { it.label },
                    selected = host.label,
                    onSelect = { lbl -> host = PathogenSimulatorEngine.HOST_PROFILES.first { it.label == lbl } },
                    tagPrefix = "host_pick"
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${host.label} • Age ${host.ageYears}y", style = MaterialTheme.typography.titleSmall)
                        EvidencePill(simResult.severityTier)
                    }
                    Text(host.summary, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Projected R_eff: ${"%.2f".format(simResult.effectiveR0)} • Peak Load: ${"%.1f".format(simResult.peakLog10Load)} log10 (Day ${"%.1f".format(simResult.peakDay)})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Incubation: ~${"%.1f".format(simResult.incubationDays)}d • Contagious Shedding: ${"%.1f".format(simResult.infectiousWindowDays)}d • Peak Fever: ${"%.1f".format(simResult.peakFeverC)}°C • Max Organ Stress: ${"%.0f".format(simResult.maxOrganStressPct)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(simResult.clinicalOutcome, style = MaterialTheme.typography.bodySmall, color = semantic.established)
                    Text("Closest Real-World Profile: ${simResult.closestRealStrain}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }

                // Live 3D Virtual Human Target Organ Infection Scene
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D Host Infection Replay • Day ${"%.1f".format(currentStep.day)}", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(48.dp)) {
                            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play host replay", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Interactive3DScene(
                        scene = buildVirtualHostInfection3DScene(currentStep, design),
                        reduceMotion = reduceMotion,
                        height = 220.dp
                    )
                    Slider(
                        value = replayDay,
                        onValueChange = { isPlaying = false; replayDay = it },
                        valueRange = 0f..28f
                    )
                }

                PillSelectorRow(
                    options = listOf("Pathogen & Burden", "Immunity & Clearance", "Fever & Organ Stress"),
                    selected = chartMode,
                    onSelect = { chartMode = it },
                    tagPrefix = "host_chart"
                )
                val primary = MaterialTheme.colorScheme.primary
                val secondary = MaterialTheme.colorScheme.secondary
                val tertiary = MaterialTheme.colorScheme.tertiary
                val crit = semantic.critical
                val est = semantic.established
                val series = remember(simResult, chartMode) {
                    when (chartMode) {
                        "Pathogen & Burden" -> listOf(
                            ChartSeries("Treated Load (log10)", primary, simResult.series.map { it.day to it.log10ViralLoad }),
                            ChartSeries("Untreated Load", crit.copy(alpha = 0.65f), simResult.untreatedSeries.map { it.day to it.log10ViralLoad }),
                            ChartSeries("Shedding R_eff", tertiary, simResult.series.map { it.day to it.sheddingReff })
                        )
                        "Immunity & Clearance" -> listOf(
                            ChartSeries("Innate IFN-I", tertiary, simResult.series.map { it.day to it.innateIfn }),
                            ChartSeries("Neutralizing IgG", est, simResult.series.map { it.day to it.neutralizingAb }),
                            ChartSeries("CD8+ CTL", primary, simResult.series.map { it.day to it.effectorCtl })
                        )
                        else -> listOf(
                            ChartSeries("Organ Stress %", crit, simResult.series.map { it.day to it.organStressPct }),
                            ChartSeries("Untreated Stress %", secondary, simResult.untreatedSeries.map { it.day to it.organStressPct }),
                            ChartSeries("IL-6 Surge x20", tertiary, simResult.series.map { it.day to it.il6Cytokine * 20.0 })
                        )
                    }
                }
                LineChartCanvas(series = series, title = "$chartMode (28-Day Virtual Host)", markerX = currentStep.day)
            }

            "Treatment Strategy" -> {
                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Therapeutic Countermeasures", style = MaterialTheme.typography.titleMedium)
                        EvidencePill("-${"%.0f".format(simResult.burdenReductionPct)}% Load AUC")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Recommended class for ${design.pathogenClass.label}: ${design.pathogenClass.standardDrugClass}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))

                    TreatmentCheckRow(
                        label = "Direct Antiviral / Targeted Antibiotic",
                        checked = treatment.useDirectAntiviralOrAntibiotic,
                        onChecked = { treatment = treatment.copy(useDirectAntiviralOrAntibiotic = it) }
                    )
                    TreatmentCheckRow(
                        label = "Neutralizing Monoclonal Antibody / bNAb",
                        checked = treatment.useMonoclonalAntibody,
                        onChecked = { treatment = treatment.copy(useMonoclonalAntibody = it) }
                    )
                    TreatmentCheckRow(
                        label = "Host Immunomodulator (Anti-IL-6 / Corticosteroid)",
                        checked = treatment.useHostImmunomodulator,
                        onChecked = { treatment = treatment.copy(useHostImmunomodulator = it) }
                    )
                    TreatmentCheckRow(
                        label = "Supportive Endothelial / Oxygenation Care",
                        checked = treatment.useSupportiveCare,
                        onChecked = { treatment = treatment.copy(useSupportiveCare = it) }
                    )
                    Spacer(Modifier.height(6.dp))
                    PathogenSlider("Therapy Start Day", treatment.interventionDay, 0.0f..10.0f) {
                        treatment = treatment.copy(interventionDay = it)
                    }
                }

                GlassCard {
                    Text("Mechanistic Strategy Assessment", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Treated Peak Organ Stress: ${"%.0f".format(simResult.maxOrganStressPct)}% vs Untreated: ${"%.0f".format(simResult.untreatedMaxStressPct)}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (simResult.maxOrganStressPct < 30.0) semantic.established else semantic.critical
                    )
                    Spacer(Modifier.height(6.dp))
                    simResult.strategyAnalysis.forEach { bullet ->
                        Text("• $bullet", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PathogenSlider(label: String, value: Double, range: ClosedFloatingPointRange<Float>, onChange: (Double) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("%.2f".format(value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat().coerceIn(range.start, range.endInclusive),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range
        )
    }
}

@Composable
private fun TreatmentCheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
