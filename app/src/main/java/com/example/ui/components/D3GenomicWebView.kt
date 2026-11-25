package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.BioinformaticsEngine
import com.example.domain.FastaRecord
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

data class PathogenStrainPanel(
    val id: String,
    val title: String,
    val subtitle: String,
    val genomeLength: Int,
    val strains: List<String>,
    val domains: List<DomainBlock>,
    val mutations: List<StrainMutation>,
    val windows: List<WindowMetric>,
    val matrix: List<PairwiseMetric>
)

data class DomainBlock(val name: String, val start: Int, val end: Int, val role: String)
data class StrainMutation(
    val pos: Int,
    val label: String,
    val strain: String,
    val strainIdx: Int,
    val impact: Double,
    val type: String,
    val note: String,
    val isEscape: Boolean
)
data class WindowMetric(val pos: Int, val diversity: Double, val gcPct: Double)
data class PairwiseMetric(val row: String, val col: String, val identityPct: Double, val snps: Int)

object GenomicStrainVizBuilder {
    val PRESET_PANELS: List<PathogenStrainPanel> = listOf(
        buildCovidSpikePanel(),
        buildFluHaPanel(),
        buildHivEnvPanel()
    )

    fun fromMultiFasta(records: List<FastaRecord>): PathogenStrainPanel {
        val clean = records.take(6).ifEmpty {
            listOf(
                FastaRecord("Strain_Ref", "ATGGCGCTACTGAACTGCAAGTACCTGATCGATCGATCG"),
                FastaRecord("Strain_Var1", "ATGGCGGTACTGAACTGCAAGTACCTGTTCGATGGATCG")
            )
        }
        val ref = clean.first()
        val maxLen = clean.maxOf { it.sequence.length }.coerceAtLeast(30)
        val strains = clean.map { it.header.take(14) }
        val mutations = mutableListOf<StrainMutation>()

        clean.forEachIndexed { sIdx, rec ->
            if (sIdx > 0) {
                val cmpLen = min(ref.sequence.length, rec.sequence.length)
                for (i in 0 until cmpLen) {
                    if (ref.sequence[i] != rec.sequence[i] && mutations.size < 45) {
                        mutations += StrainMutation(
                            pos = i + 1,
                            label = "${ref.sequence[i]}${i + 1}${rec.sequence[i]}",
                            strain = strains[sIdx],
                            strainIdx = sIdx,
                            impact = 0.45 + ((i * 17) % 50) / 100.0,
                            type = "SNV",
                            note = "Substitution relative to ${strains[0]}",
                            isEscape = (i % 4 == 0)
                        )
                    }
                }
            }
        }

        val winSize = (maxLen / 12).coerceAtLeast(10)
        val windows = mutableListOf<WindowMetric>()
        var pos = 1
        while (pos <= maxLen) {
            val end = min(maxLen, pos + winSize)
            val slices = clean.map { it.sequence.substring(min(pos - 1, it.sequence.length), min(end, it.sequence.length)) }
            val avgGc = slices.map { BioinformaticsEngine.gcContentPct(it) }.average()
            var diffCount = 0
            var pairs = 0
            for (i in 0 until min(ref.sequence.length, end)) {
                if (i >= pos - 1) {
                    val colChars = clean.mapNotNull { it.sequence.getOrNull(i) }.distinct()
                    if (colChars.size > 1) diffCount++
                    pairs++
                }
            }
            val pi = if (pairs > 0) diffCount.toDouble() / pairs else 0.0
            windows += WindowMetric(pos, pi.coerceIn(0.0, 1.0), avgGc)
            pos += winSize
        }

        val matrix = mutableListOf<PairwiseMetric>()
        clean.forEachIndexed { r, a ->
            clean.forEachIndexed { c, b ->
                val len = min(a.sequence.length, b.sequence.length).coerceAtLeast(1)
                var snps = 0
                for (k in 0 until len) if (a.sequence[k] != b.sequence[k]) snps++
                val idPct = ((len - snps).toDouble() / len) * 100.0
                matrix += PairwiseMetric(strains[r], strains[c], idPct, snps)
            }
        }

        return PathogenStrainPanel(
            id = "custom_fasta",
            title = "Custom Multi-FASTA D3 Strain Analysis",
            subtitle = "${clean.size} sequences • ${mutations.size} SNVs detected vs ${strains[0]}",
            genomeLength = maxLen,
            strains = strains,
            domains = listOf(
                DomainBlock("5'-Region", 1, (maxLen * 0.35).toInt(), "N-terminal / promoter region"),
                DomainBlock("Core Motif", (maxLen * 0.35).toInt() + 1, (maxLen * 0.72).toInt(), "Central functional domain"),
                DomainBlock("3'-Region", (maxLen * 0.72).toInt() + 1, maxLen, "C-terminal / regulatory tail")
            ),
            mutations = mutations,
            windows = windows,
            matrix = matrix
        )
    }

