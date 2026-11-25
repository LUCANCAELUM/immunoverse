package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.kb.PathogenKingdom
import com.example.data.kb.PathogenTaxonomyEntry
import com.example.data.kb.PathogenTaxonomyKb
import com.example.data.model.BookmarkItem
import com.example.domain.PathogenDesign
import com.example.domain.SpikeReceptorComparator
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay

@Composable
fun MultiKingdomPathogenExplorerPanel(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onLoadVariantToHost: (PathogenDesign) -> Unit
) {
    val semantic = LocalImmunoColors.current

    var selectedKingdom by remember { mutableStateOf(PathogenKingdom.VIRUS) }
    val specimen: PathogenTaxonomyEntry = remember(selectedKingdom) {
        PathogenTaxonomyKb.SPECIMENS.firstOrNull { it.kingdom == selectedKingdom }
            ?: PathogenTaxonomyKb.SPECIMENS.first()
    }

    var variantIndex by remember(specimen.id) { mutableIntStateOf(0) }
    var selectedPartId by remember(specimen.id) { mutableStateOf<String?>(specimen.parts.first().id) }
    var autoEvolve by remember(specimen.id) { mutableStateOf(false) }

    val activeVariant = specimen.variants[variantIndex.coerceIn(0, specimen.variants.lastIndex)]
    val activePart = specimen.parts.firstOrNull { it.id == selectedPartId } ?: specimen.parts.first()

    LaunchedEffect(autoEvolve, specimen.id, reduceMotion) {
        if (autoEvolve) {
            while (autoEvolve) {
                delay(if (reduceMotion) 1900L else 1250L)
                variantIndex = (variantIndex + 1) % specimen.variants.size
                selectedPartId = specimen.variants[variantIndex].mutatedPartId
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Pathogen Category Tabs (6 Biological Kingdoms)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        PillSelectorRow(
            options = PathogenKingdom.entries.map { it.label },
            selected = selectedKingdom.label,
            onSelect = { lbl ->
                selectedKingdom = PathogenKingdom.entries.first { it.label == lbl }
            },
            tagPrefix = "kingdom_tab"
        )

        // Pathogen Overview & Surface / Spike Summary Card
        val isPathogenSaved = bookmarks.any { it.itemId == specimen.id }
        GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(specimen.organismName, style = MaterialTheme.typography.titleLarge)
                    Text(specimen.morphologyTag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvidencePill(specimen.sizeScale)
                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                "pathogen",
                                specimen.id,
                                specimen.organismName,
                                "${specimen.kingdom.label} • ${specimen.morphologyTag}",
                                specimen.summary
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("bookmark_pathogen_${specimen.id}")
                    ) {
                        Icon(
                            imageVector = if (isPathogenSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save pathogen profile to My Library",
                            tint = if (isPathogenSaved) MaterialTheme.colorScheme.primary else semantic.mutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(specimen.summary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            val primarySurface = specimen.parts.first()
            Text(
                text = "Key Surface / Entry Protein (${primarySurface.label}): ${primarySurface.role}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Labeled 3D Morphology Explorer
        Interactive3DScene(
            scene = buildTaxonomyPathogen3DScene(specimen, activeVariant, selectedPartId),
            reduceMotion = reduceMotion,
            selectedId = selectedPartId,
            onSelectObject = { oid ->
                if (specimen.parts.any { it.id == oid }) {
                    selectedPartId = oid
                }
            },
            height = 270.dp
        )

        // Labeled Organelle & Surface Protein Selector
        GlassCard {
            Text("Inspect Labeled Surface & Internal Structures", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            PillSelectorRow(
                options = specimen.parts.map { it.label },
                selected = activePart.label,
                onSelect = { lbl ->
                    selectedPartId = specimen.parts.first { it.label == lbl }.id
                },
                tagPrefix = "pathogen_part"
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(activePart.label, style = MaterialTheme.typography.titleSmall)
                if (activePart.id == activeVariant.mutatedPartId) {
                    EvidencePill("ACTIVE MUTATION SITE")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(activePart.role, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
        }

        // Dedicated Subsection: Mutations, Generations, Variants & Strains
        val isVariantSaved = bookmarks.any { it.itemId == activeVariant.id }
        GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mutations, Generations, Variants & Strains", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvidencePill("Gen ${activeVariant.generation}")
                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                "variant",
                                activeVariant.id,
                                "${specimen.organismName}: ${activeVariant.strainLabel}",
                                activeVariant.lineageOrClade,
                                "${activeVariant.mutationSummary} • ${activeVariant.phenotypicShift}"
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("bookmark_variant_${activeVariant.id}")
                    ) {
                        Icon(
                            imageVector = if (isVariantSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save pathogen variant to My Library",
                            tint = if (isVariantSaved) MaterialTheme.colorScheme.secondary else semantic.mutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            PillSelectorRow(
                options = specimen.variants.map { it.strainLabel },
                selected = activeVariant.strainLabel,
                onSelect = { lbl ->
                    autoEvolve = false
                    val idx = specimen.variants.indexOfFirst { it.strainLabel == lbl }
                    if (idx >= 0) {
                        variantIndex = idx
                        selectedPartId = specimen.variants[idx].mutatedPartId
                    }
                },
                tagPrefix = "variant_gen"
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        autoEvolve = false
                        variantIndex = if (variantIndex > 0) variantIndex - 1 else specimen.variants.lastIndex
                        selectedPartId = specimen.variants[variantIndex].mutatedPartId
                    },
                    modifier = Modifier.weight(1f).testTag("prev_gen_btn")
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev Gen")
                }
                OutlinedButton(
                    onClick = { autoEvolve = !autoEvolve },
                    modifier = Modifier.weight(1f).testTag("auto_evolve_btn")
                ) {
                    Icon(if (autoEvolve) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (autoEvolve) "Pause" else "Evolve 0→4")
                }
                OutlinedButton(
                    onClick = {
                        autoEvolve = false
                        variantIndex = (variantIndex + 1) % specimen.variants.size
                        selectedPartId = specimen.variants[variantIndex].mutatedPartId
                    },
                    modifier = Modifier.weight(1f).testTag("next_gen_btn")
                ) {
                    Text("Next Gen")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${activeVariant.strainLabel} • Clade: ${activeVariant.lineageOrClade}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Key Mutations: ${activeVariant.mutationSummary}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            val mutPartLabel = specimen.parts.firstOrNull { it.id == activeVariant.mutatedPartId }?.label ?: activeVariant.mutatedPartId
            Text("Mutated 3D Structure: $mutPartLabel (highlighted red ★)", style = MaterialTheme.typography.labelSmall, color = semantic.critical)
            Spacer(Modifier.height(4.dp))
            Text(activeVariant.phenotypicShift, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Immune Escape: ${activeVariant.immuneEscapePct}%  •  Transmission Mult: ${"%.2f".format(activeVariant.transmissionMult)}×",
                style = MaterialTheme.typography.labelMedium,
                color = if (activeVariant.immuneEscapePct >= 65) semantic.critical else semantic.established
            )
            Text("Therapeutic Note: ${activeVariant.drugResistanceNote}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onLoadVariantToHost(activeVariant.designOverrides) },
                modifier = Modifier.fillMaxWidth().testTag("load_variant_host_btn")
            ) {
                Text("Infect Virtual Human with ${activeVariant.designOverrides.name} →")
            }
        }
    }
}

@Composable
fun SpikeHostReceptorComparePanel(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onSimulateSpikeInHost: (PathogenDesign) -> Unit
) {
    val semantic = LocalImmunoColors.current
    var selectedSpikeId by remember { mutableStateOf(SpikeReceptorComparator.VIRAL_SPIKES.first().id) }
    var selectedReceptorId by remember { mutableStateOf(SpikeReceptorComparator.HOST_RECEPTORS.first().id) }
    var dockPhase by remember { mutableFloatStateOf(0f) }

    val spike = remember(selectedSpikeId) {
        SpikeReceptorComparator.VIRAL_SPIKES.first { it.id == selectedSpikeId }
    }
    val receptor = remember(selectedReceptorId) {
        SpikeReceptorComparator.HOST_RECEPTORS.first { it.id == selectedReceptorId }
    }
    val comparison = remember(spike, receptor) {
        SpikeReceptorComparator.comparePair(spike, receptor)
    }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            while (true) {
                delay(70L)
                dockPhase = (dockPhase + 0.08f) % 6.283f
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Select Viral Spike / Envelope Protein", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        PillSelectorRow(
            options = SpikeReceptorComparator.VIRAL_SPIKES.map { it.virusName },
            selected = spike.virusName,
            onSelect = { name ->
                val hit = SpikeReceptorComparator.VIRAL_SPIKES.first { it.virusName == name }
                selectedSpikeId = hit.id
                selectedReceptorId = hit.cognateHostReceptorId
            },
            tagPrefix = "spike_pick"
        )

        Text("Select Host Cell Surface Receptor / Target", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        PillSelectorRow(
            options = SpikeReceptorComparator.HOST_RECEPTORS.map { it.receptorName },
            selected = receptor.receptorName,
            onSelect = { rName ->
                selectedReceptorId = SpikeReceptorComparator.HOST_RECEPTORS.first { it.receptorName == rName }.id
            },
            tagPrefix = "host_rec_pick"
        )

        Interactive3DScene(
            scene = SpikeReceptorComparator.buildSpikeHostDocking3DScene(spike, receptor, dockPhase),
            reduceMotion = reduceMotion,
            height = 245.dp
        )

        val spikePairId = "${spike.id}_${receptor.id}"
        val isSpikeSaved = bookmarks.any { it.itemId == spikePairId }
        GlassCard(accentColor = if (comparison.isCognateMatch) semantic.established else semantic.mutedText) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Spike–Host Compatibility Metrics", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvidencePill(if (comparison.isCognateMatch) "Cognate Lock-and-Key" else "Tropism Mismatch")
                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                "spike",
                                spikePairId,
                                "${spike.virusName} ${spike.spikeProteinName} vs ${receptor.receptorName}",
                                "Kd ${"%.1f".format(spike.kdNanomolar)} nM • PDB ${spike.pdbId}",
                                comparison.summaryInterpretation
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("bookmark_spike_btn")
                    ) {
                        Icon(
                            imageVector = if (isSpikeSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save spike comparison to My Library",
                            tint = if (isSpikeSaved) MaterialTheme.colorScheme.primary else semantic.mutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Structural Binding Score: ${"%.0f".format(comparison.bindingAffinityScore)}/100 (Kd ${"%.1f".format(spike.kdNanomolar)} nM) • Entry Efficiency: ${comparison.entryEfficiencyPct}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Modeled R_eff Index: ${"%.2f".format(comparison.projectedTransmissibilityReff)} • Ab Escape Risk: ${comparison.immuneEvasionRiskPct}% • Host Stress Burden Index: ${"%.0f".format(comparison.hostResponseLoadScore)}/100",
                style = MaterialTheme.typography.labelMedium,
                color = if (comparison.hostResponseLoadScore > 72.0) semantic.critical else semantic.established
            )
            Spacer(Modifier.height(6.dp))
            Text(comparison.summaryInterpretation, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Text("Viral Spike Profile", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${spike.spikeProteinName} (PDB ${spike.pdbId})", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(3.dp))
                Text("Class: ${spike.trimerClass}", style = MaterialTheme.typography.bodySmall)
                Text("Glycan Shield: ${spike.glycanShieldSites} N-sites", style = MaterialTheme.typography.bodySmall)
                Text("Co-factor: ${spike.secondaryCoFactor}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Text("Ab Target: ${spike.antibodyVulnerability}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Text("Host Surface Protein", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text("${receptor.receptorName} (PDB ${receptor.pdbId})", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(3.dp))
                Text("Family: ${receptor.molecularFamily}", style = MaterialTheme.typography.bodySmall)
                Text("Distribution: ${receptor.cellularDistribution}", style = MaterialTheme.typography.bodySmall)
                Text("Native Role: ${receptor.nativePhysiologicalRole}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Text("Host Defense: ${receptor.defenseResponseRole}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
            }
        }

        // Cross-Virus Comparative Benchmarking Table
        GlassCard {
            Text("Cross-Virus Spike Affinity & Evasion Benchmark", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            SpikeReceptorComparator.VIRAL_SPIKES.forEach { s ->
                val cognateRec = SpikeReceptorComparator.HOST_RECEPTORS.first { it.id == s.cognateHostReceptorId }
                val res = SpikeReceptorComparator.comparePair(s, cognateRec)
                Text(
                    text = "• ${s.virusName} (${s.pdbId}) → ${s.cognateHostReceptorName}: Kd ${s.kdNanomolar} nM | Escape ${res.immuneEvasionRiskPct}% | Index ${"%.0f".format(res.hostResponseLoadScore)}/100",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.id == spike.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onSimulateSpikeInHost(SpikeReceptorComparator.toPathogenDesign(spike)) },
                modifier = Modifier.fillMaxWidth().testTag("simulate_spike_host_btn")
            ) {
                Icon(Icons.Filled.Biotech, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Run ${spike.virusName} Kinetics in Virtual Human Host →")
            }
        }
    }
}
