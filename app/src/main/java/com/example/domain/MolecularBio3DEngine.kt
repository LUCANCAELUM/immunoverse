package com.example.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class AminoAcid3DInfo(
    val code1: Char,
    val code3: String,
    val name: String,
    val category: String,
    val mwDa: Double,
    val pKa1: Double,
    val pKa2: Double,
    val pKaR: Double?,
    val pI: Double,
    val hydropathy: Double,
    val codons: List<String>,
    val sideChainAtoms: List<SideChainAtomSpec>,
    val roleNote: String
)

data class SideChainAtomSpec(
    val label: String,
    val element: String, // C, N, O, S, H
    val dx: Float,
    val dy: Float,
    val dz: Float,
    val parentIndex: Int // -1 = C-alpha, 0+ = index in sideChainAtoms
)

data class DnaBasePairAnalysis(
    val cleanedSeq: String,
    val complementSeq: String,
    val helixForm: String,
    val lengthBp: Int,
    val gcPct: Double,
    val atPct: Double,
    val tmCelsius: Double,
    val dsMwKda: Double,
    val cpgCount: Int,
    val hBondsTotal: Int,
    val helicalPitchAngstrom: Double,
    val bpPerTurn: Double
)

data class RnaFoldAnalysis(
    val cleanedSeq: String,
    val dotBracket: String,
    val lengthNt: Int,
    val gcPct: Double,
    val wcPairs: Int,
    val wobbleGuPairs: Int,
    val unpairedCount: Int,
    val estimatedDeltaGKcal: Double,
    val translatedPeptide: String,
    val pairs: List<Pair<Int, Int>>
)

enum class SecStructType(val code: Char, val label: String) {
    HELIX('H', "Alpha Helix"),
    SHEET('E', "Beta Strand"),
    COIL('C', "Turn / Coil")
}

data class ProteinFoldAnalysis(
    val sequence: String,
    val lengthAa: Int,
    val mwKda: Double,
    val netChargePh74: Double,
    val theoreticalPi: Double,
    val gravy: Double,
    val helixPct: Double,
    val sheetPct: Double,
    val coilPct: Double,
    val cysDisulfidePairs: Int,
    val secStruct: List<SecStructType>,
    val hydropathyProfile: List<Double>
)

object MolecularBio3DEngine {
    val DNA_PRESETS = listOf(
        "IL2 Promoter Core" to "ATGGCGTACGTAGCTAGCATGCGCGTATAAATCGATCGGCTAGCA",
        "CCR5-Δ32 Junction" to "TTTCCATACAGTCAGTATCAATTCTGGAAGAATTTCCAGACATTAAAGATAGTC",
        "CpG-Rich Enhancer" to "GCGCATCGATCGCGTACGCGCGTATATCGCGCGGATCGTACGCGTA",
        "SARS-CoV-2 RBD Motif" to "GGTGTTGAAGGTTTTAATTGTTACTTTCCTTTACAATCATATGGTTTCCAACCC"
    )

    val RNA_PRESETS = listOf(
        "HIV-1 TAR Stem-Loop" to "GGUCUCUCUGGUUAGACCAGAUCUGAGCCUGGGAGCUCUCUGGCUAACUAGG",
        "tRNA-Phe Anticodon Arm" to "CCGGGGUUAGCUCAGUUGGGAGAGCGCCAGACUGAAGAUCUGGAGGUCCUGUGU",
        "SARS-CoV-2 Frameshift" to "UUUAAACGGGUUUGCGGUGUAAGUGCAGCCCGUCUUACACCGUGCGGCACAGGC",
        "Kozak Initiation Hairpin" to "GCCGCCACCAUGGCUGGCAAGGUCUUCGCCGUCAGCUGGCAGCCGGUGGCC"
    )

    val PROTEIN_PRESETS = listOf(
        "Zinc-Finger & Helix Motif" to "MKVLWAALLVTFLAGCQAKVEQAVETEPEPELRQQTEWQSGQRWELALGRFWDYLRWVQTLSEQVQEELLSSQVTQELRALMDETMKELKAYKSELEEQLTPVA",
        "Beta-Rich Ig Fold Snippet" to "EVQLVESGGGLVQPGGSLRLSCAASGFTFSSYAMSWVRQAPGKGLEWVSAISGSGGSTYYADSVKGRFTISRDNSKNTLYLQMNSLRAEDTAVYYCAK",
        "Amphipathic Antimicrobial" to "GIGKFLHSAKKFGKAFVGEIMNSKRIVQRIKDFLRNLVPRTESCGGCK"
    )

