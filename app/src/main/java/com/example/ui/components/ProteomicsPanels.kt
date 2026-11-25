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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.domain.PeptideModType
import com.example.domain.ProteaseEnzyme
import com.example.domain.ProteomicsEngine
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper

@Composable
fun ProteomicsDigestPanel(
    sequence: String,
    proteinLabel: String,
    onSendPeptideToMsMs: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var enzyme by remember { mutableStateOf(ProteaseEnzyme.TRYPSIN) }
    var missedCleavages by remember { mutableIntStateOf(1) }
    var fixedCarbC by remember { mutableStateOf(true) }
    var varOxM by remember { mutableStateOf(false) }
    var varPhospho by remember { mutableStateOf(false) }
    var selectedPepSeq by remember { mutableStateOf<String?>(null) }

    val peptides = remember(sequence, enzyme, missedCleavages, fixedCarbC, varOxM, varPhospho) {
        ProteomicsEngine.digestProtein(
            rawSeq = sequence,
            enzyme = enzyme,
            maxMissedCleavages = missedCleavages,
            fixedCarbamidomethylC = fixedCarbC,
            variableOxidationM = varOxM,
            variablePhosphoSTY = varPhospho,
            minLength = 5
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("In Silico Proteolytic Digest ($proteinLabel)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                EvidencePill("${peptides.size} peptides")
            }
            Spacer(Modifier.height(6.dp))
            PillSelectorRow(
                options = ProteaseEnzyme.entries.map { it.label },
                selected = enzyme.label,
                onSelect = { lbl -> enzyme = ProteaseEnzyme.entries.first { it.label == lbl } },
                tagPrefix = "prot_enzyme"
            )
            Spacer(Modifier.height(4.dp))
            Text(enzyme.specificity, style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            Spacer(Modifier.height(6.dp))
            PillSelectorRow(
                options = listOf("0 Missed", "1 Missed", "2 Missed"),
                selected = "$missedCleavages Missed",
                onSelect = { missedCleavages = it.first().digitToIntOrNull() ?: 1 },
                tagPrefix = "prot_missed"
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fixed Carbamidomethyl Cys (+57.0215 Da)", style = MaterialTheme.typography.bodySmall)
                Switch(checked = fixedCarbC, onCheckedChange = { fixedCarbC = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Variable Met Oxidation (+15.9949 Da)", style = MaterialTheme.typography.bodySmall)
                Switch(checked = varOxM, onCheckedChange = { varOxM = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Phospho S/T/Y Search (+79.9663 Da)", style = MaterialTheme.typography.bodySmall)
                Switch(checked = varPhospho, onCheckedChange = { varPhospho = it })
            }
        }

        Ms1FingerprintCanvas(
            peptides = peptides,
            selectedSequence = selectedPepSeq,
            onSelectPeptide = { selectedPepSeq = it.sequence }
        )

        val picked = peptides.firstOrNull { it.sequence == selectedPepSeq } ?: peptides.firstOrNull()
        if (picked != null) {
            GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                Text("Selected Trypsin/Protease Fragment (${picked.startPos}–${picked.endPos})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(picked.sequence, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                Text(
                    "Mono: ${"%.4f".format(picked.neutralMonoMassDa)} Da • [M+H]+: ${"%.4f".format(picked.mzPlus1)} • [M+2H]2+: ${"%.4f".format(picked.mzPlus2)} • [M+3H]3+: ${"%.4f".format(picked.mzPlus3)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "RP-HPLC Index: ${"%.1f".format(picked.retentionIndex)} min • MC: ${picked.missedCleavages} • ${picked.modificationNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.mutedText
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSendPeptideToMsMs(picked.sequence) },
                        modifier = Modifier.testTag("send_to_msms_btn")
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fragment in MS/MS")
                    }
                    OutlinedButton(onClick = {
                        val csv = buildString {
                            appendLine("Start,End,MissedCleavages,Sequence,MonoMass_Da,MH1_mz,MH2_mz,MH3_mz,RP_HPLC_Index,Mods")
                            peptides.forEach { p ->
                                appendLine("${p.startPos},${p.endPos},${p.missedCleavages},${p.sequence},${"%.4f".format(p.neutralMonoMassDa)},${"%.4f".format(p.mzPlus1)},${"%.4f".format(p.mzPlus2)},${"%.4f".format(p.mzPlus3)},${"%.2f".format(p.retentionIndex)},\"${p.modificationNote}\"")
                            }
                        }
                        ExportHelper.shareTextFile(context, "digest_${proteinLabel}.csv", "text/csv", csv, "Export MS1 Digest CSV")
                            .onSuccess { onToast("Exported $it") }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CSV")
                    }
                }
            }
        }

        GlassCard {
            Text("Digested Peptide Table (Showing top ${peptides.take(16).size} of ${peptides.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            peptides.take(16).forEach { p ->
                GlassCard(onClick = { selectedPepSeq = p.sequence }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${p.startPos}–${p.endPos} (MC ${p.missedCleavages})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("[M+H]+ ${"%.3f".format(p.mzPlus1)} • 2+ ${"%.3f".format(p.mzPlus2)}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
                    }
                    Text(p.sequence, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ProteomicsMsMsPanel(
    initialPeptide: String,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var peptideInput by remember(initialPeptide) { mutableStateOf(initialPeptide.ifBlank { "TITLEVEPSDTIENVK" }) }
    var modType by remember { mutableStateOf(PeptideModType.CARBAMIDOMETHYL_C) }
    var tolPpm by remember { mutableDoubleStateOf(25.0) }

    val msms = remember(peptideInput, modType) {
        ProteomicsEngine.fragmentPeptideMsMs(peptideInput, modType)
    }

    var observedPeaksText by remember(msms.peptide, modType) {
        val sampleObs = msms.ions.filter { it.charge == 1 }.filterIndexed { idx, _ -> idx % 2 == 0 }
            .joinToString(", ") { "%.4f".format(it.mz + 0.002) }
        mutableStateOf(sampleObs)
    }

    val psmMatch = remember(msms, observedPeaksText, tolPpm) {
        ProteomicsEngine.matchObservedMsMsPeaks(msms, observedPeaksText, tolPpm)
    }
    val matchedKeys = remember(psmMatch) {
        psmMatch.matchedIons.map { "${it.first.series}${it.first.number}+${it.first.charge}" }.toSet()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard {
            Text("CID / HCD Tandem MS/MS Ion Calculator", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = peptideInput,
                onValueChange = { peptideInput = it.uppercase() },
                label = { Text("Peptide Sequence (e.g. TITLEVEPSDTIENVK)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("msms_peptide_input")
            )
            Spacer(Modifier.height(6.dp))
            PillSelectorRow(
                options = PeptideModType.entries.map { it.label },
                selected = modType.label,
                onSelect = { lbl -> modType = PeptideModType.entries.first { it.label == lbl } },
                tagPrefix = "msms_mod"
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Precursor Neutral: ${"%.4f".format(msms.precursorNeutralMass)} Da • [M+H]+: ${"%.4f".format(msms.precursorMh1)} • [M+2H]2+: ${"%.4f".format(msms.precursorMh2)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        MsMsFragmentSpectrumCanvas(
            ions = msms.ions,
            matchedIons = matchedKeys,
            peptide = msms.peptide
        )

        GlassCard(accentColor = semantic.established) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Experimental MS/MS Peak Matcher (PSM)", style = MaterialTheme.typography.titleSmall, color = semantic.established)
                EvidencePill("Hyperscore ${"%.1f".format(psmMatch.hyperscore)}")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Matched Ions: ${psmMatch.matchedCount} (b: ${psmMatch.matchedBIons}, y: ${psmMatch.matchedYIons}) • Backbone Coverage: ${"%.1f".format(psmMatch.sequenceCoveragePct)}% • Mean Error: ${"%.1f".format(psmMatch.meanAbsErrorPpm)} ppm",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            Text("Mass Error Tolerance: ${tolPpm.toInt()} ppm", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = tolPpm.toFloat(),
                onValueChange = { tolPpm = it.toDouble() },
                valueRange = 5f..100f
            )
            OutlinedTextField(
                value = observedPeaksText,
                onValueChange = { observedPeaksText = it },
                label = { Text("Observed Experimental MS/MS m/z Peaks (comma/space separated)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("msms_obs_peaks_input")
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                val csv = buildString {
                    appendLine("Series,Number,Charge,Theoretical_mz,Fragment")
                    msms.ions.forEach { ion ->
                        appendLine("${ion.series},${ion.number},${ion.charge},${"%.4f".format(ion.mz)},${ion.sequenceFragment}")
                    }
                }
                ExportHelper.shareTextFile(context, "msms_ions_${msms.peptide.take(10)}.csv", "text/csv", csv, "Export MS/MS Ions CSV")
                    .onSuccess { onToast("Exported $it") }
            }) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export Theoretical b/y Ions CSV")
            }
        }

        GlassCard {
            Text("b+ & y+ Fragment Ion Ladder", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            val bSingles = msms.ions.filter { it.series == "b" && it.charge == 1 }.sortedBy { it.number }
            val ySingles = msms.ions.filter { it.series == "y" && it.charge == 1 }.sortedBy { it.number }
            bSingles.zip(ySingles.reversed()).forEach { (bIon, yIon) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("b${bIon.number}: ${"%.3f".format(bIon.mz)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(bIon.sequenceFragment, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text("y${yIon.number}: ${"%.3f".format(yIon.mz)}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProteomicsPtmAndVolcanoPanel(
    sequence: String,
    onInspectUniProt: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    val ptms = remember(sequence) { ProteomicsEngine.scanPtms(sequence) }

    var csvInput by remember { mutableStateOf(ProteomicsEngine.DEMO_VOLCANO_CSV) }
    var fcCutoff by remember { mutableDoubleStateOf(1.0) }
    var pCutoff by remember { mutableDoubleStateOf(0.05) }
    var selectedProtein by remember { mutableStateOf<String?>(null) }

    val volcanoRows = remember(csvInput, fcCutoff, pCutoff) {
        ProteomicsEngine.parseDifferentialProteomicsCsv(csvInput, fcCutoff, pCutoff)
    }
    val upCount = volcanoRows.count { it.regulation == "Up" }
    val downCount = volcanoRows.count { it.regulation == "Down" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PTM & Signaling Motif Scanner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                EvidencePill("${ptms.size} motifs")
            }
            Spacer(Modifier.height(6.dp))
            if (ptms.isEmpty()) {
                Text("No canonical N-glycosylation, kinase, ITIM/ITAM, RGD, or SUMO motifs found in current sequence.", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
            } else {
                ptms.take(12).forEach { hit ->
                    Text(
                        "• [${hit.startPos}–${hit.endPos}] ${hit.matchedSeq} — ${hit.motifName} (${hit.category})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("  ${hit.biologicalNote}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                }
            }
        }

        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LFQ / TMT Differential Proteomics (BH-FDR)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                EvidencePill("↑$upCount  ↓$downCount")
            }
            Spacer(Modifier.height(6.dp))
            Text("Log2 Fold-Change Cutoff: ±${"%.1f".format(fcCutoff)}", style = MaterialTheme.typography.labelMedium)
            Slider(value = fcCutoff.toFloat(), onValueChange = { fcCutoff = it.toDouble() }, valueRange = 0.5f..2.5f)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.05, 0.01, 0.001).forEach { pv ->
                    FilterChip(
                        selected = pCutoff == pv,
                        onClick = { pCutoff = pv },
                        label = { Text("p ≤ $pv") },
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }
            }
        }

        VolcanoPlotCanvas(
            rows = volcanoRows,
            fcCutoff = fcCutoff,
            pCutoff = pCutoff,
            selectedProtein = selectedProtein,
            onSelect = { selectedProtein = it.proteinName }
        )

        val picked = volcanoRows.firstOrNull { it.proteinName == selectedProtein } ?: volcanoRows.firstOrNull { it.regulation != "NS" }
        if (picked != null) {
            GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${picked.proteinName} (${picked.uniprotId})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    EvidencePill(picked.regulation)
                }
                Text(
                    "Log2FC: ${"%+.2f".format(picked.log2Fc)} • p = ${"%.2e".format(picked.pValue)} • BH-FDR = ${"%.4f".format(picked.fdrBh)} (${picked.condition})",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (picked.uniprotId.isNotBlank() && picked.uniprotId != "—") {
                        Button(onClick = { onInspectUniProt(picked.uniprotId) }) {
                            Text("Fetch UniProt & 3D")
                        }
                    }
                    OutlinedButton(onClick = {
                        val outCsv = buildString {
                            appendLine("Gene,UniProt,Log2FC,PValue,BH_FDR,Regulation")
                            volcanoRows.forEach { r ->
                                appendLine("${r.proteinName},${r.uniprotId},${r.log2Fc},${r.pValue},${"%.5f".format(r.fdrBh)},${r.regulation}")
                            }
                        }
                        ExportHelper.shareTextFile(context, "volcano_fdr_proteomics.csv", "text/csv", outCsv, "Export Volcano FDR CSV")
                            .onSuccess { onToast("Exported $it") }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export FDR CSV")
                    }
                }
            }
        }

        GlassCard {
            OutlinedTextField(
                value = csvInput,
                onValueChange = { csvInput = it },
                label = { Text("Paste LFQ / TMT CSV (Gene,UniProt,Log2FC,PValue,Condition)") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth().testTag("volcano_csv_input")
            )
        }
    }
}
