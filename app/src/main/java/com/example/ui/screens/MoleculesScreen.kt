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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.BookmarkItem
import com.example.data.network.AlphaFoldSummary
import com.example.data.network.PdbStructureData
import com.example.data.network.ScientificApiClient
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.example.domain.PharmacologyEngine
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PathwayGraphCanvas
import com.example.ui.components.PdbColorMode
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.components.VerifiedPdbMetadataCard
import com.example.ui.components.buildLabeledMolecule3DScene
import com.example.ui.components.buildPdb3DScene
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoleculesScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?) -> Unit,
    onRecordRecent: (String) -> Unit,
    onOpenSignaling: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()

    var subTab by remember { mutableStateOf("Molecules & PDB") }
    var selectedMolId by remember { mutableStateOf(ImmunologyKb.MOLS[4].id) } // IgG default
    var pdbQuery by remember { mutableStateOf("1IGT") }
    var colorMode by remember { mutableStateOf(PdbColorMode.BY_CHAIN) }
    var isFetchingPdb by remember { mutableStateOf(false) }
    var pdbError by remember { mutableStateOf<String?>(null) }
    var pdbData by remember { mutableStateOf<PdbStructureData?>(null) }
    var afData by remember { mutableStateOf<AlphaFoldSummary?>(null) }
    var pickedResidue by remember { mutableStateOf<String?>(null) }

    var labeledMolId by remember { mutableStateOf(PharmacologyEngine.MOLECULES.first().id) }
    var highlightGroup by remember(labeledMolId) { mutableStateOf<String?>(null) }
    var selectedAtomId by remember(labeledMolId) { mutableStateOf<String?>(null) }

    var selectedPathwayId by remember { mutableStateOf(ImmunologyKb.PATHWAYS.first().id) }
    var inhibitedNodes by remember { mutableStateOf(emptySet<String>()) }

    val selectedMol = remember(selectedMolId) { ImmunologyKb.MOLS.first { it.id == selectedMolId } }
    val selectedPathway = remember(selectedPathwayId) { ImmunologyKb.PATHWAYS.first { it.id == selectedPathwayId } }
    val isBookmarked = bookmarks.any {
        (subTab == "Molecules & PDB" && it.itemId == selectedMol.id) ||
            (subTab == "Pathways" && it.itemId == selectedPathway.id)
    }

    fun loadStructure(pdbId: String, uniprotId: String?) {
        scope.launch {
            isFetchingPdb = true
            pdbError = null
            pickedResidue = null
            ScientificApiClient.fetchRcsbPdb(pdbId)
                .onSuccess { pdbData = it }
                .onFailure { pdbError = it.localizedMessage ?: "Failed to fetch PDB $pdbId" }
            if (!uniprotId.isNullOrBlank()) {
                ScientificApiClient.fetchAlphaFold(uniprotId)
                    .onSuccess { afData = it }
                    .onFailure { afData = null }
            } else {
                afData = null
            }
            isFetchingPdb = false
        }
    }

    LaunchedEffect(Unit) {
        if (pdbData == null && !isFetchingPdb) {
            loadStructure(pdbQuery, selectedMol.uniprotId)
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
            title = "Molecular Intelligence & Pathways",
            subtitle = "Live RCSB PDB C-α traces, AlphaFold metadata & 11 interactive signalling networks.",
            badge = "Live RCSB + AlphaFold",
            isBookmarked = isBookmarked,
            onToggleBookmark = {
                if (subTab == "Molecules & PDB") {
                    onToggleBookmark("molecule", selectedMol.id, selectedMol.name, selectedMol.pdbId)
                } else {
                    onToggleBookmark("pathway", selectedPathway.id, selectedPathway.name, selectedPathway.summary)
                }
            }
        )

        PillSelectorRow(
            options = listOf("Molecules & PDB", "3D Molecules (Labeled)", "Pathways"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "mol_tab"
        )

        if (subTab == "Molecules & PDB") {
            PillSelectorRow(
                options = ImmunologyKb.MOLS.map { it.name },
                selected = selectedMol.name,
                onSelect = { name ->
                    val found = ImmunologyKb.MOLS.first { it.name == name }
                    selectedMolId = found.id
                    found.pdbId?.let { pdbQuery = it }
                    onRecordRecent(found.name)
                },
                tagPrefix = "mol_pick"
            )

            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedMol.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    EvidencePill(selectedMol.status)
                }
                Spacer(Modifier.height(4.dp))
                Text("${selectedMol.category} • PDB: ${selectedMol.pdbId ?: "N/A"} • UniProt: ${selectedMol.uniprotId ?: "N/A"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(selectedMol.summary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("Domains: ${selectedMol.domains}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Text("Binding Sites: ${selectedMol.ligandSite}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pdbQuery,
                        onValueChange = { pdbQuery = it.uppercase().take(8) },
                        label = { Text("PDB ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("pdb_id_input")
                    )
                    Button(
                        onClick = { loadStructure(pdbQuery, selectedMol.uniprotId) },
                        enabled = !isFetchingPdb,
                        modifier = Modifier.testTag("fetch_pdb_btn")
                    ) {
                        Text(if (isFetchingPdb) "Loading..." else "Load 3D PDB")
                    }
                }
            }

            if (isFetchingPdb) {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Text("Downloading real atomic coordinates from files.rcsb.org & AlphaFold DB...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (pdbError != null) {
                BannerDisclaimer("RCSB PDB Error: $pdbError (Check internet connectivity)", isSimulation = false)
            }
            pdbData?.let { data ->
                PillSelectorRow(
                    options = PdbColorMode.entries.map { it.label },
                    selected = colorMode.label,
                    onSelect = { lbl -> colorMode = PdbColorMode.entries.first { it.label == lbl } },
                    tagPrefix = "mol_pdb_color"
                )
                Interactive3DScene(
                    scene = remember(data, colorMode) { buildPdb3DScene(data, colorMode = colorMode) },
                    reduceMotion = reduceMotion,
                    selectedId = pickedResidue,
                    onSelectObject = { pickedResidue = it },
                    height = 280.dp
                )
                VerifiedPdbMetadataCard(data = data, pickedResidue = pickedResidue)
                afData?.let { af ->
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("AlphaFold DB: ${af.entryId} (${af.gene}, ${af.organism})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    isFetchingPdb = true
                                    ScientificApiClient.fetchAlphaFoldStructure(af.uniprotId)
                                        .onSuccess { pdbData = it }
                                        .onFailure { pdbError = it.localizedMessage }
                                    isFetchingPdb = false
                                }
                            }) { Text("Load AF 3D") }
                        }
                    }
                }
            }
            Simulation3DSyllabusCard(
                unit = Simulation3DSyllabusKb.byId("pdb_structure_3d"),
                selectedObjectId = pickedResidue,
                onFocusObject = { pickedResidue = it }
            )
        } else if (subTab == "3D Molecules (Labeled)") {
            val activeSmallMol = remember(labeledMolId) {
                PharmacologyEngine.MOLECULES.first { it.id == labeledMolId }
            }
            PillSelectorRow(
                options = PharmacologyEngine.MOLECULES.map { it.name },
                selected = activeSmallMol.name,
                onSelect = { name ->
                    val found = PharmacologyEngine.MOLECULES.first { it.name == name }
                    labeledMolId = found.id
                    onRecordRecent("${found.name} (3D)")
                },
                tagPrefix = "mol_labeled_pick"
            )
            GlassCard {
                Text("${activeSmallMol.name} (${activeSmallMol.formula})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("${activeSmallMol.therapeuticClass} • Target: ${activeSmallMol.targetReceptor}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Spacer(Modifier.height(6.dp))
                PillSelectorRow(
                    options = listOf("All Groups") + activeSmallMol.functionalGroupTags,
                    selected = highlightGroup ?: "All Groups",
                    onSelect = { highlightGroup = if (it == "All Groups") null else it },
                    tagPrefix = "mol_screen_grp"
                )
            }
            val scene3d = remember(labeledMolId, highlightGroup, selectedAtomId) {
                buildLabeledMolecule3DScene(activeSmallMol, highlightGroup, selectedAtomId)
            }
            Interactive3DScene(
                scene = scene3d,
                reduceMotion = reduceMotion,
                selectedId = selectedAtomId,
                onSelectObject = { selectedAtomId = it },
                height = 300.dp
            )
            GlassCard {
                Text("Labeled Atom & Bond Directory (Tap to Focus in 3D)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val focused = activeSmallMol.atoms.firstOrNull { it.id == selectedAtomId }
                if (focused != null) {
                    Text(
                        "${focused.label} • Element ${focused.symbol} (${focused.hybridization}) • Group: ${focused.functionalGroup}",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.established
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSmallMol.atoms.forEach { atom ->
                        FilterChip(
                            selected = selectedAtomId == atom.id,
                            onClick = { selectedAtomId = atom.id },
                            label = { Text(atom.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        } else {
            PillSelectorRow(
                options = ImmunologyKb.PATHWAYS.map { it.name },
                selected = selectedPathway.name,
                onSelect = { name ->
                    selectedPathwayId = ImmunologyKb.PATHWAYS.first { it.name == name }.id
                    inhibitedNodes = emptySet()
                    onRecordRecent(name)
                },
                tagPrefix = "pathway_pick"
            )
            BannerDisclaimer("Tap any pathway node to simulate kinase/receptor inhibition (what-if downstream blockade).", isSimulation = true)
            PathwayGraphCanvas(
                pathway = selectedPathway,
                inhibitedNodeIds = inhibitedNodes,
                reduceMotion = reduceMotion,
                onToggleNode = { nid ->
                    inhibitedNodes = if (nid in inhibitedNodes) inhibitedNodes - nid else inhibitedNodes + nid
                }
            )
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedPathway.name, style = MaterialTheme.typography.titleLarge)
                    EvidencePill("Established")
                }
                Spacer(Modifier.height(4.dp))
                Text(selectedPathway.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Downstream Outcome: ${selectedPathway.outcome}", style = MaterialTheme.typography.bodySmall)
                if (inhibitedNodes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "What-If Blockade Active: Inhibited ${inhibitedNodes.joinToString()} -> downstream signalling attenuated.",
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.critical
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { inhibitedNodes = emptySet() }) { Text("Reset Inhibition") }
                }
                if (onOpenSignaling != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenSignaling,
                        modifier = Modifier.fillMaxWidth().testTag("mol_open_signaling_btn")
                    ) {
                        Text("Open Full Dynamic Biological-Data Canvas")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
