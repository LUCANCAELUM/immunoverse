package com.example.domain

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class ProteomicsBenchmarkProtein(
    val uniprotId: String,
    val pdbId: String,
    val gene: String,
    val name: String,
    val role: String,
    val sequence: String
)

data class ProtParamResult(
    val sequence: String,
    val length: Int,
    val avgMassDa: Double,
    val monoisotopicMassDa: Double,
    val theoreticalPi: Double,
    val netChargePh74: Double,
    val extCoeffReduced: Int,
    val extCoeffOxidized: Int,
    val abs01Reduced: Double,
    val abs01Oxidized: Double,
    val aliphaticIndex: Double,
    val instabilityIndex: Double,
    val isStable: Boolean,
    val gravy: Double,
    val halfLifeMammalian: String,
    val counts: Map<Char, Int>,
    val titrationCurve: List<Pair<Double, Double>>, // pH to Net Charge
    val kyteDoolittleProfile: List<Double>,
    val hoppWoodsProfile: List<Double>
)

enum class ProteaseEnzyme(val label: String, val specificity: String) {
    TRYPSIN("Trypsin", "Cleaves C-term to K, R (not before P)"),
    TRYPSIN_STRICT("Trypsin/P", "Cleaves C-term to K, R (even before P)"),
    CHYMOTRYPSIN("Chymotrypsin", "Cleaves C-term to F, W, Y, L (not before P)"),
    LYS_C("Lys-C", "Cleaves C-term to K"),
    ARG_C("Arg-C", "Cleaves C-term to R"),
    GLU_C("Glu-C (V8)", "Cleaves C-term to D, E"),
    ASP_N("Asp-N", "Cleaves N-term to D"),
    CNBR("CNBr", "Cleaves C-term to M")
}

data class DigestedPeptide(
    val startPos: Int,
    val endPos: Int,
    val missedCleavages: Int,
    val sequence: String,
    val neutralMonoMassDa: Double,
    val mzPlus1: Double,
    val mzPlus2: Double,
    val mzPlus3: Double,
    val retentionIndex: Double,
    val modificationNote: String,
    val estimatedIntensity: Double
)

enum class PeptideModType(val label: String, val deltaMassDa: Double) {
    NONE("Unmodified (0.0000 Da)", 0.0),
    CARBAMIDOMETHYL_C("Carbamidomethyl C (+57.0215 Da)", 57.021464),
    OXIDATION_M("Oxidation M (+15.9949 Da)", 15.994915),
    PHOSPHO_STY("Phospho S/T/Y (+79.9663 Da)", 79.966331),
    ACETYL_NTERM("N-Term Acetyl (+42.0106 Da)", 42.010565),
    TMT6_NTERM("TMT6/10plex N-Term (+229.1629 Da)", 229.162932)
}

data class FragmentIon(
    val series: String, // "b" or "y"
    val number: Int,
    val charge: Int,
    val mz: Double,
    val neutralLossNote: String,
    val sequenceFragment: String
)

data class PeptideMsMsResult(
    val peptide: String,
    val modification: PeptideModType,
    val precursorNeutralMass: Double,
    val precursorMh1: Double,
    val precursorMh2: Double,
    val ions: List<FragmentIon>
)

data class PsmMatchSummary(
    val queryPeaksCount: Int,
    val matchedCount: Int,
    val matchedBIons: Int,
    val matchedYIons: Int,
    val sequenceCoveragePct: Double,
    val meanAbsErrorPpm: Double,
    val hyperscore: Double,
    val matchedIons: List<Pair<FragmentIon, Double>> // theoretical ion to error ppm
)

data class PtmMotifHit(
    val motifName: String,
    val category: String,
    val startPos: Int,
    val endPos: Int,
    val matchedSeq: String,
    val biologicalNote: String
)

data class DifferentialProteinRow(
    val proteinName: String,
    val uniprotId: String,
    val log2Fc: Double,
    val pValue: Double,
    val negLog10P: Double,
    val fdrBh: Double,
    val condition: String,
    val regulation: String // "Up", "Down", "NS"
)