    val AMINO_ACIDS: List<AminoAcid3DInfo> = listOf(
        aa('A', "Ala", "Alanine", "Nonpolar Aliphatic", 89.09, 2.34, 9.69, null, 6.00, 1.8, listOf("GCU", "GCC", "GCA", "GCG"),
            listOf(atom("CB", "C", 0.35f, 0.28f, 0.12f, -1)),
            "Small hydrophobic methyl side chain; strong α-helix stabilizer."),
        aa('R', "Arg", "Arginine", "Basic (+)", 174.20, 2.17, 9.04, 12.48, 10.76, -4.5, listOf("CGU", "CGC", "CGA", "CGG", "AGA", "AGG"),
            listOf(
                atom("CB", "C", 0.30f, 0.25f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.42f, 0.14f, 0),
                atom("CD", "C", 0.80f, 0.25f, 0.22f, 1),
                atom("NE", "N", 1.02f, 0.42f, 0.25f, 2),
                atom("CZ", "C", 1.24f, 0.30f, 0.28f, 3),
                atom("NH1", "N", 1.42f, 0.46f, 0.30f, 4),
                atom("NH2", "N", 1.28f, 0.06f, 0.28f, 4)
            ),
            "Guanidinium group stays +1 charged across all physiological pH; mediates DNA/RNA phosphate salt bridges."),
        aa('N', "Asn", "Asparagine", "Polar Uncharged", 132.12, 2.02, 8.80, null, 5.41, -3.5, listOf("AAU", "AAC"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("CG", "C", 0.58f, 0.42f, 0.12f, 0),
                atom("OD1", "O", 0.62f, 0.65f, 0.14f, 1),
                atom("ND2", "N", 0.80f, 0.28f, 0.16f, 1)
            ),
            "Amide side chain accepts N-linked glycosylation at N-X-S/T motifs (e.g. viral Env/Spike glycan shields)."),
        aa('D', "Asp", "Aspartate", "Acidic (-)", 133.10, 1.88, 9.60, 3.65, 2.77, -3.5, listOf("GAU", "GAC"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("CG", "C", 0.58f, 0.42f, 0.12f, 0),
                atom("OD1", "O", 0.62f, 0.66f, 0.14f, 1),
                atom("OD2", "O", 0.80f, 0.28f, 0.15f, 1)
            ),
            "Negatively charged β-carboxyl group; frequent catalytic nucleophile/general base in proteases."),
        aa('C', "Cys", "Cysteine", "Polar Uncharged", 121.16, 1.96, 10.28, 8.18, 5.07, 2.5, listOf("UGU", "UGC"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.10f, -1),
                atom("SG", "S", 0.62f, 0.42f, 0.18f, 0)
            ),
            "Thiol (-SH) oxidizes to covalent disulfide (S-S) bridges stabilizing secreted antibodies and cytokines."),
        aa('Q', "Gln", "Glutamine", "Polar Uncharged", 146.15, 2.17, 9.13, null, 5.65, -3.5, listOf("CAA", "CAG"),
            listOf(
                atom("CB", "C", 0.30f, 0.25f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.40f, 0.14f, 0),
                atom("CD", "C", 0.80f, 0.26f, 0.20f, 1),
                atom("OE1", "O", 0.85f, 0.02f, 0.22f, 2),
                atom("NE2", "N", 1.02f, 0.40f, 0.24f, 2)
            ),
            "Long polar amide side chain for H-bond networks and nitrogen transport."),
        aa('E', "Glu", "Glutamate", "Acidic (-)", 147.13, 2.19, 9.67, 4.25, 3.22, -3.5, listOf("GAA", "GAG"),
            listOf(
                atom("CB", "C", 0.30f, 0.25f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.40f, 0.14f, 0),
                atom("CD", "C", 0.80f, 0.26f, 0.20f, 1),
                atom("OE1", "O", 0.85f, 0.02f, 0.22f, 2),
                atom("OE2", "O", 1.02f, 0.40f, 0.24f, 2)
            ),
            "γ-Carboxylate (-1 at pH 7.4); forms salt bridges and chelates Mg2+/Ca2+ cofactors."),
        aa('G', "Gly", "Glycine", "Nonpolar Aliphatic", 75.07, 2.34, 9.60, null, 5.97, -0.4, listOf("GGU", "GGC", "GGA", "GGG"),
            listOf(atom("HA2", "H", 0.24f, 0.22f, 0.08f, -1)),
            "Achiral side chain (H); grants maximal backbone Ramachandran flexibility in tight β-turns and CDR loops."),
        aa('H', "His", "Histidine", "Basic (+)", 155.16, 1.82, 9.17, 6.00, 7.59, -3.2, listOf("CAU", "CAC"),
            listOf(
                atom("CB", "C", 0.30f, 0.24f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.38f, 0.12f, 0),
                atom("ND1", "N", 0.68f, 0.62f, 0.14f, 1),
                atom("CE1", "C", 0.92f, 0.62f, 0.16f, 2),
                atom("NE2", "N", 0.96f, 0.38f, 0.16f, 3),
                atom("CD2", "C", 0.72f, 0.22f, 0.14f, 4)
            ),
            "Imidazole ring (pKaR ~6.0) acts as a reversible proton shuttle at physiological pH and coordinates Zn2+."),
        aa('I', "Ile", "Isoleucine", "Nonpolar Aliphatic", 131.17, 2.36, 9.60, null, 6.02, 4.5, listOf("AUU", "AUC", "AUA"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("CG1", "C", 0.56f, 0.44f, 0.18f, 0),
                atom("CD1", "C", 0.80f, 0.56f, 0.24f, 1),
                atom("CG2", "C", 0.40f, 0.12f, -0.18f, 0)
            ),
            "Branched β-carbon hydrophobic residue favoring β-sheets and tightly packed hydrophobic protein cores."),
        aa('L', "Leu", "Leucine", "Nonpolar Aliphatic", 131.17, 2.36, 9.60, null, 5.98, 3.8, listOf("UUA", "UUG", "CUU", "CUC", "CUA", "CUG"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("CG", "C", 0.58f, 0.42f, 0.12f, 0),
                atom("CD1", "C", 0.82f, 0.56f, 0.22f, 1),
                atom("CD2", "C", 0.64f, 0.22f, -0.16f, 1)
            ),
            "Classic leucine-zipper dimerization residue; high α-helix propensity."),
        aa('K', "Lys", "Lysine", "Basic (+)", 146.19, 2.18, 8.95, 10.53, 9.74, -3.9, listOf("AAA", "AAG"),
            listOf(
                atom("CB", "C", 0.30f, 0.24f, 0.08f, -1),
                atom("CG", "C", 0.54f, 0.40f, 0.12f, 0),
                atom("CD", "C", 0.78f, 0.26f, 0.18f, 1),
                atom("CE", "C", 1.02f, 0.42f, 0.22f, 2),
                atom("NZ", "N", 1.24f, 0.28f, 0.28f, 3)
            ),
            "ε-Amino group (+1 at pH 7.4); site of histone acetylation/ubiquitination and electrostatic surface interactions."),
        aa('M', "Met", "Methionine", "Nonpolar Aliphatic", 149.21, 2.28, 9.21, null, 5.74, 1.9, listOf("AUG"),
            listOf(
                atom("CB", "C", 0.32f, 0.25f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.42f, 0.14f, 0),
                atom("SD", "S", 0.84f, 0.28f, 0.20f, 1),
                atom("CE", "C", 1.06f, 0.44f, 0.26f, 2)
            ),
            "Universal eukaryotic translation initiation residue (AUG start codon); unbranched thioether chain."),
        aa('F', "Phe", "Phenylalanine", "Aromatic", 165.19, 1.83, 9.13, null, 5.48, 2.8, listOf("UUU", "UUC"),
            listOf(
                atom("CB", "C", 0.32f, 0.24f, 0.08f, -1),
                atom("CG", "C", 0.58f, 0.38f, 0.12f, 0),
                atom("CD1", "C", 0.72f, 0.60f, 0.16f, 1),
                atom("CE1", "C", 0.96f, 0.66f, 0.18f, 2),
                atom("CZ", "C", 1.12f, 0.46f, 0.16f, 3),
                atom("CE2", "C", 0.98f, 0.24f, 0.12f, 4),
                atom("CD2", "C", 0.74f, 0.18f, 0.10f, 5)
            ),
            "Planar hydrophobic phenyl ring; drives π-π stacking in receptor-ligand binding pockets."),
        aa('P', "Pro", "Proline", "Nonpolar Aliphatic", 115.13, 1.99, 10.60, null, 6.30, -1.6, listOf("CCU", "CCC", "CCA", "CCG"),
            listOf(
                atom("CB", "C", 0.28f, 0.30f, 0.12f, -1),
                atom("CG", "C", 0.06f, 0.48f, 0.18f, 0),
                atom("CD", "C", -0.22f, 0.32f, 0.10f, 1)
            ),
            "Cyclic imino ring locks φ angle (~-65°); acts as an α-helix breaker and forms polyproline SH3-binding helices."),
        aa('S', "Ser", "Serine", "Polar Uncharged", 105.09, 2.21, 9.15, null, 5.68, -0.8, listOf("UCU", "UCC", "UCA", "UCG", "AGU", "AGC"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("OG", "O", 0.58f, 0.42f, 0.14f, 0)
            ),
            "Primary hydroxyl (-OH); major phosphorylation switch in JAK-STAT/MAPK cascades and serine protease catalytic triad."),
        aa('T', "Thr", "Threonine", "Polar Uncharged", 119.12, 2.09, 9.10, null, 5.60, -0.7, listOf("ACU", "ACC", "ACA", "ACG"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("OG1", "O", 0.56f, 0.46f, 0.16f, 0),
                atom("CG2", "C", 0.44f, 0.12f, -0.18f, 0)
            ),
            "Chiral β-branched hydroxyl; accepts O-linked glycosylation and Ser/Thr kinase phosphorylation."),
        aa('W', "Trp", "Tryptophan", "Aromatic", 204.23, 2.83, 9.39, null, 5.89, -0.9, listOf("UGG"),
            listOf(
                atom("CB", "C", 0.30f, 0.24f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.38f, 0.12f, 0),
                atom("CD1", "C", 0.68f, 0.60f, 0.16f, 1),
                atom("NE1", "N", 0.92f, 0.62f, 0.18f, 2),
                atom("CE2", "C", 0.98f, 0.38f, 0.16f, 3),
                atom("CZ2", "C", 1.22f, 0.24f, 0.18f, 4),
                atom("CH2", "C", 1.24f, 0.00f, 0.14f, 5),
                atom("CZ3", "C", 1.02f, -0.10f, 0.10f, 6),
                atom("CE3", "C", 0.78f, 0.04f, 0.08f, 7),
                atom("CD2", "C", 0.74f, 0.26f, 0.10f, 1)
            ),
            "Bulkiest aromatic indole ring; dominates 280 nm UV protein absorbance and anchors membrane-water interfaces."),
        aa('Y', "Tyr", "Tyrosine", "Aromatic", 181.19, 2.20, 9.11, 10.07, 5.66, -1.3, listOf("UAU", "UAC"),
            listOf(
                atom("CB", "C", 0.30f, 0.24f, 0.08f, -1),
                atom("CG", "C", 0.56f, 0.38f, 0.12f, 0),
                atom("CD1", "C", 0.70f, 0.60f, 0.16f, 1),
                atom("CE1", "C", 0.94f, 0.66f, 0.18f, 2),
                atom("CZ", "C", 1.10f, 0.46f, 0.16f, 3),
                atom("OH", "O", 1.34f, 0.48f, 0.18f, 4),
                atom("CE2", "C", 0.96f, 0.24f, 0.12f, 4),
                atom("CD2", "C", 0.72f, 0.18f, 0.10f, 6)
            ),
            "Phenolic -OH phosphorylated by receptor tyrosine kinases (LCK, ZAP-70, JAK) inside ITAM signaling motifs."),
        aa('V', "Val", "Valine", "Nonpolar Aliphatic", 117.15, 2.32, 9.62, null, 5.96, 4.2, listOf("GUU", "GUC", "GUA", "GUG"),
            listOf(
                atom("CB", "C", 0.32f, 0.26f, 0.08f, -1),
                atom("CG1", "C", 0.56f, 0.44f, 0.16f, 0),
                atom("CG2", "C", 0.44f, 0.10f, -0.18f, 0)
            ),
            "Symmetric β-branched hydrophobic side chain strongly favoring β-strands.")
    )

    private fun aa(
        c1: Char, c3: String, n: String, cat: String, mw: Double, p1: Double, p2: Double, pr: Double?,
        pi: Double, h: Double, cod: List<String>, sc: List<SideChainAtomSpec>, note: String
    ) = AminoAcid3DInfo(c1, c3, n, cat, mw, p1, p2, pr, pi, h, cod, sc, note)

    private fun atom(l: String, e: String, dx: Float, dy: Float, dz: Float, p: Int) =
        SideChainAtomSpec(l, e, dx, dy, dz, p)

    fun chargeAtPh(aa: AminoAcid3DInfo, ph: Double): Double {
        // Free zwitterion model: +1 from N-term (pKa2), -1 from C-term (pKa1), +/- from R-group
        val nCharge = 1.0 / (1.0 + Math.pow(10.0, ph - aa.pKa2))
        val cCharge = -1.0 / (1.0 + Math.pow(10.0, aa.pKa1 - ph))
        val rCharge = when {
            aa.pKaR == null -> 0.0
            aa.category.startsWith("Basic") -> 1.0 / (1.0 + Math.pow(10.0, ph - aa.pKaR))
            else -> -1.0 / (1.0 + Math.pow(10.0, aa.pKaR - ph))
        }
        return nCharge + cCharge + rCharge
    }

    fun analyzeDna(rawSeq: String, helixForm: String = "B-DNA"): DnaBasePairAnalysis {
        val cleaned = rawSeq.uppercase().replace('U', 'T').filter { it in "ATGC" }
            .ifEmpty { "ATGGCGTACGTAGCTAGC" }
            .take(48)
        val comp = cleaned.map {
            when (it) {
                'A' -> 'T'
                'T' -> 'A'
                'G' -> 'C'
                else -> 'G'
            }
        }.joinToString("")
        val gcCount = cleaned.count { it == 'G' || it == 'C' }
        val atCount = cleaned.length - gcCount
        val gcPct = (gcCount * 100.0) / cleaned.length
        val tm = if (cleaned.length <= 14) {
            2.0 * atCount + 4.0 * gcCount
        } else {
            64.9 + 41.0 * (gcCount - 16.4) / cleaned.length
        }
        val cpg = (0 until cleaned.length - 1).count { cleaned[it] == 'C' && cleaned[it + 1] == 'G' }
        val hBonds = gcCount * 3 + atCount * 2
        val (pitch, bpTurn) = when (helixForm) {
            "A-DNA" -> 28.2 to 11.0
            "Z-DNA" -> 45.0 to 12.0
            else -> 34.0 to 10.5
        }
        return DnaBasePairAnalysis(
            cleanedSeq = cleaned,
            complementSeq = comp,
            helixForm = helixForm,
            lengthBp = cleaned.length,
            gcPct = gcPct,
            atPct = 100.0 - gcPct,
            tmCelsius = tm,
            dsMwKda = cleaned.length * 0.65,
            cpgCount = cpg,
            hBondsTotal = hBonds,
            helicalPitchAngstrom = pitch,
            bpPerTurn = bpTurn
        )
    }

    fun analyzeRna(rawSeq: String): RnaFoldAnalysis {
        val cleaned = rawSeq.uppercase().replace('T', 'U').filter { it in "AUGC" }
            .ifEmpty { "GGUCUCUCUGGUUAGACCAGAU" }
            .take(60)
        val n = cleaned.length
        // Nussinov-style minimal hairpin/stem DP (min hairpin loop = 3)
        val dp = Array(n) { IntArray(n) }
        fun canPair(a: Char, b: Char): Boolean = (a == 'A' && b == 'U') || (a == 'U' && b == 'A') ||
            (a == 'G' && b == 'C') || (a == 'C' && b == 'G') || (a == 'G' && b == 'U') || (a == 'U' && b == 'G')
        fun pairWeight(a: Char, b: Char): Int = when {
            (a == 'G' && b == 'C') || (a == 'C' && b == 'G') -> 3
            (a == 'A' && b == 'U') || (a == 'U' && b == 'A') -> 2
            (a == 'G' && b == 'U') || (a == 'U' && b == 'G') -> 1
            else -> 0
        }

        for (len in 4..n) {
            for (i in 0..(n - len)) {
                val j = i + len - 1
                var best = max(dp[i + 1][j], dp[i][j - 1])
                if (j - i >= 4 && canPair(cleaned[i], cleaned[j])) {
                    best = max(best, dp[i + 1][j - 1] + pairWeight(cleaned[i], cleaned[j]))
                }
                for (k in (i + 1) until j) {
                    best = max(best, dp[i][k] + dp[k + 1][j])
                }
                dp[i][j] = best
            }
        }

        val pairs = mutableListOf<Pair<Int, Int>>()
        fun traceback(i: Int, j: Int) {
            if (i >= j) return
            if (dp[i][j] == dp[i + 1][j]) {
                traceback(i + 1, j)
            } else if (dp[i][j] == dp[i][j - 1]) {
                traceback(i, j - 1)
            } else if (j - i >= 4 && canPair(cleaned[i], cleaned[j]) &&
                dp[i][j] == dp[i + 1][j - 1] + pairWeight(cleaned[i], cleaned[j])
            ) {
                pairs += i to j
                traceback(i + 1, j - 1)
            } else {
                for (k in (i + 1) until j) {
                    if (dp[i][j] == dp[i][k] + dp[k + 1][j]) {
                        traceback(i, k)
                        traceback(k + 1, j)
                        break
                    }
                }
            }
        }
        if (n > 0) traceback(0, n - 1)

        val bracket = CharArray(n) { '.' }
        var wc = 0
        var gu = 0
        pairs.forEach { (i, j) ->
            bracket[i] = '('
            bracket[j] = ')'
            val a = cleaned[i]
            val b = cleaned[j]
            if ((a == 'G' && b == 'U') || (a == 'U' && b == 'G')) gu++ else wc++
        }
        val gcPct = (cleaned.count { it == 'G' || it == 'C' } * 100.0) / n.coerceAtLeast(1)
        val deltaG = -(wc * 1.95 + gu * 0.75) + (n - pairs.size * 2) * 0.12
        val peptide = BioinformaticsEngine.translate(cleaned)

        return RnaFoldAnalysis(
            cleanedSeq = cleaned,
            dotBracket = String(bracket),
            lengthNt = n,
            gcPct = gcPct,
            wcPairs = wc,
            wobbleGuPairs = gu,
            unpairedCount = n - pairs.size * 2,
            estimatedDeltaGKcal = deltaG,
            translatedPeptide = peptide,
            pairs = pairs
        )
    }

    fun analyzeProteinFold(rawInput: String): ProteinFoldAnalysis {
        val validAa = "ACDEFGHIKLMNPQRSTVWY"
        val cleaned = rawInput.uppercase().filter { it in validAa }
            .ifEmpty { "MKVLWAALLVTFLAGCQAKVEQAVETEPEPELRQQTEW" }
            .take(120)
        val stats = BioinformaticsEngine.analyzeProtein(cleaned)
        val helixPref = setOf('A', 'E', 'L', 'M', 'Q', 'K', 'R', 'H')
        val sheetPref = setOf('V', 'I', 'Y', 'F', 'W', 'T', 'C')
        val sec = cleaned.map { ch ->
            when (ch) {
                in helixPref -> SecStructType.HELIX
                in sheetPref -> SecStructType.SHEET
                else -> SecStructType.COIL
            }
        }
        val hPct = sec.count { it == SecStructType.HELIX } * 100.0 / cleaned.length
        val sPct = sec.count { it == SecStructType.SHEET } * 100.0 / cleaned.length
        val cPct = sec.count { it == SecStructType.COIL } * 100.0 / cleaned.length

        // Estimate theoretical pI via bisection
        val aaMap = AMINO_ACIDS.associateBy { it.code1 }
        fun netChargeAt(ph: Double): Double {
            var q = (1.0 / (1.0 + Math.pow(10.0, ph - 8.0))) - (1.0 / (1.0 + Math.pow(10.0, 3.6 - ph)))
            cleaned.forEach { ch ->
                val info = aaMap[ch]
                val pr = info?.pKaR
                if (pr != null) {
                    q += if (info.category.startsWith("Basic")) {
                        1.0 / (1.0 + Math.pow(10.0, ph - pr))
                    } else {
                        -1.0 / (1.0 + Math.pow(10.0, pr - ph))
                    }
                }
            }
            return q
        }
        var lo = 1.0
        var hi = 14.0
        repeat(24) {
            val mid = (lo + hi) * 0.5
            if (netChargeAt(mid) > 0) lo = mid else hi = mid
        }
        val pi = (lo + hi) * 0.5
        val cysPairs = cleaned.count { it == 'C' } / 2

        return ProteinFoldAnalysis(
            sequence = cleaned,
            lengthAa = cleaned.length,
            mwKda = stats.molecularWeightDa / 1000.0,
            netChargePh74 = netChargeAt(7.4),
            theoreticalPi = pi,
            gravy = stats.meanHydropathy,
            helixPct = hPct,
            sheetPct = sPct,
            coilPct = cPct,
            cysDisulfidePairs = cysPairs,
            secStruct = sec,
            hydropathyProfile = stats.profile
        )
    }
}
