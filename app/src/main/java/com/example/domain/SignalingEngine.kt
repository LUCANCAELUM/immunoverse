package com.example.domain

import com.example.data.kb.ImmunologyKb
import com.example.data.kb.PathwayInfo
import kotlin.math.exp

enum class NodePerturbation(val label: String) {
    NORMAL("Normal"),
    INHIBITED("Inhibited (-KO)"),
    HYPERACTIVE("Hyperactive (2x)")
}

data class CellSignalingContext(
    val id: String,
    val name: String,
    val lineage: String,
    val receptorProfile: String,
    val pathwayAffinities: Map<String, Double>,
    val nodeMultipliers: Map<String, Double>
)

data class BioConditionDataset(
    val id: String,
    val name: String,
    val summary: String,
    val stimulusDrive: Double,
    val feedbackGain: Double,
    val nodeBoosts: Map<String, Double>,
    val downstreamFocus: String
)

data class SignalingTimePoint(
    val minute: Double,
    val receptorKinase: Double,
    val cytosolicAdaptor: Double,
    val nuclearEffector: Double
)

data class SignalingSimulationResult(
    val nodeActivations: Map<String, Double>,
    val edgeFluxes: Map<String, Double>,
    val timeSeries: List<SignalingTimePoint>,
    val downstreamOutputs: List<Pair<String, Double>>,
    val matchedCsvNodes: Int,
    val peakActivation: Double,
    val summaryText: String
)

object SignalingEngine {

    val DEMO_BIO_CSV = """
        node_or_gene,fold_expression,assay_note
        cyt,1.85,Serum IL-2 / IFN-gamma burst
        jak,1.45,Phospho-JAK1/2 Y1022/1023
        stat,1.65,Phospho-STAT1/5 nuclear translocation
        socs,0.35,SOCS1 methylation dampening
        tlr,1.75,LPS challenge TLR4 surface dimer
        ikk,1.40,Phospho-IKKbeta Ser177
        lck,1.60,CD3 ITAM phosphorylation
        zap70,1.50,TCR synapse recruitment
        btk,1.35,BCR signalosome phospho-BTK
        pi3k_n,1.55,PIP3 membrane accumulation
        pten,0.30,PTEN attenuated in activated effector
        nlrp3,1.70,ASC speck priming & K+ efflux
        irf3,1.80,Type-I IFN viral sensor burst
    """.trimIndent()

