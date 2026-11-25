package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.kb.DiseaseKb
import com.example.data.kb.ImmunologyGlossaryKb
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.PathogenTaxonomyKb
import com.example.domain.PharmacologyEngine
import com.example.domain.ProteomicsEngine
import com.example.domain.SpikeReceptorComparator
import com.example.domain.VirtualLabEngine
import com.example.ui.theme.LocalImmunoColors

fun buildPlatformSearchIndex(): List<SearchHit> = buildList {
    // Pathogens & Multi-Kingdom Specimens + Variants
    PathogenTaxonomyKb.SPECIMENS.forEach { spec ->
        add(SearchHit(spec.organismName, "Pathogen (${spec.kingdom.label})", AppRoute.PATHOGEN_LAB, "${spec.morphologyTag} • ${spec.summary}"))
        spec.variants.forEach { v ->
            add(SearchHit("${spec.organismName}: ${v.strainLabel}", "Variant / Strain", AppRoute.PATHOGEN_LAB, "${v.mutationSummary} • ${v.phenotypicShift}"))
        }
    }
    // Viral Spike Proteins & Host Surface Receptors
    SpikeReceptorComparator.VIRAL_SPIKES.forEach { sp ->
        add(SearchHit("${sp.virusName} — ${sp.spikeProteinName}", "Viral Spike (${sp.pdbId})", AppRoute.PATHOGEN_LAB, "Target: ${sp.cognateHostReceptorName} • ${sp.tissueTropism}"))
    }
    SpikeReceptorComparator.HOST_RECEPTORS.forEach { rec ->
        add(SearchHit(rec.receptorName, "Host Surface Receptor", AppRoute.PATHOGEN_LAB, "${rec.molecularFamily} • ${rec.cellularDistribution}"))
    }
    // Cellular Processes (Live 3D Arena) & Organelles
    ImmunologyKb.PROCESSES.forEach { proc ->
        add(SearchHit(proc.name, "3D Cellular Process", AppRoute.CELLS, "${proc.summary} (${proc.steps.joinToString(" -> ")})"))
    }
    ImmunologyKb.ORGANELLES.forEach { org ->
        add(SearchHit(org.name, "Organelle 3D", AppRoute.CELLS, org.role))
    }
    // Immunology Concepts, Atlas, Molecules, Signaling, Glossary
    ImmunologyKb.ORGANS.forEach { add(SearchHit(it.name, "Immune Organ", AppRoute.ATLAS, it.summary)) }
    ImmunologyKb.CELLS.forEach { add(SearchHit(it.name, "Immune Cell", AppRoute.ATLAS, it.markers.joinToString())) }
    ImmunologyKb.MOLS.forEach { add(SearchHit(it.name, "Molecule ${it.pdbId ?: ""}", AppRoute.MOLECULES, it.summary)) }
    ImmunologyKb.PATHWAYS.forEach { add(SearchHit(it.name, "Signalling Pathway", AppRoute.SIGNALING, it.summary)) }
    ImmunologyGlossaryKb.TERMS.forEach { add(SearchHit("${it.term} (${it.acronym})", "Glossary Diagram", AppRoute.GLOSSARY, it.shortDefinition)) }
    DiseaseKb.DISEASES.forEach { add(SearchHit(it.name, "Disease Profile", AppRoute.DISEASES, it.agent)) }
    ProteomicsEngine.BENCHMARK_PROTEINS.forEach { p ->
        add(SearchHit(p.name, "Proteomics (${p.uniprotId})", AppRoute.PROTEOMICS, "${p.gene} • PDB ${p.pdbId} • ${p.role}"))
    }
    PharmacologyEngine.MOLECULES.forEach { add(SearchHit(it.name, "3D Molecule / Pharma", AppRoute.PHARMACOLOGY, "${it.formula} • ${it.therapeuticClass}")) }
    VirtualLabEngine.EXPERIMENTS.forEach { add(SearchHit(it.title, "Virtual Lab", AppRoute.VIRTUAL_LAB, it.category)) }
    add(SearchHit("Proteomics Research Suite (UniProt, Trypsin, MS/MS, Volcano)", "Proteomics Lab", AppRoute.PROTEOMICS, "UniProtKB, Verified 3D PDB/AlphaFold, ProtParam, b/y MS/MS & LFQ Volcano"))
    add(SearchHit("Experimental Pharmacology & Structure Elucidation", "Pharmacology Lab", AppRoute.PHARMACOLOGY, "HRMS, FT-IR, NMR, Wet-Bench, Labeled 3D Molecules & PD/PK"))
    add(SearchHit("DNA, RNA, Amino Acids & Verified PDB 3D", "3D Structural Bio", AppRoute.BIO_STRUCTURES_3D, "Double helix, RNA fold, 20 CPK amino acids & RCSB/AlphaFold 3D"))
    add(SearchHit("Genomic Sequence Browser & D3 Strain Map", "Genomics D3", AppRoute.GENOMICS, "Scroll/zoom DNA browser, ORFs, NW/SW, UPGMA, SNVs"))
    add(SearchHit("My Library (Saved Articles, Pathogens & Topics)", "Personal Collection", AppRoute.MY_LIBRARY, "Quick access to bookmarked papers, pathogens, spikes & topics"))
    AppRoute.entries.forEach { add(SearchHit(it.label, "Module", it, "Open ${it.label}")) }
}

@Composable
fun PlatformTopSearchBar(
    onNavigate: (AppRoute) -> Unit,
    libraryCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    var query by remember { mutableStateOf("") }
    val allHits = remember { buildPlatformSearchIndex() }
    val matches = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else allHits.filter {
            it.title.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.detail.lowercase().contains(q)
        }.take(8)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        "Search pathogens, cellular processes, spikes, or topics...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Platform search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(48.dp).testTag("top_search_clear_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                    unfocusedBorderColor = semantic.glassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("top_platform_search_bar")
            )
            IconButton(
                onClick = { onNavigate(AppRoute.MY_LIBRARY) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_my_library_btn")
            ) {
                BadgedBox(
                    badge = {
                        if (libraryCount > 0) {
                            Badge { Text(if (libraryCount > 99) "99+" else "$libraryCount") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CollectionsBookmark,
                        contentDescription = "Open My Library",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(visible = matches.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(matches) { hit ->
                        GlassCard(
                            onClick = {
                                onNavigate(hit.route)
                                query = ""
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    hit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    hit.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                hit.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