object ProteomicsEngine {
    const val PROTON_MASS = 1.0072764668
    const val H2O_MONO = 18.01056468
    const val NH3_MONO = 17.02654910
    const val CYS_CARBAMIDO_MONO = 57.021464

    val MONO_RESIDUE_MASS = mapOf(
        'A' to 71.037114, 'R' to 156.101111, 'N' to 114.042927, 'D' to 115.026943,
        'C' to 103.009185, 'E' to 129.042593, 'Q' to 128.058578, 'G' to 57.021464,
        'H' to 137.058912, 'I' to 113.084064, 'L' to 113.084064, 'K' to 128.094963,
        'M' to 131.040485, 'F' to 147.068414, 'P' to 97.052764, 'S' to 87.032028,
        'T' to 101.047678, 'W' to 186.079313, 'Y' to 163.063329, 'V' to 99.068414
    )

    val AVG_RESIDUE_MASS = mapOf(
        'A' to 71.0779, 'R' to 156.1857, 'N' to 114.1026, 'D' to 115.0874,
        'C' to 103.1429, 'E' to 129.1140, 'Q' to 128.1292, 'G' to 57.0513,
        'H' to 137.1393, 'I' to 113.1576, 'L' to 113.1576, 'K' to 128.1723,
        'M' to 131.1961, 'F' to 147.1739, 'P' to 97.1152, 'S' to 87.0773,
        'T' to 101.1039, 'W' to 186.2099, 'Y' to 163.1733, 'V' to 99.1311
    )

    private val KD_HYDROPATHY = mapOf(
        'I' to 4.5, 'V' to 4.2, 'L' to 3.8, 'F' to 2.8, 'C' to 2.5,
        'M' to 1.9, 'A' to 1.8, 'G' to -0.4, 'T' to -0.7, 'S' to -0.8,
        'W' to -0.9, 'Y' to -1.3, 'P' to -1.6, 'H' to -3.2, 'E' to -3.5,
        'Q' to -3.5, 'D' to -3.5, 'N' to -3.5, 'K' to -3.9, 'R' to -4.5
    )

    private val HOPP_WOODS = mapOf(
        'R' to 3.0, 'D' to 3.0, 'E' to 3.0, 'K' to 3.0, 'S' to 0.3,
        'N' to 0.2, 'Q' to 0.2, 'G' to 0.0, 'P' to 0.0, 'T' to -0.4,
        'A' to -0.5, 'H' to -0.5, 'C' to -1.0, 'M' to -1.3, 'V' to -1.5,
        'I' to -1.8, 'L' to -1.8, 'Y' to -2.3, 'F' to -2.5, 'W' to -3.4
    )