    val CELL_CONTEXTS = listOf(
        CellSignalingContext(
            id = "th1",
            name = "CD4+ Th1 Cell",
            lineage = "Adaptive",
            receptorProfile = "High TCR/CD28, IFNGR1/2, IL-12R, IL-2R",
            pathwayAffinities = mapOf("tcr_sig" to 1.25, "jak_stat" to 1.25, "mapk" to 1.15, "pi3k" to 1.15, "bcr_sig" to 0.25),
            nodeMultipliers = mapOf("pmhc" to 1.25, "lck" to 1.25, "zap70" to 1.2, "cyt" to 1.2, "jak" to 1.2, "cd28" to 1.2, "ag" to 0.2, "btk" to 0.2)
        ),
        CellSignalingContext(
            id = "ctl",
            name = "CD8+ CTL",
            lineage = "Adaptive",
            receptorProfile = "High TCR/CD8, IL-2R, IFNAR, Fas/Perforin axis",
            pathwayAffinities = mapOf("tcr_sig" to 1.3, "mapk" to 1.2, "pi3k" to 1.2, "apoptosis_p" to 1.15, "ifn_sig" to 1.15, "bcr_sig" to 0.2),
            nodeMultipliers = mapOf("pmhc" to 1.3, "lck" to 1.25, "erk" to 1.2, "mtor" to 1.2, "casp8" to 1.15, "ag" to 0.2)
        ),
        CellSignalingContext(
            id = "bcell",
            name = "B Lymphocyte",
            lineage = "Adaptive",
            receptorProfile = "High BCR (IgM/IgD), CD19/CD21, BTK, NF-kB, IL-4R",
            pathwayAffinities = mapOf("bcr_sig" to 1.35, "pi3k" to 1.2, "nfkb" to 1.15, "jak_stat" to 1.1, "tcr_sig" to 0.25),
            nodeMultipliers = mapOf("ag" to 1.35, "syk" to 1.3, "btk" to 1.35, "plcg2" to 1.25, "pmhc" to 0.2, "lck" to 0.2)
        ),
        CellSignalingContext(
            id = "mac",
            name = "M1 Macrophage",
            lineage = "Innate",
            receptorProfile = "Very high TLR4, IFNGR, NLRP3, FcR, Complement CR3",
            pathwayAffinities = mapOf("nfkb" to 1.35, "inflammasome" to 1.35, "jak_stat" to 1.2, "complement" to 1.2, "tcr_sig" to 0.2, "bcr_sig" to 0.2),
            nodeMultipliers = mapOf("tlr" to 1.35, "myd88" to 1.3, "nlrp3" to 1.35, "casp1" to 1.3, "jak" to 1.2, "pmhc" to 0.2, "ag" to 0.2)
        ),
        CellSignalingContext(
            id = "nk",
            name = "NK Cell",
            lineage = "Innate Lymphoid",
            receptorProfile = "High IL-15R/JAK1-3, IFNAR, NKG2D, FasL/Granzyme",
            pathwayAffinities = mapOf("jak_stat" to 1.3, "ifn_sig" to 1.25, "mapk" to 1.2, "apoptosis_p" to 1.2, "bcr_sig" to 0.2),
            nodeMultipliers = mapOf("cyt" to 1.25, "jak" to 1.25, "tbk1" to 1.15, "fas" to 1.2, "ag" to 0.2)
        ),
        CellSignalingContext(
            id = "dc",
            name = "Dendritic Cell",
            lineage = "APC Sentinel",
            receptorProfile = "High MHC I/II, TLR3/4/7, cGAS-STING, NLRP3",
            pathwayAffinities = mapOf("ag_pres" to 1.35, "ifn_sig" to 1.3, "nfkb" to 1.25, "inflammasome" to 1.15, "tcr_sig" to 0.25),
            nodeMultipliers = mapOf("prot" to 1.35, "tap" to 1.3, "mhc" to 1.35, "prr" to 1.3, "tbk1" to 1.25, "tlr" to 1.25)
        )
    )