    private fun buildCovidSpikePanel() = PathogenStrainPanel(
        id = "scov2_spike",
        title = "SARS-CoV-2 Spike Variants (D3.js Comparison)",
        subtitle = "1,273 aa Spike glycoprotein • Wuhan-Hu-1 vs Delta vs Omicron BA.1 vs JN.1",
        genomeLength = 1273,
        strains = listOf("Wuhan-Hu-1", "Delta", "Omicron BA.1", "Omicron JN.1"),
        domains = listOf(
            DomainBlock("NTD", 14, 305, "N-terminal domain (supersite neutralization)"),
            DomainBlock("RBD", 319, 541, "ACE2 receptor-binding domain (primary escape hotspot)"),
            DomainBlock("S1/S2", 675, 700, "Furin cleavage motif (P681R/H modulates syncytia)"),
            DomainBlock("S2 Fusion", 788, 1273, "Conserved heptad repeat fusion stalk")
        ),
        mutations = listOf(
            StrainMutation(452, "L452R", "Delta", 1, 0.85, "RBD Missense", "Boosts ACE2 binding & class 3 mAb escape", true),
            StrainMutation(478, "T478K", "Delta", 1, 0.72, "RBD Missense", "Increases electrostatic affinity for ACE2", true),
            StrainMutation(681, "P681R", "Delta", 1, 0.90, "Furin Site", "Accelerates furin cleavage & cell fusion", false),
            StrainMutation(417, "K417N", "Omicron BA.1", 2, 0.88, "RBD Escape", "Evades class 1 neutralizing antibodies", true),
            StrainMutation(484, "E484A", "Omicron BA.1", 2, 0.94, "RBD Escape", "Major class 2 mAb & convalescent escape", true),
            StrainMutation(501, "N501Y", "Omicron BA.1", 2, 0.86, "RBD Affinity", "High-affinity ACE2 salt-bridge stabilization", true),
            StrainMutation(346, "R346T", "Omicron JN.1", 3, 0.82, "RBD Escape", "Evades broad class 3 antibodies", true),
            StrainMutation(455, "L455S", "Omicron JN.1", 3, 0.98, "RBD Hallmark", "Signature JN.1 immune-evasion substitution", true),
            StrainMutation(486, "F486P", "Omicron JN.1", 3, 0.91, "RBD Escape", "Preserves ACE2 affinity while escaping serum", true),
            StrainMutation(614, "D614G", "Delta", 1, 0.65, "SD2 Hinge", "Shifts RBD into open 'up' conformation", false),
            StrainMutation(954, "Q954H", "Omicron BA.1", 2, 0.42, "HR1 S2", "Stabilizes pre-fusion trimer packing", false)
        ),
        windows = listOf(
            WindowMetric(60, 0.28, 42.0), WindowMetric(180, 0.52, 41.2), WindowMetric(300, 0.34, 44.5),
            WindowMetric(420, 0.88, 46.8), WindowMetric(520, 0.76, 45.2), WindowMetric(660, 0.48, 43.1),
            WindowMetric(820, 0.16, 40.4), WindowMetric(980, 0.19, 39.8), WindowMetric(1160, 0.12, 41.0)
        ),
        matrix = listOf(
            PairwiseMetric("Wuhan-Hu-1", "Wuhan-Hu-1", 100.0, 0), PairwiseMetric("Wuhan-Hu-1", "Delta", 98.9, 14),
            PairwiseMetric("Wuhan-Hu-1", "Omicron BA.1", 96.5, 44), PairwiseMetric("Wuhan-Hu-1", "Omicron JN.1", 94.2, 74),
            PairwiseMetric("Delta", "Wuhan-Hu-1", 98.9, 14), PairwiseMetric("Delta", "Delta", 100.0, 0),
            PairwiseMetric("Delta", "Omicron BA.1", 95.8, 53), PairwiseMetric("Delta", "Omicron JN.1", 93.6, 81),
            PairwiseMetric("Omicron BA.1", "Wuhan-Hu-1", 96.5, 44), PairwiseMetric("Omicron BA.1", "Delta", 95.8, 53),
            PairwiseMetric("Omicron BA.1", "Omicron BA.1", 100.0, 0), PairwiseMetric("Omicron BA.1", "Omicron JN.1", 96.1, 49),
            PairwiseMetric("Omicron JN.1", "Wuhan-Hu-1", 94.2, 74), PairwiseMetric("Omicron JN.1", "Delta", 93.6, 81),
            PairwiseMetric("Omicron JN.1", "Omicron BA.1", 96.1, 49), PairwiseMetric("Omicron JN.1", "Omicron JN.1", 100.0, 0)
        )
    )