    val BENCHMARK_PROTEINS = listOf(
        ProteomicsBenchmarkProtein(
            uniprotId = "P0CG48",
            pdbId = "1UBQ",
            gene = "UBB",
            name = "Human Ubiquitin (1UBQ)",
            role = "Lys-48/63 polyubiquitin signaling tag regulating NF-κB & proteasomal antigen processing",
            sequence = "MQIFVKTLTGKTITLEVEPSDTIENVKAKIQDKEGIPPDQQRLIFAGKQLEDGRTLSDYNIQKESTLHLVLRLRGG"
        ),
        ProteomicsBenchmarkProtein(
            uniprotId = "P01579",
            pdbId = "1FG9",
            gene = "IFNG",
            name = "Human Interferon-Gamma (IFNG)",
            role = "Canonical Th1 macrophage-activating cytokine driving MHC-I/II upregulation and iNOS",
            sequence = "MKYTSYILAFQLCIVLGSLGCYCQDPYVKEAENLKKYFNAGHSDVADNGTLFLGILKNWKEESDRKIMQSQIVSFYFKLFKNFKDDQSIQKSVETIKEDMNVKFFNSNKKKRDDFEKLTNYSVTDLNVQRKAIHELIQVMAELSPAAKTGKRKRSQMLFRGRRASQ"
        ),
        ProteomicsBenchmarkProtein(
            uniprotId = "P05231",
            pdbId = "1ALU",
            gene = "IL6",
            name = "Human Interleukin-6 (IL6)",
            role = "Acute-phase gp130/JAK-STAT3 pleiotropic cytokine and therapeutic target (tocilizumab)",
            sequence = "VPPGEDSKDVAAPHRQPLTSSERIDKQIRYILDGISALRKETCNKSNMCESSKEALAENNLNLPKMAEKDGCFQSGFNEETCLVKIITGLLEFEVYLEYLQNRFESSEEQARAVQMSTKVLIQFLQKKAKNLDAITTPDPTTNASLLTKLQAQNQWLQDMTTHLILRSFKEFLQSSLRALRQM"
        ),
        ProteomicsBenchmarkProtein(
            uniprotId = "P01375",
            pdbId = "1TNF",
            gene = "TNF",
            name = "TNF-Alpha Soluble Trimer (TNF)",
            role = "Pyrogenic inflammatory cytokine cleaved by ADAM17/TACE; neutralized by adalimumab/infliximab",
            sequence = "VRSSSRTPSDKPVAHVVANPQAEGQLQWLNRRANALLANGVELRDNQLVVPSEGLYLIYSQVLFKGQGCPSTHVLLTHTISRIAVSYQTKVNLLSAIKSPCQRETPEGAEAKPWYEPIYLGGVFQLEKGDRLSAEINRPDYLDFAESGQVYFGIIAL"
        ),
        ProteomicsBenchmarkProtein(
            uniprotId = "Q9NZQ7",
            pdbId = "3BIK",
            gene = "CD274",
            name = "PD-L1 Checkpoint Ectodomain (CD274)",
            role = "Ig-like immunosuppressive checkpoint ligand engaging T-cell PD-1 to restrain CTL lysis",
            sequence = "FTVTVPKDLYVVEYGSNMTIECKFPVEKQLDLAALIVYWEMEDKNIIQFVHGEEDLKVQHSSYRQRARLLKDQLSLGNAALQITDVKLQDAGVYRCMISYGGADYKRITVKVNAPYNKINQRILVVDPVTSEHELTCQAEGYPKAEVIWTSSDHQVLSGKTTTTNSKREEKLFNVTSTLRINTTTNEIFYCTFRRLDPEENHTAELVIPELPLAHPPNER"
        )
    )

    val DEMO_VOLCANO_CSV = """Gene,UniProt,Log2FC,PValue,Condition
STAT1,P42224,2.84,0.000012,TCR+IFN Stim vs Resting
CXCL10,P02778,3.41,0.000004,TCR+IFN Stim vs Resting
GZMB,P10144,2.45,0.00009,TCR+IFN Stim vs Resting
IFNG,P01579,3.12,0.00002,TCR+IFN Stim vs Resting
IL2RA,P01589,2.10,0.00035,TCR+IFN Stim vs Resting
PDCD1,Q15116,1.68,0.0018,TCR+IFN Stim vs Resting
CD274,Q9NZQ7,1.92,0.0007,TCR+IFN Stim vs Resting
ISG15,P05161,2.55,0.00005,TCR+IFN Stim vs Resting
ZAP70,P43403,1.22,0.0084,TCR+IFN Stim vs Resting
LCK,P06239,0.52,0.095,TCR+IFN Stim vs Resting
FOXP3,Q9BZS1,-1.45,0.0042,TCR+IFN Stim vs Resting
KLF2,Q9Y5W3,-2.05,0.0004,TCR+IFN Stim vs Resting
SELL,P14151,-1.88,0.0011,TCR+IFN Stim vs Resting
CCR7,P32248,-1.52,0.0031,TCR+IFN Stim vs Resting
TXNIP,Q9H3M7,-2.35,0.00018,TCR+IFN Stim vs Resting
GAPDH,P04406,0.08,0.74,TCR+IFN Stim vs Resting
ACTB,P60709,-0.12,0.61,TCR+IFN Stim vs Resting
B2M,P61769,0.78,0.042,TCR+IFN Stim vs Resting
TNF,P01375,1.74,0.0015,TCR+IFN Stim vs Resting
TGFB1,P01137,-0.92,0.032,TCR+IFN Stim vs Resting"""

