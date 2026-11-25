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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.network.PdbSeqMatch
import com.example.data.network.PdbStructureData
import com.example.data.network.ScientificApiClient
import com.example.data.network.UniProtEntry
import com.example.data.network.UniProtSummary
import com.example.domain.ProteomicsEngine
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PdbColorMode
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.ProteomicsDigestPanel
import com.example.ui.components.ProteomicsMsMsPanel
import com.example.ui.components.ProteomicsPtmAndVolcanoPanel
import com.example.ui.components.TitrationCurveCanvas
import com.example.ui.components.VerifiedPdbMetadataCard
import com.example.ui.components.buildPdb3DScene
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProteomicsScreen(
    reduceMotion: Boolean,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()

    var subTab by remember { mutableStateOf("UniProt & 3D") }

    // Active protein sequence & benchmark state
    var selectedBenchmark by remember { mutableStateOf(ProteomicsEngine.BENCHMARK_PROTEINS.first()) }
    var activeProteinTitle by remember { mutableStateOf(selectedBenchmark.name) }
    var activeUniProtId by remember { mutableStateOf(selectedBenchmark.uniprotId) }
    var activeSequence by remember { mutableStateOf(selectedBenchmark.sequence) }

    // Live UniProt search & entry state
    var uniProtQuery by remember { mutableStateOf("IFNG human") }
    var uniProtHits by remember { mutableStateOf<List<UniProtSummary>>(emptyList()) }
    var uniProtEntry by remember { mutableStateOf<UniProtEntry?>(null) }
    var uniProtLoading by remember { mutableStateOf(false) }
    var uniProtError by remember { mutableStateOf<String?>(null) }

    // Experimental PDB / AlphaFold 3D state
    var pdbIdInput by remember { mutableStateOf(selectedBenchmark.pdbId) }
    var pdbData by remember { mutableStateOf<PdbStructureData?>(null) }
    var pdbLoading by remember { mutableStateOf(false) }
    var pdbError by remember { mutableStateOf<String?>(null) }
    var colorMode by remember { mutableStateOf(PdbColorMode.SECONDARY_STRUCTURE) }
    var pickedResidue by remember { mutableStateOf<String?>(null) }
    var seqPdbMatches by remember { mutableStateOf<List<PdbSeqMatch>>(emptyList()) }

    // MS/MS handoff peptide
    var msmsPeptide by remember { mutableStateOf("TITLEVEPSDTIENVK") }

    fun loadExperimentalPdb(pdbId: String) {
        scope.launch {
            pdbLoading = true
            pdbError = null
            pickedResidue = null
            ScientificApiClient.fetchRcsbPdb(pdbId)
                .onSuccess { pdbData = it }
                .onFailure { pdbError = it.localizedMessage ?: "Failed to load PDB $pdbId" }
            pdbLoading = false
        }
    }

    fun loadAlphaFold3D(uniprotAcc: String) {
        scope.launch {
            pdbLoading = true
            pdbError = null
            pickedResidue = null
            ScientificApiClient.fetchAlphaFoldStructure(uniprotAcc)
                .onSuccess { pdbData = it }
                .onFailure { pdbError = it.localizedMessage ?: "AlphaFold 3D unavailable for $uniprotAcc" }
            pdbLoading = false
        }
    }

    fun loadFullUniProt(acc: String) {
        scope.launch {
            uniProtLoading = true
            uniProtError = null
            ScientificApiClient.fetchUniProtEntry(acc)
                .onSuccess { entry ->
                    uniProtEntry = entry
                    activeUniProtId = entry.accession
                    activeProteinTitle = "${entry.proteinName} (${entry.geneName})"
                    if (entry.sequence.isNotBlank()) activeSequence = entry.sequence
                    val firstPdb = entry.pdbRefs.firstOrNull()?.pdbId
                    if (firstPdb != null) {
                        pdbIdInput = firstPdb
                        loadExperimentalPdb(firstPdb)
                    } else {
                        loadAlphaFold3D(entry.accession)
                    }
                }
                .onFailure { uniProtError = it.localizedMessage ?: "Failed to fetch UniProt $acc" }
            uniProtLoading = false
        }
    }

    // Automatically load verified empirical 1UBQ PDB structure on initial entry
    LaunchedEffect(Unit) {
        if (pdbData == null) {
            loadExperimentalPdb(selectedBenchmark.pdbId)
        }
    }

    val protParam = remember(activeSequence) { ProteomicsEngine.computeProtParam(activeSequence) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Proteomics Research Suite",
            subtitle = "UniProtKB, Verified 3D X-Ray/Cryo-EM/AlphaFold, ProtParam, Trypsin MS1, MS/MS b/y & Volcano FDR.",
            badge = "Live UniProt + PDB"
        )

        PillSelectorRow(
            options = listOf("UniProt & 3D", "ProtParam", "Digest & MS1", "MS/MS & PSM", "PTM & Volcano"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "proteomics_tab"
        )

        when (subTab) {
            "UniProt & 3D" -> {
                PillSelectorRow(
                    options = ProteomicsEngine.BENCHMARK_PROTEINS.map { "${it.gene} (${it.pdbId})" },
                    selected = "${selectedBenchmark.gene} (${selectedBenchmark.pdbId})",
                    onSelect = { lbl ->
                        val bm = ProteomicsEngine.BENCHMARK_PROTEINS.first { "${it.gene} (${it.pdbId})" == lbl }
                        selectedBenchmark = bm
                        activeProteinTitle = bm.name
                        activeUniProtId = bm.uniprotId
                        activeSequence = bm.sequence
                        pdbIdInput = bm.pdbId
                        loadExperimentalPdb(bm.pdbId)
                    },
                    tagPrefix = "proteomics_bm"
                )

                GlassCard {
                    Text("Live UniProtKB Search & Sequence Resolver", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uniProtQuery,
                            onValueChange = { uniProtQuery = it },
                            label = { Text("Protein / Gene / Accession") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("uniprot_search_input")
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    uniProtLoading = true
                                    uniProtError = null
                                    val clean = uniProtQuery.trim().uppercase()
                                    if (clean.matches(Regex("[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}"))) {
                                        loadFullUniProt(clean)
                                    } else {
                                        ScientificApiClient.searchUniProt(uniProtQuery)
                                            .onSuccess { uniProtHits = it }
                                            .onFailure { uniProtError = it.localizedMessage ?: "UniProt search failed" }
                                    }
                                    uniProtLoading = false
                                }
                            },
                            enabled = !uniProtLoading,
                            modifier = Modifier.testTag("uniprot_search_btn")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Search")
                        }
                    }
                    if (uniProtHits.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uniProtHits.take(6).forEach { hit ->
                                FilterChip(
                                    selected = activeUniProtId == hit.accession,
                                    onClick = { loadFullUniProt(hit.accession) },
                                    label = { Text("${hit.geneName} (${hit.accession}, ${hit.length}aa)") },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                )
                            }
                        }
                    }
                    uniProtError?.let {
                        Spacer(Modifier.height(4.dp))
                        BannerDisclaimer("UniProtKB: $it", isSimulation = false)
                    }
                }

                uniProtEntry?.let { entry ->
                    GlassCard(accentColor = MaterialTheme.colorScheme.secondary) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${entry.proteinName} (${entry.geneName})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            EvidencePill(entry.accession)
                        }
                        Text("${entry.organism} • ${entry.length} aa • ${"%.1f".format(entry.massDa / 1000.0)} kDa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.functionNote, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                        if (entry.subcellularLocations.isNotEmpty()) {
                            Spacer(Modifier.height(3.dp))
                            Text("Localization: ${entry.subcellularLocations.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
                        }
                        if (entry.pdbRefs.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Experimental PDB Structures (Tap to Load 3D):", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                entry.pdbRefs.take(8).forEach { ref ->
                                    FilterChip(
                                        selected = pdbData?.pdbId == ref.pdbId,
                                        onClick = {
                                            pdbIdInput = ref.pdbId
                                            loadExperimentalPdb(ref.pdbId)
                                        },
                                        label = { Text("${ref.pdbId} (${ref.resolution})") },
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                GlassCard {
                    Text("Verified Experimental 3D Protein Structure ($activeProteinTitle)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = pdbIdInput,
                            onValueChange = { pdbIdInput = it.uppercase().take(8) },
                            label = { Text("RCSB PDB ID (e.g. 1UBQ, 1FG9)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("proteomics_pdb_input")
                        )
                        Button(onClick = { loadExperimentalPdb(pdbIdInput) }, enabled = !pdbLoading) {
                            Text("PDB 3D")
                        }
                        OutlinedButton(onClick = { loadAlphaFold3D(activeUniProtId) }, enabled = !pdbLoading) {
                            Text("AlphaFold")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pdbLoading = true
                                    pdbError = null
                                    ScientificApiClient.searchRcsbBySequence(activeSequence)
                                        .onSuccess { matches ->
                                            seqPdbMatches = matches
                                            matches.firstOrNull()?.let {
                                                pdbIdInput = it.pdbId
                                                loadExperimentalPdb(it.pdbId)
                                            }
                                        }
                                        .onFailure { pdbError = it.localizedMessage ?: "Sequence PDB match failed" }
                                    pdbLoading = false
                                }
                            },
                            enabled = !pdbLoading
                        ) { Text("Find Experimental PDB by Sequence") }
                    }
                    if (seqPdbMatches.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            seqPdbMatches.forEach { m ->
                                FilterChip(
                                    selected = pdbData?.pdbId == m.pdbId,
                                    onClick = { pdbIdInput = m.pdbId; loadExperimentalPdb(m.pdbId) },
                                    label = { Text("Match ${m.pdbId}") },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                )
                            }
                        }
                    }
                }

                if (pdbLoading) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("Fetching verified atomic coordinates & HELIX/SHEET/SSBOND records...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                pdbError?.let { BannerDisclaimer("Structure Fetch Error: $it", isSimulation = false) }
                pdbData?.let { data ->
                    PillSelectorRow(
                        options = PdbColorMode.entries.map { it.label },
                        selected = colorMode.label,
                        onSelect = { lbl -> colorMode = PdbColorMode.entries.first { it.label == lbl } },
                        tagPrefix = "proteomics_pdb_color"
                    )
                    Interactive3DScene(
                        scene = remember(data, colorMode) { buildPdb3DScene(data, colorMode = colorMode) },
                        reduceMotion = reduceMotion,
                        selectedId = pickedResidue,
                        onSelectObject = { pickedResidue = it },
                        height = 285.dp
                    )
                    VerifiedPdbMetadataCard(data = data, pickedResidue = pickedResidue)
                }

                GlassCard {
                    OutlinedTextField(
                        value = activeSequence,
                        onValueChange = { activeSequence = it.uppercase() },
                        label = { Text("Active Protein Sequence / FASTA (${protParam.length} aa)") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("proteomics_seq_input")
                    )
                }
            }

            "ProtParam" -> {
                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ExPASy ProtParam ($activeProteinTitle)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        EvidencePill("${protParam.length} aa")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Avg MW: ${"%.2f".format(protParam.avgMassDa)} Da (${"%.2f".format(protParam.avgMassDa / 1000.0)} kDa) • Monoisotopic: ${"%.4f".format(protParam.monoisotopicMassDa)} Da",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Theoretical pI: ${"%.2f".format(protParam.theoreticalPi)} • Net Charge (pH 7.4): ${"%+.2f".format(protParam.netChargePh74)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Extinction Coeff ε₂₈₀: ${protParam.extCoeffOxidized} M⁻¹cm⁻¹ (S-S) / ${protParam.extCoeffReduced} (reduced) • Abs 0.1% (1 g/L): ${"%.3f".format(protParam.abs01Oxidized)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Aliphatic Index: ${"%.1f".format(protParam.aliphaticIndex)} • GRAVY: ${"%.3f".format(protParam.gravy)} • Instability Index: ${"%.1f".format(protParam.instabilityIndex)} (${if (protParam.isStable) "STABLE <40" else "UNSTABLE ≥40"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (protParam.isStable) semantic.established else semantic.critical
                    )
                    Text("Est. Half-Life: ${protParam.halfLifeMammalian}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        val rep = buildString {
                            appendLine("# ProtParam Report: $activeProteinTitle ($activeUniProtId)")
                            appendLine("Length: ${protParam.length} aa")
                            appendLine("Average MW: ${"%.2f".format(protParam.avgMassDa)} Da")
                            appendLine("Monoisotopic MW: ${"%.4f".format(protParam.monoisotopicMassDa)} Da")
                            appendLine("Theoretical pI: ${"%.2f".format(protParam.theoreticalPi)}")
                            appendLine("Extinction Coeff (oxidized/reduced): ${protParam.extCoeffOxidized} / ${protParam.extCoeffReduced} M-1cm-1")
                            appendLine("Instability Index: ${"%.2f".format(protParam.instabilityIndex)} (${if (protParam.isStable) "Stable" else "Unstable"})")
                            appendLine("Aliphatic Index: ${"%.2f".format(protParam.aliphaticIndex)}")
                            appendLine("GRAVY: ${"%.3f".format(protParam.gravy)}")
                            appendLine("Sequence:\n${protParam.sequence}")
                        }
                        ExportHelper.shareTextFile(context, "protparam_${activeUniProtId}.txt", "text/plain", rep, "Export ProtParam Report")
                            .onSuccess { onToast("Exported $it") }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export ProtParam Report")
                    }
                }

                TitrationCurveCanvas(curve = protParam.titrationCurve, theoreticalPi = protParam.theoreticalPi)

                val aaOrder = listOf('A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y')
                BarChartCanvas(
                    labels = aaOrder.map { it.toString() },
                    values = aaOrder.map { (protParam.counts[it] ?: 0).toDouble() },
                    title = "Amino Acid Composition (20 Canonical Residues)",
                    height = 175.dp
                )

                if (protParam.kyteDoolittleProfile.isNotEmpty()) {
                    BarChartCanvas(
                        labels = protParam.kyteDoolittleProfile.take(24).indices.map { "${it + 1}" },
                        values = protParam.kyteDoolittleProfile.take(24),
                        title = "Kyte–Doolittle Hydropathy Profile (7-aa Window, N-term 24 aa)",
                        height = 155.dp
                    )
                }
            }

            "Digest & MS1" -> {
                ProteomicsDigestPanel(
                    sequence = activeSequence,
                    proteinLabel = activeUniProtId,
                    onSendPeptideToMsMs = { pep ->
                        msmsPeptide = pep
                        subTab = "MS/MS & PSM"
                        onToast("Loaded $pep into Tandem MS/MS")
                    },
                    onToast = onToast
                )
            }

            "MS/MS & PSM" -> {
                ProteomicsMsMsPanel(
                    initialPeptide = msmsPeptide,
                    onToast = onToast
                )
            }

            "PTM & Volcano" -> {
                ProteomicsPtmAndVolcanoPanel(
                    sequence = activeSequence,
                    onInspectUniProt = { acc ->
                        subTab = "UniProt & 3D"
                        loadFullUniProt(acc)
                    },
                    onToast = onToast
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