    val CONDITIONS = listOf(
        BioConditionDataset(
            id = "baseline",
            name = "Homeostasis",
            summary = "Tonic physiological baseline with intact SOCS/PTEN negative feedback brakes.",
            stimulusDrive = 0.45,
            feedbackGain = 1.0,
            nodeBoosts = mapOf("socs" to 1.15, "pten" to 1.2),
            downstreamFocus = "Quiescent surveillance"
        ),
        BioConditionDataset(
            id = "viral_ifn",
            name = "Acute Viral IFN",
            summary = "High PAMP RNA/DNA sensing driving TBK1-IRF3, IFNAR-JAK-STAT1, and MHC I loading.",
            stimulusDrive = 1.45,
            feedbackGain = 0.85,
            nodeBoosts = mapOf("prr" to 1.6, "tbk1" to 1.5, "irf3" to 1.5, "cyt" to 1.5, "jak" to 1.45, "prot" to 1.4, "c3b" to 1.2),
            downstreamFocus = "Antiviral ISGs & CTL priming"
        ),
        BioConditionDataset(
            id = "lps_sepsis",
            name = "LPS / TLR4 Storm",
            summary = "Intense Gram-negative endotoxin surge activating NF-kB, NLRP3 inflammasome, and complement.",
            stimulusDrive = 1.65,
            feedbackGain = 0.65,
            nodeBoosts = mapOf("tlr" to 1.7, "myd88" to 1.55, "ikk" to 1.5, "sig1" to 1.6, "sig2" to 1.6, "c1q" to 1.5, "socs" to 0.5),
            downstreamFocus = "TNF-a, IL-1b, IL-6 & pyroptosis"
        ),
        BioConditionDataset(
            id = "tcr_synapse",
            name = "TCR + CD28 Synapse",
            summary = "Full Signal 1 + Signal 2 immunological synapse driving NFAT, AP-1, and mTOR anabolism.",
            stimulusDrive = 1.5,
            feedbackGain = 0.85,
            nodeBoosts = mapOf("pmhc" to 1.65, "lck" to 1.55, "cd28" to 1.55, "pi3k_n" to 1.45, "rtk" to 1.5, "ras" to 1.45, "pten" to 0.45),
            downstreamFocus = "IL-2 burst & clonal expansion"
        ),
        BioConditionDataset(
            id = "jaki",
            name = "Tofacitinib (JAKi)",
            summary = "Small-molecule ATP-competitive JAK1/3 blockade suppressing downstream STAT and ISGs.",
            stimulusDrive = 1.2,
            feedbackGain = 1.15,
            nodeBoosts = mapOf("jak" to 0.08, "ifnar" to 0.25, "stat" to 0.2),
            downstreamFocus = "Cytokine receptor dampening"
        ),
        BioConditionDataset(
            id = "btki",
            name = "Ibrutinib (BTKi)",
            summary = "Covalent BTK Cys481 inhibition blocking BCR PLCg2 Ca2+ mobilization and survival.",
            stimulusDrive = 1.2,
            feedbackGain = 1.1,
            nodeBoosts = mapOf("btk" to 0.06, "plcg2" to 0.15, "pi3k_n" to 0.6),
            downstreamFocus = "BCR survival attenuation"
        ),
        BioConditionDataset(
            id = "pten_loss",
            name = "PTEN Loss / mTOR+",
            summary = "Loss of PTEN lipid phosphatase unleashes hyperactive PIP3-AKT-mTOR signaling.",
            stimulusDrive = 1.25,
            feedbackGain = 0.5,
            nodeBoosts = mapOf("pten" to 0.05, "pi3k_n" to 1.7, "akt" to 1.75, "mtor" to 1.85),
            downstreamFocus = "Constitutive anabolism & survival"
        ),
        BioConditionDataset(
            id = "custom_csv",
            name = "Custom Bio CSV",
            summary = "Applies fold-expression weights directly from the user-loaded biological dataset.",
            stimulusDrive = 1.1,
            feedbackGain = 1.0,
            nodeBoosts = emptyMap(),
            downstreamFocus = "User dataset driven"
        )
    )

    fun parseBiologicalCsv(csvText: String): Map<String, Double> {
        val out = linkedMapOf<String, Double>()
        csvText.lines().forEachIndexed { idx, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val parts = line.split(',', '\t', ';', '=').map { it.trim() }
            if (parts.size < 2) return@forEachIndexed
            val key = parts[0].lowercase()
            val num = parts[1].toDoubleOrNull()
            if (idx == 0 && num == null) return@forEachIndexed // header row
            if (key.isNotEmpty() && num != null) {
                out[key] = num.coerceIn(0.0, 4.0)
            }
        }
        return out
    }