    fun cleanProteinSeq(raw: String): String {
        val lines = raw.lines().filterNot { it.trim().startsWith(">") }
        return lines.joinToString("").uppercase().filter { it in MONO_RESIDUE_MASS }
    }

    fun computeProtParam(rawSeq: String): ProtParamResult {
        val seq = cleanProteinSeq(rawSeq)
        val counts = mutableMapOf<Char, Int>()
        MONO_RESIDUE_MASS.keys.forEach { counts[it] = 0 }
        seq.forEach { ch -> counts[ch] = (counts[ch] ?: 0) + 1 }

        if (seq.isEmpty()) {
            return ProtParamResult(
                sequence = "", length = 0, avgMassDa = 0.0, monoisotopicMassDa = 0.0,
                theoreticalPi = 7.0, netChargePh74 = 0.0, extCoeffReduced = 0, extCoeffOxidized = 0,
                abs01Reduced = 0.0, abs01Oxidized = 0.0, aliphaticIndex = 0.0, instabilityIndex = 0.0,
                isStable = true, gravy = 0.0, halfLifeMammalian = "N/A",
                counts = counts, titrationCurve = emptyList(),
                kyteDoolittleProfile = emptyList(), hoppWoodsProfile = emptyList()
            )
        }

        val n = seq.length
        val avgMass = seq.sumOf { AVG_RESIDUE_MASS[it] ?: 110.0 } + 18.01528
        val monoMass = seq.sumOf { MONO_RESIDUE_MASS[it] ?: 110.0 } + H2O_MONO

        fun chargeAtPh(ph: Double): Double {
            val pos = { pka: Double -> 1.0 / (1.0 + 10.0.pow(ph - pka)) }
            val neg = { pka: Double -> -1.0 / (1.0 + 10.0.pow(pka - ph)) }
            var q = pos(8.0) + neg(3.1) // N-term NH3+, C-term COO-
            q += (counts['K'] ?: 0) * pos(10.0)
            q += (counts['R'] ?: 0) * pos(12.0)
            q += (counts['H'] ?: 0) * pos(6.05)
            q += (counts['D'] ?: 0) * neg(4.05)
            q += (counts['E'] ?: 0) * neg(4.45)
            q += (counts['C'] ?: 0) * neg(9.0)
            q += (counts['Y'] ?: 0) * neg(10.0)
            return q
        }

        var lo = 0.0
        var hi = 14.0
        repeat(45) {
            val mid = (lo + hi) * 0.5
            if (chargeAtPh(mid) > 0.0) lo = mid else hi = mid
        }
        val pi = (lo + hi) * 0.5
        val netPh74 = chargeAtPh(7.4)

        val nW = counts['W'] ?: 0
        val nY = counts['Y'] ?: 0
        val nC = counts['C'] ?: 0
        val extRed = nW * 5500 + nY * 1490
        val extOx = extRed + (nC / 2) * 125
        val absRed = if (avgMass > 0) extRed / avgMass else 0.0
        val absOx = if (avgMass > 0) extOx / avgMass else 0.0

        val pctA = 100.0 * (counts['A'] ?: 0) / n
        val pctV = 100.0 * (counts['V'] ?: 0) / n
        val pctI = 100.0 * (counts['I'] ?: 0) / n
        val pctL = 100.0 * (counts['L'] ?: 0) / n
        val aliphatic = pctA + 2.9 * pctV + 3.9 * (pctI + pctL)

        val gravy = seq.sumOf { KD_HYDROPATHY[it] ?: 0.0 } / n

        // Guruprasad et al. (1990) dipeptide instability weight approximation
        var diSum = 0.0
        for (i in 0 until n - 1) {
            diSum += dipeptideWeight(seq[i], seq[i + 1])
        }
        val ii = if (n > 1) (10.0 / n) * diSum else 0.0

        val halfLife = when (seq.first()) {
            'M', 'G', 'A', 'S', 'T', 'V' -> ">20–30 hours (Mammalian reticulocytes, stabilizing N-end)"
            'P' -> ">20 hours (Pro N-end)"
            'I', 'L', 'K', 'H', 'C' -> "3.5–10 hours (Intermediate destabilizing N-end)"
            else -> "1.0–2.8 hours (Destabilizing Arg/Asp/Glu/Phe/Trp/Tyr N-end)"
        }

        val titration = (1..13).map { ph -> ph.toDouble() to chargeAtPh(ph.toDouble()) }
        val kdProfile = slidingProfile(seq, KD_HYDROPATHY, window = 7)
        val hwProfile = slidingProfile(seq, HOPP_WOODS, window = 7)

        return ProtParamResult(
            sequence = seq,
            length = n,
            avgMassDa = avgMass,
            monoisotopicMassDa = monoMass,
            theoreticalPi = pi,
            netChargePh74 = netPh74,
            extCoeffReduced = extRed,
            extCoeffOxidized = extOx,
            abs01Reduced = absRed,
            abs01Oxidized = absOx,
            aliphaticIndex = aliphatic,
            instabilityIndex = ii,
            isStable = ii < 40.0,
            gravy = gravy,
            halfLifeMammalian = halfLife,
            counts = counts,
            titrationCurve = titration,
            kyteDoolittleProfile = kdProfile,
            hoppWoodsProfile = hwProfile
        )
    }

