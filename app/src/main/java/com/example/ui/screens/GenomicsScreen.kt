package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.network.NcbiGeneHit
import com.example.data.network.ScientificApiClient
import com.example.domain.BioinformaticsEngine
import com.example.ui.components.BarChartCanvas
import com.example.ui.components.D3GenomicWebView
import com.example.ui.components.EvidencePill
import com.example.ui.components.GenomicSequenceBrowser
import com.example.ui.components.GenomicStrainVizBuilder
import com.example.ui.components.GlassCard
import com.example.ui.components.HeatmapCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PhylogenyTreeCanvas
import com.example.ui.components.PillSelectorRow
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun GenomicsScreen(
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current
    val scope = rememberCoroutineScope()

    var subTab by remember { mutableStateOf("Sequence & ORFs") }
    var fastaInput by remember { mutableStateOf(BioinformaticsEngine.DEMO_FASTA) }
    var multiFastaInput by remember { mutableStateOf(BioinformaticsEngine.DEMO_MULTI_FASTA) }
    var alignGlobal by remember { mutableStateOf(true) }
    var selectedEnzyme by remember { mutableStateOf(BioinformaticsEngine.RESTRICTION_ENZYMES.keys.first()) }
    var refCodon by remember { mutableStateOf("TAC") }
    var altCodon by remember { mutableStateOf("TAA") }

    var geneQuery by remember { mutableStateOf("FOXP3") }
    var geneHits by remember { mutableStateOf<List<NcbiGeneHit>>(emptyList()) }
    var geneLoading by remember { mutableStateOf(false) }
    var geneError by remember { mutableStateOf<String?>(null) }
    var showBlastDialog by remember { mutableStateOf(false) }

    val fastaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val txt = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (txt.isNotBlank()) {
                    fastaInput = txt.take(12000)
                    multiFastaInput = txt.take(12000)
                    onToast("Loaded FASTA file into Genomics workbench")
                }
            }.onFailure { onToast("Read error: ${it.localizedMessage}") }
        }
    }

    val records = remember(fastaInput) { BioinformaticsEngine.parseFasta(fastaInput) }
    val primarySeq = records.firstOrNull()?.sequence.orEmpty()
    val seqType = remember(primarySeq) { BioinformaticsEngine.detectSeqType(primarySeq) }
    val orfs = remember(primarySeq, seqType) {
        if (seqType == "DNA" || seqType == "RNA") BioinformaticsEngine.findOrfs(primarySeq, 10) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Genomics & Bioinformatics",
            subtitle = "FASTA, 6-frame ORFs, NW/SW alignment, JC69 UPGMA, protein hydropathy & NCBI Gene.",
            badge = "Computational Bio"
        )

        var d3DatasetMode by remember { mutableStateOf(GenomicStrainVizBuilder.PRESET_PANELS.first().title) }

        PillSelectorRow(
            options = listOf("Sequence & ORFs", "Align & Tree", "D3 Strain Viz", "Protein & SNV", "Expression & NCBI"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "gen_tab"
        )

        when (subTab) {
            "Sequence & ORFs" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { fastaPicker.launch(arrayOf("text/*", "application/octet-stream")) },
                        modifier = Modifier.weight(1f).testTag("gen_upload_fasta_btn")
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Load FASTA")
                    }
                    OutlinedButton(
                        onClick = { fastaInput = BioinformaticsEngine.DEMO_FASTA },
                        modifier = Modifier.weight(1f)
                    ) { Text("Load Demo IL2") }
                }
                OutlinedTextField(
                    value = fastaInput,
                    onValueChange = { fastaInput = it },
                    label = { Text("FASTA / Nucleotide / Protein Sequence") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("gen_fasta_input")
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Composition & Primer Metrics", style = MaterialTheme.typography.titleMedium)
                        EvidencePill(seqType)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Length: ${primarySeq.length} residues/bp | GC%: ${"%.1f".format(BioinformaticsEngine.gcContentPct(primarySeq))}% | Est. Tm: ${"%.1f".format(BioinformaticsEngine.estimateTmCelsius(primarySeq))} °C", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (seqType == "DNA" || seqType == "RNA") {
                        Spacer(Modifier.height(4.dp))
                        Text("5'->3' RevComp (first 60 nt): ${BioinformaticsEngine.revcomp(primarySeq).take(60)}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        Text("mRNA Transcript (first 60 nt): ${BioinformaticsEngine.transcribe(primarySeq).take(60)}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = semantic.mutedText)
                    }
                }

                if (seqType == "DNA" || seqType == "RNA") {
                    PillSelectorRow(
                        options = BioinformaticsEngine.RESTRICTION_ENZYMES.keys.toList(),
                        selected = selectedEnzyme,
                        onSelect = { selectedEnzyme = it },
                        tagPrefix = "gen_enz"
                    )
                    val frags = remember(primarySeq, selectedEnzyme) { BioinformaticsEngine.digestDna(primarySeq, selectedEnzyme) }
                    GenomicSequenceBrowser(
                        rawSequence = primarySeq,
                        sequenceTitle = records.firstOrNull()?.header?.takeIf { it.isNotBlank() } ?: "Interactive DNA Strand Browser",
                        orfs = orfs,
                        restrictionFragments = frags,
                        selectedEnzymeLabel = selectedEnzyme.substringBefore(" ")
                    )
                    GlassCard {
                        Text("Restriction Digest ($selectedEnzyme): ${frags.size} fragment(s)", style = MaterialTheme.typography.titleSmall)
                        Text(frags.joinToString(" | ") { "${it.lengthBp} bp (${it.start}..${it.end})" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                GlassCard {
                    Text("6-Frame ORF Finder (≥10 aa): ${orfs.size} candidate(s)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    if (orfs.isEmpty()) {
                        Text("No ORFs ≥10 aa detected. Paste an ATG-containing CDS or load Demo IL2.", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                    } else {
                        orfs.take(4).forEach { hit ->
                            Text("Frame ${hit.frame} (${hit.startNt}..${hit.endNt} nt, ${hit.aaLength} aa): ${hit.protein.take(48)}...", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            "Align & Tree" -> {
                OutlinedButton(
                    onClick = { multiFastaInput = BioinformaticsEngine.DEMO_MULTI_FASTA },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Load 4-Species Multi-FASTA Demo") }

                OutlinedTextField(
                    value = multiFastaInput,
                    onValueChange = { multiFastaInput = it },
                    label = { Text("Multi-FASTA Input (≥2 sequences)") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("gen_multifasta_input")
                )

                PillSelectorRow(
                    options = listOf("Needleman–Wunsch (Global)", "Smith–Waterman (Local)"),
                    selected = if (alignGlobal) "Needleman–Wunsch (Global)" else "Smith–Waterman (Local)",
                    onSelect = { alignGlobal = it.startsWith("Needleman") },
                    tagPrefix = "gen_aln"
                )

                val multi = remember(multiFastaInput) { BioinformaticsEngine.parseFasta(multiFastaInput) }
                if (multi.size >= 2) {
                    val aln = remember(multi, alignGlobal) {
                        BioinformaticsEngine.alignSequences(multi[0].sequence, multi[1].sequence, global = alignGlobal)
                    }
                    GlassCard {
                        Text("Pairwise Alignment: ${multi[0].header} vs ${multi[1].header}", style = MaterialTheme.typography.titleSmall)
                        Text("Score: ${aln.score} • Identity: ${"%.1f".format(aln.identityPct)}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(aln.alignedA.take(48), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        Text(aln.midline.take(48), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                        Text(aln.alignedB.take(48), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                    val tree = remember(multi) { BioinformaticsEngine.buildUpgmaTree(multi.take(6)) }
                    PhylogenyTreeCanvas(root = tree, height = 185.dp)
                    GlassCard {
                        Text("Jukes–Cantor UPGMA Newick:", style = MaterialTheme.typography.labelMedium)
                        Text("${BioinformaticsEngine.toNewick(tree)};", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = semantic.mutedText)
                    }
                }
            }

            "D3 Strain Viz" -> {
                val options = GenomicStrainVizBuilder.PRESET_PANELS.map { it.title } + "My Multi-FASTA Strains"
                PillSelectorRow(
                    options = options,
                    selected = d3DatasetMode,
                    onSelect = { d3DatasetMode = it },
                    tagPrefix = "gen_d3_mode"
                )
                val activePanel = remember(d3DatasetMode, multiFastaInput) {
                    if (d3DatasetMode == "My Multi-FASTA Strains") {
                        GenomicStrainVizBuilder.fromMultiFasta(BioinformaticsEngine.parseFasta(multiFastaInput))
                    } else {
                        GenomicStrainVizBuilder.PRESET_PANELS.firstOrNull { it.title == d3DatasetMode }
                            ?: GenomicStrainVizBuilder.PRESET_PANELS.first()
                    }
                }
                D3GenomicWebView(panel = activePanel)
            }

            "Protein & SNV" -> {
                val protSeq = remember(primarySeq, seqType, orfs) {
                    if (seqType == "Protein") primarySeq
                    else orfs.firstOrNull()?.protein ?: BioinformaticsEngine.translate(primarySeq)
                }
                val stats = remember(protSeq) { BioinformaticsEngine.analyzeProtein(protSeq) }
                GlassCard {
                    Text("Protein Physicochemical Profile (${stats.length} aa)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Molecular Weight: ${"%.1f".format(stats.molecularWeightDa)} Da (~${"%.2f".format(stats.molecularWeightDa / 1000.0)} kDa)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Est. Net Charge (pH 7): ${"%.1f".format(stats.netChargePh7)} | Mean Kyte–Doolittle GRAVY: ${"%.2f".format(stats.meanHydropathy)}", style = MaterialTheme.typography.bodySmall)
                }
                if (stats.profile.isNotEmpty()) {
                    BarChartCanvas(
                        labels = stats.profile.take(18).indices.map { "${it + 1}" },
                        values = stats.profile.take(18),
                        title = "Kyte–Doolittle Residue Hydropathy (First 18 aa)",
                        height = 160.dp
                    )
                }

                GlassCard {
                    Text("Codon SNV Effect Classifier", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = refCodon,
                            onValueChange = { refCodon = it.uppercase().take(3) },
                            label = { Text("Ref Codon") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("gen_ref_codon")
                        )
                        OutlinedTextField(
                            value = altCodon,
                            onValueChange = { altCodon = it.uppercase().take(3) },
                            label = { Text("Alt Codon") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("gen_alt_codon")
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val snv = BioinformaticsEngine.classifySnv(refCodon, altCodon)
                    if (snv != null) {
                        Text("${snv.refCodon} (${snv.refAa}) → ${snv.altCodon} (${snv.altAa}): ${snv.classification}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Enter two valid 3-base DNA/RNA codons (e.g. TAC -> TAA).", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                    }
                }
            }

            "Expression & NCBI" -> {
                HeatmapCanvas(
                    rowLabels = BioinformaticsEngine.DEMO_EXPRESSION_GENES,
                    colLabels = BioinformaticsEngine.DEMO_EXPRESSION_SAMPLES,
                    matrix = BioinformaticsEngine.DEMO_EXPRESSION_MATRIX,
                    title = "Immune Transcriptome Z-Score Heatmap (Illustrative Demo)",
                    height = 210.dp
                )

                GlassCard {
                    Text("Live NCBI Gene Summary & BLAST Handoff", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = geneQuery,
                            onValueChange = { geneQuery = it },
                            label = { Text("Human Gene Symbol (e.g. FOXP3, PDCD1)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("gen_ncbi_input")
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    geneLoading = true
                                    geneError = null
                                    ScientificApiClient.searchNcbiGene(geneQuery)
                                        .onSuccess { geneHits = it }
                                        .onFailure { geneError = it.localizedMessage ?: "NCBI lookup failed" }
                                    geneLoading = false
                                }
                            },
                            enabled = !geneLoading,
                            modifier = Modifier.testTag("gen_ncbi_search_btn")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Query")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showBlastDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("gen_blast_btn")
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Sequence in External NCBI BLAST")
                    }
                }

                if (geneLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Querying NCBI E-utilities...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (geneError != null) {
                    Text("NCBI error: $geneError", style = MaterialTheme.typography.bodySmall, color = semantic.critical)
                }
                geneHits.forEach { g ->
                    GlassCard {
                        Text("${g.symbol} — ${g.name} (Chr ${g.chromosome})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("GeneID: ${g.geneId} • ${g.organism}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                        Spacer(Modifier.height(4.dp))
                        Text(g.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showBlastDialog) {
        AlertDialog(
            onDismissRequest = { showBlastDialog = false },
            title = { Text("External NCBI BLAST Transmission Notice") },
            text = {
                Text("Opening NCBI Web BLAST in your browser will send the active sequence snippet externally to blast.ncbi.nlm.nih.gov. Proceed only with non-sensitive or educational sequences.")
            },
            confirmButton = {
                Button(onClick = {
                    showBlastDialog = false
                    val encSeq = URLEncoder.encode(primarySeq.take(1500), "UTF-8")
                    val url = "https://blast.ncbi.nlm.nih.gov/Blast.cgi?PROGRAM=blastn&PAGE_TYPE=BlastSearch&QUERY=$encSeq"
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        .onFailure { onToast("No browser available") }
                }) { Text("Consent & Open BLAST") }
            },
            dismissButton = { TextButton(onClick = { showBlastDialog = false }) { Text("Cancel") } }
        )
    }
}
