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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.network.PdbSeqMatch
import com.example.data.network.PdbStructureData
import com.example.data.network.ScientificApiClient
import com.example.domain.MolecularBio3DEngine
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PdbColorMode
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.VerifiedPdbMetadataCard
import com.example.ui.components.buildAminoAcid3DScene
import com.example.ui.components.buildDnaHelix3DScene
import com.example.ui.components.buildPdb3DScene
import com.example.ui.components.buildRnaFold3DScene
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.launch

@Composable
fun BioStructures3DScreen(
    reduceMotion: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()

    var subTab by remember { mutableStateOf("DNA 3D") }

    // DNA state
    var dnaSeq by remember { mutableStateOf(MolecularBio3DEngine.DNA_PRESETS.first().second) }
    var helixForm by remember { mutableStateOf("B-DNA") }
    var selectedDnaObj by remember { mutableStateOf<String?>(null) }

    // RNA state
    var rnaSeq by remember { mutableStateOf(MolecularBio3DEngine.RNA_PRESETS.first().second) }
    var selectedRnaObj by remember { mutableStateOf<String?>(null) }

    // Amino Acid state
    var selectedAaCode by remember { mutableStateOf('W') } // Tryptophan default
    var envPh by remember { mutableFloatStateOf(7.4f) }
    var selectedAaAtom by remember { mutableStateOf<String?>(null) }

    // Protein state (actual scientifically solved RCSB PDB & AlphaFold DB structures)
    var protSeq by remember { mutableStateOf(MolecularBio3DEngine.PROTEIN_PRESETS.first().second) }
    var protMode by remember { mutableStateOf("RCSB PDB & AlphaFold 3D") }
    var pdbId by remember { mutableStateOf("1UBQ") }
    var uniprotId by remember { mutableStateOf("P0CG48") }
    var colorMode by remember { mutableStateOf(PdbColorMode.SECONDARY_STRUCTURE) }
    var pdbLoading by remember { mutableStateOf(false) }
    var pdbError by remember { mutableStateOf<String?>(null) }
    var pdbData by remember { mutableStateOf<PdbStructureData?>(null) }
    var seqMatches by remember { mutableStateOf<List<PdbSeqMatch>>(emptyList()) }
    var pickedProtObj by remember { mutableStateOf<String?>(null) }

    fun fetchExperimentalPdb(targetId: String) {
        scope.launch {
            pdbLoading = true
            pdbError = null
            pickedProtObj = null
            ScientificApiClient.fetchRcsbPdb(targetId)
                .onSuccess { pdbData = it }
                .onFailure { pdbError = it.localizedMessage ?: "Failed to load PDB $targetId" }
            pdbLoading = false
        }
    }

    LaunchedEffect(subTab) {
        if (subTab == "Protein 3D & PDB" && pdbData == null && !pdbLoading) {
            fetchExperimentalPdb(pdbId)
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
            title = "3D Molecular Bio Studio",
            subtitle = "Interactive 3D visualization & structural analysis for DNA, RNA, Amino Acids & Proteins.",
            badge = "3D Structural Lab"
        )

        PillSelectorRow(
            options = listOf("DNA 3D", "RNA 3D", "Amino Acids 3D", "Protein 3D & PDB"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "bio3d_tab"
        )

        when (subTab) {
            "DNA 3D" -> {
                PillSelectorRow(
                    options = MolecularBio3DEngine.DNA_PRESETS.map { it.first },
                    selected = MolecularBio3DEngine.DNA_PRESETS.firstOrNull { it.second == dnaSeq }?.first ?: "",
                    onSelect = { name -> dnaSeq = MolecularBio3DEngine.DNA_PRESETS.first { it.first == name }.second },
                    tagPrefix = "dna_preset"
                )
                PillSelectorRow(
                    options = listOf("B-DNA", "A-DNA", "Z-DNA"),
                    selected = helixForm,
                    onSelect = { helixForm = it },
                    tagPrefix = "dna_form"
                )
                val dnaAnalysis = remember(dnaSeq, helixForm) { MolecularBio3DEngine.analyzeDna(dnaSeq, helixForm) }
                Interactive3DScene(
                    scene = remember(dnaAnalysis) { buildDnaHelix3DScene(dnaAnalysis) },
                    reduceMotion = reduceMotion,
                    selectedId = selectedDnaObj,
                    onSelectObject = { selectedDnaObj = it },
                    height = 250.dp
                )
                OutlinedTextField(
                    value = dnaSeq,
                    onValueChange = { dnaSeq = it.uppercase() },
                    label = { Text("5'→3' DNA Sequence (A, T, G, C)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("dna_seq_input")
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${dnaAnalysis.helixForm} Duplex Analysis", style = MaterialTheme.typography.titleMedium)
                        EvidencePill("${dnaAnalysis.lengthBp} bp")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "GC%: ${"%.1f".format(dnaAnalysis.gcPct)}% | AT%: ${"%.1f".format(dnaAnalysis.atPct)}% | Melting Tm: ${"%.1f".format(dnaAnalysis.tmCelsius)} °C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Total H-Bonds: ${dnaAnalysis.hBondsTotal} | CpG Sites: ${dnaAnalysis.cpgCount} | dsDNA MW: ${"%.2f".format(dnaAnalysis.dsMwKda)} kDa",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Helical Pitch: ${dnaAnalysis.helicalPitchAngstrom} Å (${dnaAnalysis.bpPerTurn} bp/turn)",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.mutedText
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("5' ${dnaAnalysis.cleanedSeq.take(36)} 3'", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text("3' ${dnaAnalysis.complementSeq.take(36)} 5'", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = semantic.established)
                }
            }

            "RNA 3D" -> {
                PillSelectorRow(
                    options = MolecularBio3DEngine.RNA_PRESETS.map { it.first },
                    selected = MolecularBio3DEngine.RNA_PRESETS.firstOrNull { it.second == rnaSeq }?.first ?: "",
                    onSelect = { name -> rnaSeq = MolecularBio3DEngine.RNA_PRESETS.first { it.first == name }.second },
                    tagPrefix = "rna_preset"
                )
                val rnaAnalysis = remember(rnaSeq) { MolecularBio3DEngine.analyzeRna(rnaSeq) }
                Interactive3DScene(
                    scene = remember(rnaAnalysis) { buildRnaFold3DScene(rnaAnalysis) },
                    reduceMotion = reduceMotion,
                    selectedId = selectedRnaObj,
                    onSelectObject = { selectedRnaObj = it },
                    height = 250.dp
                )
                OutlinedTextField(
                    value = rnaSeq,
                    onValueChange = { rnaSeq = it.uppercase() },
                    label = { Text("5'→3' RNA Sequence (A, U, G, C)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("rna_seq_input")
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RNA Secondary Fold & Translation", style = MaterialTheme.typography.titleMedium)
                        EvidencePill("ΔG ${"%.1f".format(rnaAnalysis.estimatedDeltaGKcal)} kcal/mol")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Length: ${rnaAnalysis.lengthNt} nt | GC%: ${"%.1f".format(rnaAnalysis.gcPct)}% | Watson–Crick Pairs: ${rnaAnalysis.wcPairs} | Wobble G·U: ${rnaAnalysis.wobbleGuPairs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Unpaired Loop/Bulge Bases: ${rnaAnalysis.unpairedCount}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Dot-Bracket: ${rnaAnalysis.dotBracket}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text(
                        "Frame-1 Peptide: ${rnaAnalysis.translatedPeptide.ifBlank { "(No coding triplets)" }}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = semantic.established
                    )
                }
            }

            "Amino Acids 3D" -> {
                val activeAa = remember(selectedAaCode) {
                    MolecularBio3DEngine.AMINO_ACIDS.first { it.code1 == selectedAaCode }
                }
                PillSelectorRow(
                    options = MolecularBio3DEngine.AMINO_ACIDS.map { "${it.code3} (${it.code1})" },
                    selected = "${activeAa.code3} (${activeAa.code1})",
                    onSelect = { lbl ->
                        selectedAaCode = MolecularBio3DEngine.AMINO_ACIDS.first { "${it.code3} (${it.code1})" == lbl }.code1
                    },
                    tagPrefix = "aa_pick"
                )
                Interactive3DScene(
                    scene = remember(activeAa) { buildAminoAcid3DScene(activeAa) },
                    reduceMotion = reduceMotion,
                    selectedId = selectedAaAtom,
                    onSelectObject = { selectedAaAtom = it },
                    height = 240.dp
                )
                val qAtPh = remember(activeAa, envPh) { MolecularBio3DEngine.chargeAtPh(activeAa, envPh.toDouble()) }
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${activeAa.name} (${activeAa.code3} / ${activeAa.code1})", style = MaterialTheme.typography.titleMedium)
                        EvidencePill(activeAa.category)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "MW: ${"%.2f".format(activeAa.mwDa)} Da | Isoelectric Point pI: ${"%.2f".format(activeAa.pI)} | Kyte–Doolittle: ${activeAa.hydropathy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "pKa1 (α-COOH): ${activeAa.pKa1} • pKa2 (α-NH3+): ${activeAa.pKa2}" +
                            (activeAa.pKaR?.let { " • pKaR (Side Chain): $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("mRNA Codons: ${activeAa.codons.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Environmental pH: ${"%.1f".format(envPh)}", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Net Zwitterion Charge: ${"%+.2f".format(qAtPh)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (qAtPh > 0.2) MaterialTheme.colorScheme.primary else if (qAtPh < -0.2) semantic.critical else semantic.established
                        )
                    }
                    Slider(value = envPh, onValueChange = { envPh = it }, valueRange = 1f..14f)
                    Text(activeAa.roleNote, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                }
            }

            "Protein 3D & PDB" -> {
                PillSelectorRow(
                    options = listOf("RCSB PDB & AlphaFold 3D", "Sequence → Real PDB Match"),
                    selected = protMode,
                    onSelect = { protMode = it },
                    tagPrefix = "prot_mode"
                )

                if (protMode == "RCSB PDB & AlphaFold 3D") {
                    val benchmarkPdbs = listOf(
                        "1UBQ (Ubiquitin 1.8Å)" to "1UBQ",
                        "1IGT (IgG2a 2.8Å)" to "1IGT",
                        "1AO7 (TCR-pMHC 2.6Å)" to "1AO7",
                        "6VXX (Spike Cryo-EM)" to "6VXX",
                        "1TNF (TNF-α Trimer)" to "1TNF"
                    )
                    PillSelectorRow(
                        options = benchmarkPdbs.map { it.first },
                        selected = benchmarkPdbs.firstOrNull { it.second == pdbId }?.first ?: "",
                        onSelect = { lbl ->
                            val chosen = benchmarkPdbs.first { it.first == lbl }.second
                            pdbId = chosen
                            fetchExperimentalPdb(chosen)
                        },
                        tagPrefix = "pdb_benchmark"
                    )
                    GlassCard {
                        Text("Scientifically Solved 3D Structures (RCSB PDB & AlphaFold DB)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pdbId,
                                onValueChange = { pdbId = it.uppercase().take(8) },
                                label = { Text("4-Char PDB ID (e.g. 1UBQ, 1IGT)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("bio3d_pdb_input")
                            )
                            Button(
                                onClick = { fetchExperimentalPdb(pdbId) },
                                enabled = !pdbLoading,
                                modifier = Modifier.testTag("bio3d_fetch_pdb_btn")
                            ) { Text("Load PDB") }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = uniprotId,
                                onValueChange = { uniprotId = it.uppercase().take(12) },
                                label = { Text("UniProt ID for AlphaFold 3D (e.g. P01579)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("bio3d_af_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pdbLoading = true
                                        pdbError = null
                                        pickedProtObj = null
                                        ScientificApiClient.fetchAlphaFoldStructure(uniprotId)
                                            .onSuccess { pdbData = it }
                                            .onFailure { pdbError = it.localizedMessage ?: "AlphaFold fetch failed" }
                                        pdbLoading = false
                                    }
                                },
                                enabled = !pdbLoading,
                                modifier = Modifier.testTag("bio3d_fetch_af_btn")
                            ) { Text("AlphaFold") }
                        }
                    }
                } else {
                    PillSelectorRow(
                        options = MolecularBio3DEngine.PROTEIN_PRESETS.map { it.first },
                        selected = MolecularBio3DEngine.PROTEIN_PRESETS.firstOrNull { it.second == protSeq }?.first ?: "",
                        onSelect = { name -> protSeq = MolecularBio3DEngine.PROTEIN_PRESETS.first { it.first == name }.second },
                        tagPrefix = "prot_preset"
                    )
                    val protAnalysis = remember(protSeq) { MolecularBio3DEngine.analyzeProteinFold(protSeq) }
                    OutlinedTextField(
                        value = protSeq,
                        onValueChange = { protSeq = it.uppercase() },
                        label = { Text("Polypeptide Sequence (Matches Experimental PDB Structures)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("prot_seq_input")
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                pdbLoading = true
                                pdbError = null
                                pickedProtObj = null
                                ScientificApiClient.searchRcsbBySequence(protSeq)
                                    .onSuccess { matches ->
                                        seqMatches = matches
                                        val top = matches.firstOrNull()
                                        if (top != null) {
                                            pdbId = top.pdbId
                                            ScientificApiClient.fetchRcsbPdb(top.pdbId)
                                                .onSuccess { pdbData = it }
                                                .onFailure { pdbError = it.localizedMessage }
                                        } else {
                                            pdbError = "No experimental PDB homolog >=30% identity found for this sequence."
                                        }
                                    }
                                    .onFailure { pdbError = it.localizedMessage ?: "Sequence search failed" }
                                pdbLoading = false
                            }
                        },
                        enabled = !pdbLoading,
                        modifier = Modifier.fillMaxWidth().testTag("bio3d_seq_match_btn")
                    ) { Text(if (pdbLoading) "Searching RCSB PDB..." else "Match Sequence to Verified Experimental PDB 3D") }

                    if (seqMatches.isNotEmpty()) {
                        PillSelectorRow(
                            options = seqMatches.map { "PDB ${it.pdbId}" },
                            selected = "PDB ${pdbData?.pdbId ?: seqMatches.first().pdbId}",
                            onSelect = { lbl ->
                                val pid = lbl.removePrefix("PDB ").trim()
                                pdbId = pid
                                fetchExperimentalPdb(pid)
                            },
                            tagPrefix = "bio3d_seq_hit"
                        )
                    }

                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sequence Chemistry & Composition", style = MaterialTheme.typography.titleMedium)
                            EvidencePill("${protAnalysis.lengthAa} aa")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "MW: ${"%.2f".format(protAnalysis.mwKda)} kDa | Theoretical pI: ${"%.2f".format(protAnalysis.theoreticalPi)} | Net Charge (pH 7.4): ${"%+.1f".format(protAnalysis.netChargePh74)} | GRAVY: ${"%.2f".format(protAnalysis.gravy)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (protAnalysis.hydropathyProfile.isNotEmpty()) {
                        BarChartCanvas(
                            labels = protAnalysis.hydropathyProfile.take(18).indices.map { "${it + 1}" },
                            values = protAnalysis.hydropathyProfile.take(18),
                            title = "Kyte–Doolittle Residue Hydropathy (First 18 aa)",
                            height = 145.dp
                        )
                    }
                }

                if (pdbLoading) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("Downloading verified experimental coordinates from RCSB PDB / AlphaFold DB...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                pdbError?.let { BannerDisclaimer("RCSB PDB / AlphaFold Error: $it", isSimulation = false) }
                pdbData?.let { data ->
                    PillSelectorRow(
                        options = PdbColorMode.entries.map { it.label },
                        selected = colorMode.label,
                        onSelect = { lbl -> colorMode = PdbColorMode.entries.first { it.label == lbl } },
                        tagPrefix = "bio3d_pdb_color"
                    )
                    Interactive3DScene(
                        scene = remember(data, colorMode) { buildPdb3DScene(data, colorMode = colorMode) },
                        reduceMotion = reduceMotion,
                        selectedId = pickedProtObj,
                        onSelectObject = { pickedProtObj = it },
                        height = 275.dp
                    )
                    VerifiedPdbMetadataCard(data = data, pickedResidue = pickedProtObj)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