    private fun slidingProfile(seq: String, table: Map<Char, Double>, window: Int): List<Double> {
        if (seq.isEmpty()) return emptyList()
        val half = window / 2
        return seq.indices.map { idx ->
            val from = (idx - half).coerceAtLeast(0)
            val to = (idx + half).coerceAtMost(seq.lastIndex)
            var s = 0.0
            for (k in from..to) s += (table[seq[k]] ?: 0.0)
            s / (to - from + 1)
        }
    }

    private fun dipeptideWeight(a: Char, b: Char): Double {
        // Core Guruprasad DIWV high/low stabilizing & destabilizing signatures
        return when ("$a$b") {
            "DS", "DP", "ES", "EP", "SP", "TP", "PP", "QP", "KE", "RE", "WS", "YS" -> 22.0
            "DG", "EG", "SS", "TS", "PS", "PE", "PD", "DD", "EE", "RR" -> 12.5
            "MG", "MA", "AV", "VA", "IL", "LI", "AL", "LA", "VL", "LV", "VY", "YF" -> -6.5
            else -> when {
                a in "DESPQ" && b in "PSDEQ" -> 8.5
                a in "IVLAF" && b in "IVLAF" -> -2.5
                else -> 1.8
            }
        }
    }

    fun digestProtein(
        rawSeq: String,
        enzyme: ProteaseEnzyme,
        maxMissedCleavages: Int = 1,
        fixedCarbamidomethylC: Boolean = true,
        variableOxidationM: Boolean = false,
        variablePhosphoSTY: Boolean = false,
        minLength: Int = 5
    ): List<DigestedPeptide> {
        val seq = cleanProteinSeq(rawSeq)
        if (seq.isEmpty()) return emptyList()

        // Determine cleavage boundaries (indices in 0..seq.length where cleavage occurs after index-1)
        val cuts = mutableListOf(0)
        for (i in 0 until seq.length - 1) {
            val cur = seq[i]
            val next = seq[i + 1]
            val shouldCut = when (enzyme) {
                ProteaseEnzyme.TRYPSIN -> (cur == 'K' || cur == 'R') && next != 'P'
                ProteaseEnzyme.TRYPSIN_STRICT -> cur == 'K' || cur == 'R'
                ProteaseEnzyme.CHYMOTRYPSIN -> (cur == 'F' || cur == 'W' || cur == 'Y' || cur == 'L') && next != 'P'
                ProteaseEnzyme.LYS_C -> cur == 'K'
                ProteaseEnzyme.ARG_C -> cur == 'R'
                ProteaseEnzyme.GLU_C -> cur == 'D' || cur == 'E'
                ProteaseEnzyme.ASP_N -> next == 'D'
                ProteaseEnzyme.CNBR -> cur == 'M'
            }
            if (shouldCut) cuts += (i + 1)
        }
        cuts += seq.length
        val uniqueCuts = cuts.distinct().sorted()

        val peptides = mutableListOf<DigestedPeptide>()
        val missedAllowed = maxMissedCleavages.coerceIn(0, 2)
        for (i in 0 until uniqueCuts.lastIndex) {
            for (mc in 0..missedAllowed) {
                val endCutIdx = i + 1 + mc
                if (endCutIdx >= uniqueCuts.size) break
                val startIdx = uniqueCuts[i]
                val endIdx = uniqueCuts[endCutIdx]
                val pep = seq.substring(startIdx, endIdx)
                if (pep.length < minLength) continue

                var mass = pep.sumOf { MONO_RESIDUE_MASS[it] ?: 110.0 } + H2O_MONO
                val mods = mutableListOf<String>()
                if (fixedCarbamidomethylC) {
                    val cCount = pep.count { it == 'C' }
                    if (cCount > 0) {
                        mass += cCount * CYS_CARBAMIDO_MONO
                        mods += "${cCount}xCarbamidomethyl(C)"
                    }
                }
                if (variableOxidationM && 'M' in pep) {
                    val mCount = pep.count { it == 'M' }
                    mass += mCount * 15.994915
                    mods += "${mCount}xOx(M)"
                }
                if (variablePhosphoSTY && pep.any { it == 'S' || it == 'T' || it == 'Y' }) {
                    mass += 79.966331
                    mods += "1xPhospho(S/T/Y)"
                }

                val kdMean = pep.sumOf { KD_HYDROPATHY[it] ?: 0.0 } / pep.length
                val retentionIdx = (12.0 + pep.length * 0.65 + kdMean * 4.2).coerceIn(2.0, 95.0)
                val mz1 = mass + PROTON_MASS
                val mz2 = (mass + 2.0 * PROTON_MASS) / 2.0
                val mz3 = (mass + 3.0 * PROTON_MASS) / 3.0
                // Heuristic MS1 ionization efficiency (K/R basicity + optimal length 7-20)
                val flyability = ((if (pep.last() == 'R') 1.0 else if (pep.last() == 'K') 0.85 else 0.72) *
                    (1.0 / (1.0 + abs(pep.length - 12) * 0.06)) *
                    (1.0 - mc * 0.28)).coerceIn(0.15, 1.0)

                peptides += DigestedPeptide(
                    startPos = startIdx + 1,
                    endPos = endIdx,
                    missedCleavages = mc,
                    sequence = pep,
                    neutralMonoMassDa = mass,
                    mzPlus1 = mz1,
                    mzPlus2 = mz2,
                    mzPlus3 = mz3,
                    retentionIndex = retentionIdx,
                    modificationNote = mods.ifEmpty { listOf("Unmodified") }.joinToString(", "),
                    estimatedIntensity = flyability
                )
            }
        }
        return peptides.sortedBy { it.mzPlus1 }
    }