    fun simulate(
        pathway: PathwayInfo,
        cellContext: CellSignalingContext,
        condition: BioConditionDataset,
        customCsvMap: Map<String, Double>,
        stimulusDose: Double,
        feedbackStrength: Double,
        perturbations: Map<String, NodePerturbation>
    ): SignalingSimulationResult {
        val incomingActivate = pathway.nodes.associate { n ->
            n.id to pathway.edges.filter { it.to == n.id && it.action != "inhibits" }.map { it.from }
        }
        val incomingInhibit = pathway.nodes.associate { n ->
            n.id to pathway.edges.filter { it.to == n.id && it.action == "inhibits" }.map { it.from }
        }

        val pathwayAffinity = cellContext.pathwayAffinities[pathway.id] ?: 1.0
        var matchedCsvCount = 0

        val nodeCsvFactor = pathway.nodes.associate { n ->
            val labelTokens = n.label.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
            val matched = customCsvMap.entries.firstOrNull { (k, _) ->
                k == n.id.lowercase() ||
                    n.label.lowercase().contains(k) ||
                    labelTokens.any { tok -> k.contains(tok) || tok.contains(k) }
            }
            if (matched != null) {
                matchedCsvCount++
                n.id to matched.value.coerceIn(0.05, 2.5)
            } else {
                n.id to 1.0
            }
        }

        val act = pathway.nodes.associate { n ->
            val pert = perturbations[n.id] ?: NodePerturbation.NORMAL
            val init = when (pert) {
                NodePerturbation.INHIBITED -> 0.03
                NodePerturbation.HYPERACTIVE -> 0.96
                NodePerturbation.NORMAL -> 0.2
            }
            n.id to init
        }.toMutableMap()

        val effStim = (stimulusDose * condition.stimulusDrive * pathwayAffinity).coerceIn(0.05, 2.2)
        val effFeedback = (feedbackStrength * condition.feedbackGain).coerceIn(0.0, 2.2)

        repeat(16) {
            val next = act.toMutableMap()
            for (n in pathway.nodes) {
                val pert = perturbations[n.id] ?: NodePerturbation.NORMAL
                if (pert == NodePerturbation.INHIBITED) {
                    next[n.id] = 0.03
                    continue
                }
                val cellMult = cellContext.nodeMultipliers[n.id] ?: 1.0
                val condMult = condition.nodeBoosts[n.id] ?: 1.0
                val csvMult = if (condition.id == "custom_csv" || customCsvMap.isNotEmpty()) {
                    nodeCsvFactor[n.id] ?: 1.0
                } else 1.0
                val combinedMult = (cellMult * condMult * csvMult).coerceIn(0.04, 2.5)

                val actSources = incomingActivate[n.id].orEmpty()
                val rawDrive = if (actSources.isEmpty()) {
                    (0.58 * effStim * combinedMult)
                } else {
                    val sourceMean = actSources.map { act[it] ?: 0.0 }.average()
                    val sourceMax = actSources.maxOf { act[it] ?: 0.0 }
                    val blended = 0.45 * sourceMean + 0.55 * sourceMax
                    (blended * (0.78 + 0.34 * combinedMult))
                }

                val inhSources = incomingInhibit[n.id].orEmpty()
                val inhibitionFactor = if (inhSources.isEmpty()) 1.0 else {
                    val inhStrength = inhSources.maxOf { act[it] ?: 0.0 }
                    (1.0 - effFeedback * inhStrength * 0.72).coerceIn(0.12, 1.0)
                }

                var value = (rawDrive * inhibitionFactor).coerceIn(0.03, 0.99)
                if (pert == NodePerturbation.HYPERACTIVE) {
                    value = maxOf(0.92, (value * 1.45).coerceAtMost(0.99))
                }
                next[n.id] = 0.35 * (act[n.id] ?: 0.0) + 0.65 * value
            }
            act.putAll(next)
        }

        val edgeFluxes = pathway.edges.associate { e ->
            val srcVal = act[e.from] ?: 0.0
            val dstPert = perturbations[e.to] ?: NodePerturbation.NORMAL
            val rawFlux = if (e.action == "inhibits") {
                (srcVal * effFeedback * 0.85).coerceIn(0.02, 1.0)
            } else {
                val dstCap = if (dstPert == NodePerturbation.INHIBITED) 0.18 else 1.0
                (srcVal * dstCap).coerceIn(0.02, 1.0)
            }
            "${e.from}->${e.to}" to rawFlux
        }

        val orderedNodes = pathway.nodes.sortedBy { it.x }
        val upstreamVal = orderedNodes.take(2).map { act[it.id] ?: 0.0 }.average()
        val midVal = orderedNodes.drop(1).dropLast(1).ifEmpty { orderedNodes }.map { act[it.id] ?: 0.0 }.average()
        val terminalVal = orderedNodes.takeLast(1).map { act[it.id] ?: 0.0 }.average()

        val timeSeries = (0..12).map { step ->
            val t = step * 5.0 // 0..60 minutes
            val fastRise = 1.0 - exp(-t / 9.0)
            val feedbackDecay = 1.0 - (0.28 * effFeedback * (1.0 - exp(-t / 28.0))).coerceIn(0.0, 0.55)
            val midRise = 1.0 - exp(-maxOf(0.0, t - 2.0) / 14.0)
            val nucRise = 1.0 - exp(-maxOf(0.0, t - 6.0) / 22.0)
            SignalingTimePoint(
                minute = t,
                receptorKinase = (upstreamVal * fastRise * feedbackDecay).coerceIn(0.0, 1.0) * 100.0,
                cytosolicAdaptor = (midVal * midRise * (0.85 + 0.15 * feedbackDecay)).coerceIn(0.0, 1.0) * 100.0,
                nuclearEffector = (terminalVal * nucRise).coerceIn(0.0, 1.0) * 100.0
            )
        }

        val downstreamOutputs = computeDownstreamSignature(pathway.id, terminalVal, midVal, act)
        val peakAct = act.values.maxOrNull() ?: 0.0
        val inhibitedNames = pathway.nodes.filter { perturbations[it.id] == NodePerturbation.INHIBITED }.map { it.label }
        val summary = buildString {
            append("${cellContext.name} under ${condition.name}: terminal effector activity ${(terminalVal * 100).toInt()}%.")
            if (inhibitedNames.isNotEmpty()) {
                append(" Blockade at ${inhibitedNames.joinToString()} attenuates downstream flux.")
            }
            if (matchedCsvCount > 0) {
                append(" Mapped $matchedCsvCount biological CSV feature(s) onto network nodes.")
            }
        }

        return SignalingSimulationResult(
            nodeActivations = act,
            edgeFluxes = edgeFluxes,
            timeSeries = timeSeries,
            downstreamOutputs = downstreamOutputs,
            matchedCsvNodes = matchedCsvCount,
            peakActivation = peakAct,
            summaryText = summary
        )
    }