    private fun buildFluHaPanel() = PathogenStrainPanel(
        id = "flu_ha",
        title = "Influenza A Hemagglutinin (H1N1 vs H3N2)",
        subtitle = "566 aa HA trimer comparing antigenic sites vs conserved HA2 stalk",
        genomeLength = 566,
        strains = listOf("H1N1 pdm09", "H1N1 2024", "H3N2 Victoria"),
        domains = listOf(
            DomainBlock("HA1 Head", 18, 327, "Sialic acid receptor binding & antigenic sites Sa/Sb/Ca/Cb"),
            DomainBlock("Cleavage", 328, 345, "Trypsin-like protease cleavage loop"),
            DomainBlock("HA2 Stalk", 346, 566, "Conserved fusion peptide & universal vaccine target")
        ),
        mutations = listOf(
            StrainMutation(155, "G155E", "H1N1 2024", 1, 0.76, "Antigenic Site Sa", "Shifts sialic-acid receptor specificity", true),
            StrainMutation(189, "K189R", "H1N1 2024", 1, 0.68, "Antigenic Site Sb", "Alters seasonal vaccine hemagglutination inhibition", true),
            StrainMutation(225, "D225G", "H3N2 Victoria", 2, 0.92, "220-Loop", "Broadens α2,3/α2,6 sialic-acid binding", true),
            StrainMutation(145, "N145K", "H3N2 Victoria", 2, 0.89, "Site A Drift", "Classic H3N2 cluster transition position", true),
            StrainMutation(412, "I412V", "H3N2 Victoria", 2, 0.30, "HA2 Stalk", "Minor conservative stalk polymorphism", false)
        ),
        windows = listOf(
            WindowMetric(40, 0.30, 44.0), WindowMetric(130, 0.78, 45.2), WindowMetric(210, 0.85, 43.8),
            WindowMetric(310, 0.42, 42.1), WindowMetric(420, 0.18, 40.5), WindowMetric(520, 0.14, 41.4)
        ),
        matrix = listOf(
            PairwiseMetric("H1N1 pdm09", "H1N1 pdm09", 100.0, 0), PairwiseMetric("H1N1 pdm09", "H1N1 2024", 94.5, 31),
            PairwiseMetric("H1N1 pdm09", "H3N2 Victoria", 62.4, 213),
            PairwiseMetric("H1N1 2024", "H1N1 pdm09", 94.5, 31), PairwiseMetric("H1N1 2024", "H1N1 2024", 100.0, 0),
            PairwiseMetric("H1N1 2024", "H3N2 Victoria", 61.8, 216),
            PairwiseMetric("H3N2 Victoria", "H1N1 pdm09", 62.4, 213), PairwiseMetric("H3N2 Victoria", "H1N1 2024", 61.8, 216),
            PairwiseMetric("H3N2 Victoria", "H3N2 Victoria", 100.0, 0)
        )
    )