    fun fragmentPeptideMsMs(
        rawPeptide: String,
        modification: PeptideModType = PeptideModType.CARBAMIDOMETHYL_C
    ): PeptideMsMsResult {
        val pep = rawPeptide.uppercase().filter { it in MONO_RESIDUE_MASS }
        if (pep.isEmpty()) {
            return PeptideMsMsResult("", modification, 0.0, 0.0, 0.0, emptyList())
        }

        fun residueModMass(ch: Char, idx: Int): Double = when (modification) {
            PeptideModType.NONE -> 0.0
            PeptideModType.CARBAMIDOMETHYL_C -> if (ch == 'C') modification.deltaMassDa else 0.0
            PeptideModType.OXIDATION_M -> if (ch == 'M') modification.deltaMassDa else 0.0
            PeptideModType.PHOSPHO_STY -> if (idx == pep.indexOfFirst { it == 'S' || it == 'T' || it == 'Y' }) modification.deltaMassDa else 0.0
            PeptideModType.ACETYL_NTERM -> if (idx == 0) modification.deltaMassDa else 0.0
            PeptideModType.TMT6_NTERM -> if (idx == 0) modification.deltaMassDa else 0.0
        }

        val resMasses = pep.mapIndexed { idx, ch -> (MONO_RESIDUE_MASS[ch] ?: 110.0) + residueModMass(ch, idx) }
        val totalNeutral = resMasses.sum() + H2O_MONO
        val ions = mutableListOf<FragmentIon>()

        // b1..b(n-1)
        var bCum = 0.0
        for (i in 0 until pep.lastIndex) {
            bCum += resMasses[i]
            val frag = pep.substring(0, i + 1)
            val b1 = bCum + PROTON_MASS
            val b2 = (bCum + 2.0 * PROTON_MASS) / 2.0
            ions += FragmentIon("b", i + 1, 1, b1, "", frag)
            if (i >= 2) ions += FragmentIon("b", i + 1, 2, b2, "2+", frag)
        }

        // y1..y(n-1)
        var yCum = H2O_MONO
        for (i in pep.lastIndex downTo 1) {
            yCum += resMasses[i]
            val num = pep.length - i
            val frag = pep.substring(i)
            val y1 = yCum + PROTON_MASS
            val y2 = (yCum + 2.0 * PROTON_MASS) / 2.0
            ions += FragmentIon("y", num, 1, y1, "", frag)
            if (num >= 3) ions += FragmentIon("y", num, 2, y2, "2+", frag)
        }

        return PeptideMsMsResult(
            peptide = pep,
            modification = modification,
            precursorNeutralMass = totalNeutral,
            precursorMh1 = totalNeutral + PROTON_MASS,
            precursorMh2 = (totalNeutral + 2.0 * PROTON_MASS) / 2.0,
            ions = ions.sortedBy { it.mz }
        )
    }

