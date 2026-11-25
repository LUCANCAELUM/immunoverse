package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AntagonistMechanism
import com.example.domain.DosingRoute
import com.example.domain.ElucidationEvaluation
import com.example.domain.PharmacologyEngine
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.ChartSeries
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.buildLabeledMolecule3DScene
import com.example.ui.theme.LocalImmunoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PharmacologyScreen(
    reduceMotion: Boolean,
    onOpenBio3D: (() -> Unit)? = null,
    onToast: (String) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("Structure Elucidation") }
    var selectedMolId by remember { mutableStateOf(PharmacologyEngine.MOLECULES.first().id) }
    val selectedMol = remember(selectedMolId) {
        PharmacologyEngine.MOLECULES.first { it.id == selectedMolId }
    }

    // Structure Elucidation state
    var blindedMode by remember { mutableStateOf(true) }
    var revealed by remember(selectedMolId) { mutableStateOf(false) }
    var completedExperiments by remember(selectedMolId) { mutableStateOf(setOf("HRMS")) }
    var activeExperiment by remember { mutableStateOf("HRMS") }
    var guessedFormula by remember(selectedMolId) { mutableStateOf("") }
    var guessedIhdText by remember(selectedMolId) { mutableStateOf("") }
    var guessedGroups by remember(selectedMolId) { mutableStateOf(emptySet<String>()) }
    var evalResult by remember(selectedMolId) { mutableStateOf<ElucidationEvaluation?>(null) }

    // Labeled 3D explorer state
    var highlightGroup by remember(selectedMolId) { mutableStateOf<String?>(null) }
    var selectedAtomId by remember(selectedMolId) { mutableStateOf<String?>(null) }

    // PD/PK virtual assay state
    var doseMg by remember(selectedMolId) { mutableDoubleStateOf(250.0) }
    var antagonistRatio by remember { mutableDoubleStateOf(3.0) }
    var antagonistMech by remember { mutableStateOf(AntagonistMechanism.COMPETITIVE) }
    var dosingRoute by remember { mutableStateOf(DosingRoute.ORAL) }

    val sampleDisplayTitle = if (subTab == "Structure Elucidation" && blindedMode && !revealed) {
        selectedMol.unknownCode
    } else {
        "${selectedMol.name} (${selectedMol.formula})"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Experimental Pharmacology Lab",
            subtitle = "Virtual structure determination (HRMS, FT-IR, NMR, wet-bench), labeled 3D molecules & PD/PK receptor assays.",
            badge = "Virtual Pharma"
        )

        BannerDisclaimer(
            text = "Virtual analytical spectroscopy & PD/PK modeling for education and hypothesis testing. Verify experimental assays in accredited wet labs.",
            isSimulation = true
        )

        PillSelectorRow(
            options = listOf("Structure Elucidation", "3D Molecules (Labeled)", "PD / PK Virtual Assay"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "pharma_tab"
        )

        // Molecule selector strip (shows unknown sample code when in blinded elucidation mode)
        val selectorLabels = remember(subTab, blindedMode, revealed) {
            PharmacologyEngine.MOLECULES.map { m ->
                if (subTab == "Structure Elucidation" && blindedMode && !revealed && m.id != selectedMolId) {
                    m.unknownCode
                } else if (subTab == "Structure Elucidation" && blindedMode && !revealed && m.id == selectedMolId) {
                    "${m.unknownCode} (Active)"
                } else {
                    m.name
                }
            }
        }
        val activeSelectorLabel = if (subTab == "Structure Elucidation" && blindedMode && !revealed) {
            "${selectedMol.unknownCode} (Active)"
        } else {
            selectedMol.name
        }

        PillSelectorRow(
            options = selectorLabels,
            selected = activeSelectorLabel,
            onSelect = { picked ->
                val idx = selectorLabels.indexOf(picked).coerceAtLeast(0)
                selectedMolId = PharmacologyEngine.MOLECULES[idx].id
            },
            tagPrefix = "pharma_mol_pick"
        )

        when (subTab) {
            "Structure Elucidation" -> {
                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Target Sample: $sampleDisplayTitle", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Assays Run: ${completedExperiments.size} / 6 • Class: ${if (blindedMode && !revealed) "Unknown Drug Candidate" else selectedMol.therapeuticClass}",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.mutedText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Blind Unknown", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = blindedMode,
                                onCheckedChange = {
                                    blindedMode = it
                                    if (!it) revealed = true
                                },
                                modifier = Modifier.testTag("pharma_blind_switch")
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Run Virtual Analytical Instruments:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    val expList = listOf("HRMS", "FT-IR", "1H NMR", "13C NMR", "Wet Bench", "pKa Titration")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expList.forEach { exp ->
                            val isActive = activeExperiment == exp
                            val isDone = exp in completedExperiments
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    activeExperiment = exp
                                    completedExperiments = completedExperiments + exp
                                },
                                label = {
                                    Text(if (isDone) "✓ $exp" else exp, maxLines = 1)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("pharma_exp_${exp.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }

                // Active Experiment Readout + Plot
                when (activeExperiment) {
                    "HRMS" -> {
                        GlassCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("High-Resolution Mass Spectrometry (ESI-MS)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                EvidencePill("Exact Mass")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Molecular Formula: ${selectedMol.formula} • Monoisotopic Mass: ${"%.4f".format(selectedMol.monoisotopicMass)} Da • IHD (Degrees of Unsaturation) = ${selectedMol.ihd}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            BarChartCanvas(
                                labels = selectedMol.msFragments.map { "m/z ${"%.0f".format(it.mz)}" },
                                values = selectedMol.msFragments.map { it.relIntensity },
                                title = "HRMS Fragmentation Relative Intensity (%)",
                                height = 155.dp
                            )
                            Spacer(Modifier.height(6.dp))
                            selectedMol.msFragments.forEach { frag ->
                                Text(
                                    "• m/z ${"%.2f".format(frag.mz)} (${frag.ionFormula}, ${"%.0f".format(frag.relIntensity)}%) — ${frag.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.mutedText
                                )
                            }
                        }
                    }

                    "FT-IR" -> {
                        val irCurve = remember(selectedMolId) { PharmacologyEngine.generateFtirCurve(selectedMol) }
                        LineChartCanvas(
                            series = listOf(ChartSeries("% Transmittance", MaterialTheme.colorScheme.primary, irCurve)),
                            title = "FT-IR Spectrum (500–4000 cm⁻¹) — %T",
                            height = 175.dp
                        )
                        GlassCard {
                            Text("Diagnostic IR Absorption Bands", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            selectedMol.irPeaks.forEach { pk ->
                                Text(
                                    "• ${"%.0f".format(pk.wavenumberCm1)} cm⁻¹ (${pk.intensity}, ${"%.0f".format(pk.transmittancePct)}%T): ${pk.assignment}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    "1H NMR" -> {
                        val nmrCurve = remember(selectedMolId) { PharmacologyEngine.generateHNmrCurve(selectedMol) }
                        LineChartCanvas(
                            series = listOf(ChartSeries("1H Resonance (ppm)", MaterialTheme.colorScheme.secondary, nmrCurve)),
                            title = "¹H NMR Chemical Shift Profile (0–12 ppm)",
                            height = 175.dp
                        )
                        GlassCard {
                            Text("¹H NMR Multiplet Table (400 MHz, DMSO-d6)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            selectedMol.hNmrPeaks.forEach { pk ->
                                val jText = if (pk.jHz > 0.0) ", J=${"%.1f".format(pk.jHz)} Hz" else ""
                                Text(
                                    "• δ ${"%.2f".format(pk.ppm)} ppm (${pk.multiplicity}, ${pk.protons}H$jText) — ${pk.assignment}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    "13C NMR" -> {
                        BarChartCanvas(
                            labels = selectedMol.cNmrPeaks.map { "${"%.0f".format(it.ppm)} ppm" },
                            values = selectedMol.cNmrPeaks.map { it.ppm },
                            title = "¹³C NMR Shifts (Carbon Environment Diagnostic)",
                            height = 165.dp
                        )
                        GlassCard {
                            Text("¹³C NMR Resonance Assignments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(4.dp))
                            selectedMol.cNmrPeaks.forEach { pk ->
                                Text("• δ ${"%.1f".format(pk.ppm)} ppm — ${pk.environment}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    "Wet Bench" -> {
                        GlassCard {
                            Text("Simulated Wet-Bench Functional Group Spot Tests", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            selectedMol.wetBenchTests.forEach { test ->
                                val badgeColor = if (test.positive) semantic.established else semantic.mutedText
                                Text(
                                    "${if (test.positive) "[POSITIVE]" else "[NEGATIVE]"} ${test.testName} (${test.reagent})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = badgeColor
                                )
                                Text("   Observation: ${test.observation}", style = MaterialTheme.typography.bodySmall)
                                Text("   Inference: ${test.inference}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }

                    else -> {
                        val titrCurve = remember(selectedMolId) { PharmacologyEngine.generateTitrationCurve(selectedMol) }
                        LineChartCanvas(
                            series = listOf(ChartSeries("Solution pH vs Equivalents", semantic.established, titrCurve)),
                            title = "Acid-Base pKa Titration Curve (0 → 2 Equivalents)",
                            markerX = 0.5,
                            height = 175.dp
                        )
                        GlassCard {
                            Text("Ionization & Physicochemical Profile", style = MaterialTheme.typography.titleSmall, color = semantic.established)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Measured pKa value(s): ${selectedMol.pkaValues.joinToString(", ")} • Half-equivalence (0.5 eq) confirms buffer pKa.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "cLogP: ${selectedMol.lipinski.logP} • TPSA: ${selectedMol.lipinski.tpsaA2} Å² • HBD ${selectedMol.lipinski.hbd} / HBA ${selectedMol.lipinski.hba}",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.mutedText
                            )
                        }
                    }
                }

                // Structure Deduction Workbench
                GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                    Text("Structure Determination Workbench", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Enter deduced formula & IHD from HRMS and select the functional groups confirmed by IR, NMR & wet-bench assays:",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = guessedFormula,
                            onValueChange = { guessedFormula = it.uppercase() },
                            label = { Text("Formula (e.g. C8H9NO2)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f).testTag("pharma_guess_formula")
                        )
                        OutlinedTextField(
                            value = guessedIhdText,
                            onValueChange = { guessedIhdText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("IHD") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f).testTag("pharma_guess_ihd")
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                guessedFormula = selectedMol.formula
                                guessedIhdText = selectedMol.ihd.toString()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Auto-Fill HRMS Values")
                        }
                        OutlinedButton(
                            onClick = {
                                revealed = true
                                onToast("Revealed ${selectedMol.name} — tap 3D button below to inspect!")
                            },
                            modifier = Modifier.weight(1f).testTag("pharma_reveal_btn")
                        ) {
                            Text("Reveal Answer")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Select Deduced Functional Groups:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PharmacologyEngine.ALL_FUNCTIONAL_GROUPS.forEach { grp ->
                            val picked = grp in guessedGroups
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    guessedGroups = if (picked) guessedGroups - grp else guessedGroups + grp
                                },
                                label = { Text(grp, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val res = PharmacologyEngine.evaluateElucidationHypothesis(
                                    target = selectedMol,
                                    guessedFormula = guessedFormula,
                                    guessedIhd = guessedIhdText.toIntOrNull(),
                                    selectedGroups = guessedGroups
                                )
                                evalResult = res
                                if (res.scorePct >= 90) revealed = true
                            },
                            modifier = Modifier.weight(1f).testTag("pharma_verify_hypothesis_btn")
                        ) {
                            Text("Verify Structure Hypothesis")
                        }
                        OutlinedButton(
                            onClick = {
                                revealed = true
                                subTab = "3D Molecules (Labeled)"
                            },
                            modifier = Modifier.weight(1f).testTag("pharma_open_solved_3d_btn")
                        ) {
                            Text("Inspect 3D Molecule")
                        }
                    }

                    evalResult?.let { res ->
                        Spacer(Modifier.height(8.dp))
                        EvidencePill("Elucidation Score: ${res.scorePct}%")
                        Spacer(Modifier.height(4.dp))
                        Text(res.summaryFeedback, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Formula: ${if (res.formulaMatched) "Match" else "Mismatch"} • IHD: ${if (res.ihdMatched) "Match" else "Mismatch"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (res.missedGroups.isNotEmpty()) {
                            Text("Missed groups: ${res.missedGroups.joinToString()}", style = MaterialTheme.typography.labelSmall, color = semantic.critical)
                        }
                    }

                    if (revealed || !blindedMode) {
                        Spacer(Modifier.height(8.dp))
                        GlassCard {
                            Text("Solved Identity: ${selectedMol.name} (${selectedMol.formula})", style = MaterialTheme.typography.titleSmall, color = semantic.established)
                            Text("IUPAC: ${selectedMol.iupacName}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                            Spacer(Modifier.height(4.dp))
                            Text("Target & Mechanism: ${selectedMol.targetReceptor} — ${selectedMol.mechanism}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            "3D Molecules (Labeled)" -> {
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${selectedMol.name} (${selectedMol.formula})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(selectedMol.iupacName, style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                        }
                        EvidencePill("Labeled 3D")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Highlight Functional Group in 3D:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    PillSelectorRow(
                        options = listOf("All Groups") + selectedMol.functionalGroupTags,
                        selected = highlightGroup ?: "All Groups",
                        onSelect = { highlightGroup = if (it == "All Groups") null else it },
                        tagPrefix = "mol3d_group"
                    )
                }

                val molScene = remember(selectedMolId, highlightGroup, selectedAtomId) {
                    buildLabeledMolecule3DScene(selectedMol, highlightGroup, selectedAtomId)
                }
                Interactive3DScene(
                    scene = molScene,
                    reduceMotion = reduceMotion,
                    selectedId = selectedAtomId,
                    onSelectObject = { selectedAtomId = it },
                    height = 310.dp
                )

                val activeAtom = selectedMol.atoms.firstOrNull { it.id == selectedAtomId }
                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        if (activeAtom != null) "Selected Atom Label: ${activeAtom.label}" else "Tap any sphere on the 3D molecule or select below to inspect",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (activeAtom != null) {
                        Text(
                            "Element: ${activeAtom.symbol} • Hybridization: ${activeAtom.hybridization} • Functional Moiety: ${activeAtom.functionalGroup}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Lipinski Rule of 5 & ADME Descriptors:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "MW ${"%.2f".format(selectedMol.lipinski.mw)} Da • cLogP ${selectedMol.lipinski.logP} • HBD ${selectedMol.lipinski.hbd} • HBA ${selectedMol.lipinski.hba} • TPSA ${selectedMol.lipinski.tpsaA2} Å² • BBB: ${if (selectedMol.lipinski.bbbPermeable) "Permeable" else "Peripheral"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Labeled Atom Directory:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedMol.atoms.forEach { atom ->
                            FilterChip(
                                selected = selectedAtomId == atom.id,
                                onClick = { selectedAtomId = atom.id },
                                label = { Text(atom.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    if (onOpenBio3D != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenBio3D,
                            modifier = Modifier.fillMaxWidth().testTag("pharma_open_bio3d_btn")
                        ) {
                            Text("Explore 3D DNA, RNA, Amino Acids & Proteins")
                        }
                    }
                }
            }

            else -> {
                val pdPk = remember(selectedMolId, doseMg, antagonistRatio, antagonistMech, dosingRoute) {
                    PharmacologyEngine.runPharmacodynamicsAndPk(
                        mol = selectedMol,
                        doseMg = doseMg,
                        antagonistRatio = antagonistRatio,
                        antagonistMechanism = antagonistMech,
                        route = dosingRoute
                    )
                }

                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Text("Virtual Dose-Response & PK Experiment: ${selectedMol.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Target: ${selectedMol.targetReceptor}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                    Spacer(Modifier.height(8.dp))

                    Text("Administered Dose: ${"%.0f".format(doseMg)} mg", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = doseMg.toFloat(),
                        onValueChange = { doseMg = it.toDouble() },
                        valueRange = 25f..1500f,
                        modifier = Modifier.testTag("pharma_dose_slider")
                    )

                    Text("Administration Route:", style = MaterialTheme.typography.labelMedium)
                    PillSelectorRow(
                        options = DosingRoute.entries.map { it.label },
                        selected = dosingRoute.label,
                        onSelect = { lbl -> dosingRoute = DosingRoute.entries.first { it.label == lbl } },
                        tagPrefix = "pharma_route"
                    )

                    Spacer(Modifier.height(6.dp))
                    Text("Antagonist Co-Administration:", style = MaterialTheme.typography.labelMedium)
                    PillSelectorRow(
                        options = AntagonistMechanism.entries.map { it.label },
                        selected = antagonistMech.label,
                        onSelect = { lbl -> antagonistMech = AntagonistMechanism.entries.first { it.label == lbl } },
                        tagPrefix = "pharma_antag"
                    )
                    if (antagonistMech != AntagonistMechanism.NONE) {
                        Spacer(Modifier.height(6.dp))
                        Text("Antagonist Ratio ([I] / Ki): ${"%.1f".format(antagonistRatio)}x", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = antagonistRatio.toFloat(),
                            onValueChange = { antagonistRatio = it.toDouble() },
                            valueRange = 0.5f..10f
                        )
                    }
                }

                LineChartCanvas(
                    series = buildList {
                        add(ChartSeries("Agonist Baseline (%)", MaterialTheme.colorScheme.primary, pdPk.baselineCurve))
                        if (antagonistMech != AntagonistMechanism.NONE) {
                            add(ChartSeries("+Antagonist (%)", semantic.critical, pdPk.modulatedCurve))
                        }
                    },
                    title = "Log Dose-Response Curve (% Receptor Effect vs Log₁₀[nM])",
                    height = 185.dp
                )

                LineChartCanvas(
                    series = listOf(
                        ChartSeries("Plasma Cp (mg/L)", Color(0xFF22E5FF), pdPk.pkCurve),
                        ChartSeries("MEC", semantic.established, pdPk.mecLine),
                        ChartSeries("MTC", semantic.critical, pdPk.mtcLine)
                    ),
                    title = "24h Pharmacokinetic Plasma Concentration Profile",
                    height = 185.dp
                )

                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PK / PD Readout", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        EvidencePill(if (pdPk.windowStatus.contains("Window")) "Established" else "Experimental")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(pdPk.windowStatus, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Apparent EC₅₀: ${"%.0f".format(pdPk.apparentEc50Nm)} nM • Emax: ${"%.1f".format(pdPk.apparentEmaxPct)}% • Schild DR: ${"%.2f".format(pdPk.schildRatio)}x • Therapeutic Index (LD50/ED50): ${"%.1f".format(pdPk.therapeuticIndex)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Cmax: ${"%.2f".format(pdPk.cMaxMgL)} mg/L at Tmax ${"%.1f".format(pdPk.tMaxHr)}h • AUC₀₋₂₄: ${"%.1f".format(pdPk.aucMgHrL)} mg·h/L • t½: ${"%.1f".format(pdPk.halfLifeHr)}h • CL: ${"%.1f".format(pdPk.clearanceLhr)} L/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(pdPk.clinicalInterpretation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