    private fun computeDownstreamSignature(
        pathwayId: String,
        terminalVal: Double,
        midVal: Double,
        act: Map<String, Double>
    ): List<Pair<String, Double>> {
        val base = terminalVal * 100.0
        val mid = midVal * 100.0
        return when (pathwayId) {
            "jak_stat" -> listOf(
                "ISG15" to base * 0.98,
                "IRF1" to base * 0.90,
                "CD25" to (act["stat"] ?: terminalVal) * 88.0,
                "SOCS1" to (act["socs"] ?: midVal) * 95.0,
                "MHC-I" to base * 0.82
            )
            "nfkb" -> listOf(
                "TNF-a" to base * 0.96,
                "IL-6" to base * 0.91,
                "IL-1b" to mid * 0.86,
                "CXCL8" to base * 0.84,
                "BCL2L1" to base * 0.78
            )
            "tcr_sig" -> listOf(
                "IL-2" to base * 0.98,
                "IFN-g" to base * 0.90,
                "CD69" to mid * 0.92,
                "CD40L" to base * 0.84,
                "GZMB" to base * 0.79
            )
            "bcr_sig" -> listOf(
                "NF-kB" to base * 0.95,
                "BCL-XL" to base * 0.88,
                "CD86" to mid * 0.90,
                "MHC-II" to base * 0.86,
                "AICDA" to base * 0.75
            )
            "ifn_sig" -> listOf(
                "MxA" to base * 0.97,
                "OAS1" to base * 0.92,
                "PKR" to base * 0.88,
                "IFNB1" to (act["irf3"] ?: midVal) * 94.0,
                "CXCL10" to base * 0.84
            )
            "inflammasome" -> listOf(
                "IL-1b" to base * 0.98,
                "IL-18" to base * 0.91,
                "GSDMD" to base * 0.88,
                "ASC" to (act["nlrp3"] ?: midVal) * 90.0,
                "DAMPs" to base * 0.76
            )
            "pi3k" -> listOf(
                "S6K1" to base * 0.96,
                "4EBP1" to base * 0.90,
                "GLUT1" to (act["akt"] ?: midVal) * 92.0,
                "FOXO-" to base * 0.82,
                "Survival" to base * 0.94
            )
            else -> listOf(
                "Effector" to base * 0.96,
                "Cytokine" to base * 0.88,
                "Survive" to mid * 0.85,
                "Traffick" to base * 0.78,
                "Metab." to (base + mid) * 0.44
            )
        }.map { (k, v) -> k to v.coerceIn(0.0, 100.0) }
    }
}