    fun matchObservedMsMsPeaks(
        msms: PeptideMsMsResult,
        observedMzText: String,
        tolerancePpm: Double = 25.0
    ): PsmMatchSummary {
        val observed = observedMzText.split(Regex("[,;\\s\\n]+"))
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 40.0 }
        if (observed.isEmpty() || msms.ions.isEmpty()) {
            return PsmMatchSummary(observed.size, 0, 0, 0, 0.0, 0.0, 0.0, emptyList())
        }
        val tolDa = { mz: Double -> (mz * tolerancePpm / 1_000_000.0).coerceAtLeast(0.02) }
        val matched = mutableListOf<Pair<FragmentIon, Double>>()
        val coveredCleavages = mutableSetOf<Int>()
        var bCount = 0
        var yCount = 0

        for (ion in msms.ions) {
            val bestObs = observed.minByOrNull { abs(it - ion.mz) } ?: continue
            val diff = abs(bestObs - ion.mz)
            if (diff <= tolDa(ion.mz)) {
                val errPpm = (diff / ion.mz) * 1_000_000.0
                matched += ion to errPpm
                if (ion.series == "b") {
                    bCount++
                    coveredCleavages += ion.number
                } else {
                    yCount++
                    coveredCleavages += (msms.peptide.length - ion.number)
                }
            }
        }
        val cleavableBonds = (msms.peptide.length - 1).coerceAtLeast(1)
        val covPct = 100.0 * coveredCleavages.size / cleavableBonds
        val meanPpm = if (matched.isNotEmpty()) matched.map { it.second }.average() else 0.0
        val hyperscore = if (matched.isEmpty()) 0.0 else (matched.size * 4.8 + covPct * 0.35 + ln(1.0 + bCount * yCount.toDouble()) * 5.2)
        return PsmMatchSummary(
            queryPeaksCount = observed.size,
            matchedCount = matched.size,
            matchedBIons = bCount,
            matchedYIons = yCount,
            sequenceCoveragePct = covPct.coerceIn(0.0, 100.0),
            meanAbsErrorPpm = meanPpm,
            hyperscore = hyperscore,
            matchedIons = matched
        )
    }

    fun scanPtms(rawSeq: String): List<PtmMotifHit> {
        val seq = cleanProteinSeq(rawSeq)
        if (seq.length < 3) return emptyList()
        val hits = mutableListOf<PtmMotifHit>()

        data class PatternSpec(val name: String, val cat: String, val regex: Regex, val note: String)
        val specs = listOf(
            PatternSpec("N-Glycosylation Sequon", "Glycosylation", Regex("N[^P][ST]"), "ER oligosaccharyltransferase consensus N-X-S/T (immune receptors & viral envelopes)"),
            PatternSpec("PKC Phosphorylation", "Phosphorylation", Regex("[ST].[RK]"), "Protein Kinase C Ser/Thr phosphorylation motif"),
            PatternSpec("CK2 Phosphorylation", "Phosphorylation", Regex("[ST]..[DE]"), "Casein Kinase II acidic phosphorylation site"),
            PatternSpec("PKA / CaMKII Motif", "Phosphorylation", Regex("[RK][RK].[ST]"), "Basophilic kinase consensus motif"),
            PatternSpec("ITIM Inhibitory Motif", "Immune Signaling", Regex("[IVL].Y..[LV]"), "Immunoreceptor Tyrosine-based Inhibitory Motif (SHP-1/2 recruitment on PD-1/KIR)"),
            PatternSpec("ITAM-like YxxL", "Immune Signaling", Regex("Y..[ILV]"), "SH2-binding pTyr activation module motif (CD3ζ / FcRγ / BCR)"),
            PatternSpec("Integrin Binding RGD", "Adhesion", Regex("RGD"), "Canonical αvβ3 / α5β1 integrin cell-attachment tripeptide"),
            PatternSpec("Sumoylation Motif", "Ubiquitin-Like", Regex("[IVL]K.E"), "SUMO conjugation consensus ψ-K-x-E"),
            PatternSpec("KDEL ER Retention", "Trafficking", Regex("[KH]DEL$"), "C-terminal luminal endoplasmic reticulum retrieval signal")
        )

        for (spec in specs) {
            spec.regex.findAll(seq).take(12).forEach { m ->
                hits += PtmMotifHit(
                    motifName = spec.name,
                    category = spec.cat,
                    startPos = m.range.first + 1,
                    endPos = m.range.last + 1,
                    matchedSeq = m.value,
                    biologicalNote = spec.note
                )
            }
        }
        return hits.sortedBy { it.startPos }
    }

    fun parseDifferentialProteomicsCsv(
        csv: String,
        fcCutoff: Double = 1.0,
        pCutoff: Double = 0.05
    ): List<DifferentialProteinRow> {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return emptyList()
        val rawRows = lines.drop(1).mapNotNull { line ->
            val parts = line.split(',').map { it.trim() }
            if (parts.size < 4) return@mapNotNull null
            val gene = parts[0].ifBlank { return@mapNotNull null }
            val uniprot = parts[1].ifBlank { "—" }
            val fc = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            val pv = (parts[3].toDoubleOrNull() ?: return@mapNotNull null).coerceIn(1e-12, 1.0)
            val cond = parts.getOrNull(4).orEmpty().ifBlank { "Treatment vs Ctrl" }
            Triple(Triple(gene, uniprot, cond), fc, pv)
        }
        if (rawRows.isEmpty()) return emptyList()

        // Benjamini-Hochberg FDR adjustment
        val n = rawRows.size
        val indexedSorted = rawRows.withIndex().sortedBy { it.value.third }
        val fdrs = DoubleArray(n)
        var runningMin = 1.0
        for (rankIdx in indexedSorted.lastIndex downTo 0) {
            val origIdx = indexedSorted[rankIdx].index
            val rank = rankIdx + 1
            val rawP = indexedSorted[rankIdx].value.third
            val bh = (rawP * n / rank).coerceAtMost(1.0)
            runningMin = minOf(runningMin, bh)
            fdrs[origIdx] = runningMin
        }

        return rawRows.mapIndexed { idx, (meta, fc, pv) ->
            val negLog = -kotlin.math.log10(pv)
            val reg = when {
                pv <= pCutoff && fc >= fcCutoff -> "Up"
                pv <= pCutoff && fc <= -fcCutoff -> "Down"
                else -> "NS"
            }
            DifferentialProteinRow(
                proteinName = meta.first,
                uniprotId = meta.second,
                log2Fc = fc,
                pValue = pv,
                negLog10P = negLog,
                fdrBh = fdrs[idx],
                condition = meta.third,
                regulation = reg
            )
        }
    }
}