    private fun buildHivEnvPanel() = PathogenStrainPanel(
        id = "hiv_env",
        title = "HIV-1 Env gp160 Clade Diversity",
        subtitle = "856 aa Env • Hypervariable V1–V5 loops vs conserved CD4bs & MPER",
        genomeLength = 856,
        strains = listOf("Clade B", "Clade C", "CRF01_AE"),
        domains = listOf(
            DomainBlock("V1/V2", 131, 196, "Trimer apex glycan shield"),
            DomainBlock("V3 Loop", 296, 331, "CCR5/CXCR4 coreceptor tropism determinant"),
            DomainBlock("CD4bs", 365, 475, "Conserved CD4 binding site (VRC01 bNAb target)"),
            DomainBlock("gp41 MPER", 660, 683, "Membrane-proximal external region")
        ),
        mutations = listOf(
            StrainMutation(160, "N160K", "Clade C", 1, 0.90, "V2 Apex", "Modulates PG9/PG16 bNAb glycan recognition", true),
            StrainMutation(306, "S306R", "CRF01_AE", 2, 0.86, "V3 Crown", "Influences CCR5 vs CXCR4 coreceptor switch", true),
            StrainMutation(332, "N332T", "CRF01_AE", 2, 0.94, "V3 Base Glycan", "Key high-mannose glycan supersite shift", true),
            StrainMutation(456, "R456W", "Clade C", 1, 0.62, "CD4bs Loop", "Partial CD4bs bNAb sensitivity shift", false)
        ),
        windows = listOf(
            WindowMetric(80, 0.44, 41.0), WindowMetric(165, 0.92, 39.2), WindowMetric(315, 0.86, 40.5),
            WindowMetric(430, 0.29, 43.2), WindowMetric(580, 0.35, 44.0), WindowMetric(740, 0.28, 45.1)
        ),
        matrix = listOf(
            PairwiseMetric("Clade B", "Clade B", 100.0, 0), PairwiseMetric("Clade B", "Clade C", 77.5, 193),
            PairwiseMetric("Clade B", "CRF01_AE", 75.8, 207),
            PairwiseMetric("Clade C", "Clade B", 77.5, 193), PairwiseMetric("Clade C", "Clade C", 100.0, 0),
            PairwiseMetric("Clade C", "CRF01_AE", 76.2, 204),
            PairwiseMetric("CRF01_AE", "Clade B", 75.8, 207), PairwiseMetric("CRF01_AE", "Clade C", 76.2, 204),
            PairwiseMetric("CRF01_AE", "CRF01_AE", 100.0, 0)
        )
    )

    fun toJson(panel: PathogenStrainPanel): String {
        val root = JSONObject()
        root.put("title", panel.title)
        root.put("subtitle", panel.subtitle)
        root.put("genomeLength", panel.genomeLength)
        root.put("strains", JSONArray(panel.strains))
        root.put("domains", JSONArray().apply {
            panel.domains.forEach { d ->
                put(JSONObject().apply {
                    put("name", d.name)
                    put("start", d.start)
                    put("end", d.end)
                    put("role", d.role)
                })
            }
        })
        root.put("mutations", JSONArray().apply {
            panel.mutations.forEach { m ->
                put(JSONObject().apply {
                    put("pos", m.pos)
                    put("label", m.label)
                    put("strain", m.strain)
                    put("strainIdx", m.strainIdx)
                    put("impact", m.impact)
                    put("type", m.type)
                    put("note", m.note)
                    put("isEscape", m.isEscape)
                })
            }
        })
        root.put("windows", JSONArray().apply {
            panel.windows.forEach { w ->
                put(JSONObject().apply {
                    put("pos", w.pos)
                    put("diversity", w.diversity)
                    put("gcPct", w.gcPct)
                })
            }
        })
        root.put("matrix", JSONArray().apply {
            panel.matrix.forEach { c ->
                put(JSONObject().apply {
                    put("row", c.row)
                    put("col", c.col)
                    put("identityPct", c.identityPct)
                    put("snps", c.snps)
                })
            }
        })
        return root.toString()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3GenomicWebView(
    panel: PathogenStrainPanel,
    modifier: Modifier = Modifier
) {
    val payloadJson = remember(panel) { GenomicStrainVizBuilder.toJson(panel) }
    var pageReady by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("D3.js Interactive Strain Explorer", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                EvidencePill("D3.js v7")
            }
            Spacer(Modifier.height(4.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(620.dp),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = false
                        setBackgroundColor(android.graphics.Color.parseColor("#0B1120"))
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageReady = true
                                val quoted = JSONObject.quote(payloadJson)
                                view?.evaluateJavascript("window.renderGenomicsViz($quoted);", null)
                            }
                        }
                        loadUrl("file:///android_asset/d3_genomics_viz.html")
                    }
                },
                update = { webView ->
                    if (pageReady) {
                        val quoted = JSONObject.quote(payloadJson)
                        webView.evaluateJavascript("window.renderGenomicsViz($quoted);", null)
                    }
                }
            )
        }
    }
}
