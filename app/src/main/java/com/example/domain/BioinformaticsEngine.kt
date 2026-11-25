package com.example.domain

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

data class FastaRecord(val header: String, val sequence: String)

data class OrfHit(
    val frame: Int,
    val startNt: Int,
    val endNt: Int,
    val aaLength: Int,
    val protein: String
)

data class AlignmentResult(
    val alignedA: String,
    val alignedB: String,
    val midline: String,
    val score: Int,
    val identityPct: Double
)

data class TreeNode(
    val label: String,
    val height: Double,
    val left: TreeNode? = null,
    val right: TreeNode? = null
)

data class ProteinStats(
    val length: Int,
    val molecularWeightDa: Double,
    val netChargePh7: Double,
    val meanHydropathy: Double,
    val profile: List<Double>
)

data class SnvResult(
    val refCodon: String,
    val altCodon: String,
    val refAa: Char,
    val altAa: Char,
    val classification: String
)

data class DigestFragment(val start: Int, val end: Int, val lengthBp: Int)

data class DescriptiveStats(
    val column: String,
    val n: Int,
    val mean: Double,
    val sd: Double,
    val median: Double,
    val q1: Double,
    val q3: Double,
    val min: Double,
    val max: Double
)

data class OlsFit(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double
)

data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

object BioinformaticsEngine {
    val DEMO_FASTA = """>Demo_Human_IL2_CDS (Synthetic Teaching Excerpt)
ATGTACAGGATGCAACTCCTGTCTTGCATTGCACTAAGTCTTGCACTTGTCACAAACAGTGCACCTACTTCAAGTTCTACAAAGAAAACACAGCTACAACTGGAGCATTTACTGCTGGATTTACAGATGATTTTGAATGGAATTAATAATTACAAGAATCCCAAACTCACCAGGATGCTCACATTTAAGTTTTACATGCCCAAGAAGGCCACAGAACTGAAACATCTTCAGTGTCTAGAAGAAGAACTCAAACCT"""

    val DEMO_MULTI_FASTA = """>SeqA_Human
ATGGTGCTGTCTCCTGCCGACAAGACCAACGTCAAGGCCGCCTGGGGTAAGGTCGGCGCGCACGCT
>SeqB_Primate
ATGGTGCTGTCTCCTGCCGACAAGACCAACATCAAGGCCGCCTGGGGTAAGGTCGGCGCGCACGCT
>SeqC_Rodent
ATGGTGCTTTCTGCTGAAGACAAAAACCACATCAAGGCCACCTGGGATAAGATTGGCGGCCATGCT
>SeqD_Avian
ATGGTGCTGTCTGCCGCTGACAAGACCAACCTCAAGGGCATCTTCTCCAAGATCGGCGGCCATGCT"""

    val DEMO_EXPRESSION_GENES = listOf("IFNG", "IL2", "FOXP3", "GZMB", "CXCL10", "ISG15", "TNFA", "CD19")
    val DEMO_EXPRESSION_SAMPLES = listOf("Healthy_1", "Healthy_2", "Acute_Viral_1", "Acute_Viral_2", "Resolved_1")
    val DEMO_EXPRESSION_MATRIX = listOf(
        listOf(-0.8, -0.7, 2.1, 2.3, -0.1),
        listOf(-0.6, -0.5, 1.7, 1.9, 0.2),
        listOf(0.4, 0.3, -0.9, -0.7, 1.6),
        listOf(-0.9, -0.8, 2.4, 2.1, -0.2),
        listOf(-0.7, -0.9, 2.2, 2.5, 0.0),
        listOf(-1.0, -0.9, 2.6, 2.4, -0.3),
        listOf(-0.4, -0.3, 1.8, 1.5, 0.1),
        listOf(0.5, 0.6, -0.5, -0.4, 1.4)
    )

    val DEMO_CYTOKINE_CSV = """Day,IL6_pg_mL,IFNg_pg_mL,CD4_count_per_uL,ViralLoad_log10
0,4.2,2.1,820,4.8
3,28.5,14.2,690,5.6
7,54.1,42.8,560,4.9
10,31.0,61.4,610,3.4
14,15.4,39.2,710,2.1
21,7.8,16.5,790,1.2
28,5.1,8.3,840,0.6"""

    private val CODON_TABLE = mapOf(
        "UUU" to 'F', "UUC" to 'F', "UUA" to 'L', "UUG" to 'L',
        "CUU" to 'L', "CUC" to 'L', "CUA" to 'L', "CUG" to 'L',
        "AUU" to 'I', "AUC" to 'I', "AUA" to 'I', "AUG" to 'M',
        "GUU" to 'V', "GUC" to 'V', "GUA" to 'V', "GUG" to 'V',
        "UCU" to 'S', "UCC" to 'S', "UCA" to 'S', "UCG" to 'S',
        "CCU" to 'P', "CCC" to 'P', "CCA" to 'P', "CCG" to 'P',
        "ACU" to 'T', "ACC" to 'T', "ACA" to 'T', "ACG" to 'T',
        "GCU" to 'A', "GCC" to 'A', "GCA" to 'A', "GCG" to 'A',
        "UAU" to 'Y', "UAC" to 'Y', "UAA" to '*', "UAG" to '*',
        "CAU" to 'H', "CAC" to 'H', "CAA" to 'Q', "CAG" to 'Q',
        "AAU" to 'N', "AAC" to 'N', "AAA" to 'K', "AAG" to 'K',
        "GAU" to 'D', "GAC" to 'D', "GAA" to 'E', "GAG" to 'E',
        "UGU" to 'C', "UGC" to 'C', "UGA" to '*', "UGG" to 'W',
        "CGU" to 'R', "CGC" to 'R', "CGA" to 'R', "CGG" to 'R',
        "AGU" to 'S', "AGC" to 'S', "AGA" to 'R', "AGG" to 'R',
        "GGU" to 'G', "GGC" to 'G', "GGA" to 'G', "GGG" to 'G'
    )

    private val KD_HYDROPATHY = mapOf(
        'I' to 4.5, 'V' to 4.2, 'L' to 3.8, 'F' to 2.8, 'C' to 2.5,
        'M' to 1.9, 'A' to 1.8, 'G' to -0.4, 'T' to -0.7, 'S' to -0.8,
        'W' to -0.9, 'Y' to -1.3, 'P' to -1.6, 'H' to -3.2, 'E' to -3.5,
        'Q' to -3.5, 'D' to -3.5, 'N' to -3.5, 'K' to -3.9, 'R' to -4.5
    )

    private val AA_MW = mapOf(
        'A' to 89.09, 'R' to 174.20, 'N' to 132.12, 'D' to 133.10, 'C' to 121.16,
        'E' to 147.13, 'Q' to 146.15, 'G' to 75.07, 'H' to 155.16, 'I' to 131.18,
        'L' to 131.18, 'K' to 146.19, 'M' to 149.21, 'F' to 165.19, 'P' to 115.13,
        'S' to 105.09, 'T' to 119.12, 'W' to 204.23, 'Y' to 181.19, 'V' to 117.15
    )

    val RESTRICTION_ENZYMES = mapOf(
        "EcoRI (GAATTC)" to Pair("GAATTC", 1),
        "BamHI (GGATCC)" to Pair("GGATCC", 1),
        "HindIII (AAGCTT)" to Pair("AAGCTT", 1),
        "XbaI (TCTAGA)" to Pair("TCTAGA", 1),
        "NotI (GCGGCCGC)" to Pair("GCGGCCGC", 2),
        "EcoRV (GATATC)" to Pair("GATATC", 3)
    )

    fun parseFasta(text: String): List<FastaRecord> {
        val records = mutableListOf<FastaRecord>()
        var header = "Sequence_1"
        val seqBuf = StringBuilder()
        var hasContent = false
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith(">")) {
                if (hasContent && seqBuf.isNotEmpty()) {
                    records += FastaRecord(header, seqBuf.toString().uppercase())
                    seqBuf.clear()
                }
                header = line.removePrefix(">").trim().ifEmpty { "Sequence_${records.size + 1}" }
                hasContent = true
            } else {
                hasContent = true
                seqBuf.append(line.replace(Regex("[^A-Za-z*]"), ""))
            }
        }
        if (seqBuf.isNotEmpty()) {
            records += FastaRecord(header, seqBuf.toString().uppercase())
        }
        return records
    }

    fun detectSeqType(seq: String): String {
        val clean = seq.uppercase().replace(Regex("[^A-Z]"), "")
        if (clean.isEmpty()) return "Empty"
        val ntSet = setOf('A', 'C', 'G', 'T', 'U', 'N')
        val ntRatio = clean.count { it in ntSet }.toDouble() / clean.length
        return if (ntRatio > 0.85) {
            if ('U' in clean && 'T' !in clean) "RNA" else "DNA"
        } else "Protein"
    }

    fun transcribe(dna: String): String = dna.uppercase().replace('T', 'U')

    fun revcomp(seq: String): String {
        val map = mapOf('A' to 'T', 'T' to 'A', 'U' to 'A', 'C' to 'G', 'G' to 'C', 'N' to 'N')
        return seq.uppercase().reversed().map { map[it] ?: 'N' }.joinToString("")
    }

    fun gcContentPct(seq: String): Double {
        val clean = seq.uppercase().filter { it in "ACGTU" }
        if (clean.isEmpty()) return 0.0
        val gc = clean.count { it == 'G' || it == 'C' }
        return 100.0 * gc / clean.length
    }

    fun estimateTmCelsius(seq: String): Double {
        val clean = seq.uppercase().filter { it in "ACGTU" }
        if (clean.isEmpty()) return 0.0
        val at = clean.count { it == 'A' || it == 'T' || it == 'U' }
        val gc = clean.count { it == 'G' || it == 'C' }
        return if (clean.length <= 14) {
            2.0 * at + 4.0 * gc
        } else {
            64.9 + 41.0 * (gc - 16.4) / clean.length
        }
    }

    fun translate(seq: String, frameOffset: Int = 0): String {
        val rna = transcribe(seq).drop(frameOffset.coerceIn(0, 2))
        val out = StringBuilder()
        var i = 0
        while (i + 3 <= rna.length) {
            val codon = rna.substring(i, i + 3)
            out.append(CODON_TABLE[codon] ?: 'X')
            i += 3
        }
        return out.toString()
    }

    fun findOrfs(dna: String, minAa: Int = 20): List<OrfHit> {
        val hits = mutableListOf<OrfHit>()
        val clean = dna.uppercase().filter { it in "ACGTU" }
        val rc = revcomp(clean)
        val strands = listOf(Pair(clean, listOf(1, 2, 3)), Pair(rc, listOf(-1, -2, -3)))
        for ((seq, frames) in strands) {
            for (idx in 0..2) {
                val aa = translate(seq, idx)
                var pos = 0
                while (pos < aa.length) {
                    val mIdx = aa.indexOf('M', pos)
                    if (mIdx < 0) break
                    val stopIdx = aa.indexOf('*', mIdx)
                    val endIdx = if (stopIdx >= 0) stopIdx else aa.length
                    val len = endIdx - mIdx
                    if (len >= minAa) {
                        hits += OrfHit(
                            frame = frames[idx],
                            startNt = idx + mIdx * 3 + 1,
                            endNt = idx + endIdx * 3,
                            aaLength = len,
                            protein = aa.substring(mIdx, endIdx)
                        )
                    }
                    pos = if (stopIdx >= 0) stopIdx + 1 else aa.length
                }
            }
        }
        return hits.sortedByDescending { it.aaLength }
    }

    fun alignSequences(
        seqA: String,
        seqB: String,
        global: Boolean = true,
        match: Int = 2,
        mismatch: Int = -1,
        gap: Int = -2
    ): AlignmentResult {
        val a = seqA.trim().uppercase().take(240)
        val b = seqB.trim().uppercase().take(240)
        val n = a.length
        val m = b.length
        if (n == 0 || m == 0) return AlignmentResult("", "", "", 0, 0.0)

        val dp = Array(n + 1) { IntArray(m + 1) }
        val trace = Array(n + 1) { IntArray(m + 1) } // 1 diag, 2 up, 3 left
        if (global) {
            for (i in 1..n) { dp[i][0] = i * gap; trace[i][0] = 2 }
            for (j in 1..m) { dp[0][j] = j * gap; trace[0][j] = 3 }
        }

        var bestI = n
        var bestJ = m
        var bestScore = Int.MIN_VALUE

        for (i in 1..n) {
            for (j in 1..m) {
                val diag = dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) match else mismatch
                val up = dp[i - 1][j] + gap
                val left = dp[i][j - 1] + gap
                var cell = max(diag, max(up, left))
                var dir = when {
                    cell == diag -> 1
                    cell == up -> 2
                    else -> 3
                }
                if (!global && cell < 0) {
                    cell = 0
                    dir = 0
                }
                dp[i][j] = cell
                trace[i][j] = dir
                if (!global && cell >= bestScore) {
                    bestScore = cell
                    bestI = i
                    bestJ = j
                }
            }
        }
        if (global) {
            bestScore = dp[n][m]
            bestI = n
            bestJ = m
        }

        val alnA = StringBuilder()
        val alnB = StringBuilder()
        var i = bestI
        var j = bestJ
        while ((global && (i > 0 || j > 0)) || (!global && i > 0 && j > 0 && dp[i][j] > 0)) {
            when (trace[i][j]) {
                1 -> { alnA.append(a[i - 1]); alnB.append(b[j - 1]); i--; j-- }
                2 -> { alnA.append(a[i - 1]); alnB.append('-'); i-- }
                3 -> { alnA.append('-'); alnB.append(b[j - 1]); j-- }
                else -> break
            }
        }
        val ra = alnA.reverse().toString()
        val rb = alnB.reverse().toString()
        val mid = buildString {
            for (k in ra.indices) {
                append(if (ra[k] == rb[k] && ra[k] != '-') '|' else if (ra[k] == '-' || rb[k] == '-') ' ' else '.')
            }
        }
        val matches = ra.indices.count { ra[it] == rb[it] && ra[it] != '-' }
        val pct = if (ra.isEmpty()) 0.0 else 100.0 * matches / ra.length
        return AlignmentResult(ra, rb, mid, bestScore, pct)
    }

    fun jukesCantorDistance(seqA: String, seqB: String): Double {
        val len = minOf(seqA.length, seqB.length)
        if (len == 0) return 0.0
        var diffs = 0
        var valid = 0
        for (i in 0 until len) {
            val c1 = seqA[i].uppercaseChar()
            val c2 = seqB[i].uppercaseChar()
            if (c1 in "ACGTU" && c2 in "ACGTU") {
                valid++
                if (c1 != c2) diffs++
            }
        }
        if (valid == 0) return 0.0
        val p = diffs.toDouble() / valid
        return if (p < 0.74) -0.75 * ln(1.0 - (4.0 / 3.0) * p) else 2.5
    }

    fun buildUpgmaTree(records: List<FastaRecord>): TreeNode? {
        if (records.size < 2) return records.firstOrNull()?.let { TreeNode(it.header, 0.0) }
        val clusters = records.map { Pair(TreeNode(it.header, 0.0), 1) }.toMutableList()
        val n = records.size
        val dist = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = jukesCantorDistance(records[i].sequence, records[j].sequence)
                dist[i][j] = d
                dist[j][i] = d
            }
        }
        val active = (0 until n).toMutableList()
        val nodes = records.map { TreeNode(it.header, 0.0) }.toMutableList()
        val sizes = IntArray(n) { 1 }
        val mat = Array(n * 2) { DoubleArray(n * 2) }
        for (i in 0 until n) for (j in 0 until n) mat[i][j] = dist[i][j]
        val nodePool = nodes.toMutableList()
        val sizePool = sizes.toMutableList()

        while (active.size > 1) {
            var bi = 0
            var bj = 1
            var bestD = Double.MAX_VALUE
            for (ii in active.indices) {
                for (jj in ii + 1 until active.size) {
                    val u = active[ii]
                    val v = active[jj]
                    if (mat[u][v] < bestD) {
                        bestD = mat[u][v]
                        bi = ii
                        bj = jj
                    }
                }
            }
            val u = active[bi]
            val v = active[bj]
            val merged = TreeNode("", bestD / 2.0, nodePool[u], nodePool[v])
            val newIdx = nodePool.size
            nodePool += merged
            val newSize = sizePool[u] + sizePool[v]
            sizePool += newSize
            for (k in active) {
                if (k != u && k != v) {
                    val nd = (mat[u][k] * sizePool[u] + mat[v][k] * sizePool[v]) / newSize
                    mat[newIdx][k] = nd
                    mat[k][newIdx] = nd
                }
            }
            active.removeAt(bj)
            active.removeAt(bi)
            active.add(newIdx)
        }
        return nodePool[active.first()]
    }

    fun toNewick(node: TreeNode?): String {
        if (node == null) return ""
        if (node.left == null && node.right == null) return node.label.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val lLen = max(0.001, node.height - (node.left?.height ?: 0.0))
        val rLen = max(0.001, node.height - (node.right?.height ?: 0.0))
        return "(${toNewick(node.left)}:${"%.4f".format(lLen)},${toNewick(node.right)}:${"%.4f".format(rLen)})"
    }

    fun analyzeProtein(aaSeq: String): ProteinStats {
        val clean = aaSeq.uppercase().filter { it in AA_MW || it == '*' }.replace("*", "")
        if (clean.isEmpty()) return ProteinStats(0, 0.0, 0.0, 0.0, emptyList())
        val mw = clean.sumOf { AA_MW[it] ?: 110.0 } - 18.015 * max(0, clean.length - 1)
        val pos = clean.count { it == 'K' || it == 'R' } + 0.1 * clean.count { it == 'H' }
        val neg = clean.count { it == 'D' || it == 'E' }
        val profile = clean.map { KD_HYDROPATHY[it] ?: 0.0 }
        val meanHyd = profile.average()
        return ProteinStats(clean.length, mw, pos - neg, meanHyd, profile)
    }

    fun digestDna(seq: String, enzymeKey: String): List<DigestFragment> {
        val clean = seq.uppercase().filter { it in "ACGT" }
        val (site, cutOffset) = RESTRICTION_ENZYMES[enzymeKey] ?: return emptyList()
        val cuts = mutableListOf(0)
        var idx = clean.indexOf(site)
        while (idx >= 0) {
            cuts += (idx + cutOffset)
            idx = clean.indexOf(site, idx + 1)
        }
        cuts += clean.length
        return cuts.zipWithNext().map { (s, e) -> DigestFragment(s + 1, e, e - s) }.filter { it.lengthBp > 0 }
    }

    fun classifySnv(refCodonInput: String, altCodonInput: String): SnvResult? {
        val ref = transcribe(refCodonInput).filter { it in "ACGU" }
        val alt = transcribe(altCodonInput).filter { it in "ACGU" }
        if (ref.length != 3 || alt.length != 3) return null
        val rAa = CODON_TABLE[ref] ?: '?'
        val aAa = CODON_TABLE[alt] ?: '?'
        val cls = when {
            ref == alt -> "No nucleotide change"
            rAa == aAa -> "Synonymous (silent)"
            aAa == '*' && rAa != '*' -> "Nonsense (premature stop)"
            rAa == '*' && aAa != '*' -> "Stop-loss (readthrough)"
            else -> "Missense ($rAa -> $aAa)"
        }
        return SnvResult(ref, alt, rAa, aAa, cls)
    }

    fun parseCsv(raw: String): CsvTable {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return CsvTable(emptyList(), emptyList())
        val sep = if (lines.first().contains('\t')) '\t' else ','
        val headers = lines.first().split(sep).map { it.trim() }
        val rows = lines.drop(1).map { line -> line.split(sep).map { it.trim() } }
        return CsvTable(headers, rows)
    }

    fun extractNumericColumn(table: CsvTable, colIdx: Int): List<Double> =
        table.rows.mapNotNull { row -> row.getOrNull(colIdx)?.toDoubleOrNull() }

    fun describeColumn(name: String, values: List<Double>): DescriptiveStats? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val n = sorted.size
        val mean = sorted.average()
        val variance = if (n > 1) sorted.sumOf { (it - mean) * (it - mean) } / (n - 1) else 0.0
        val sd = sqrt(variance)
        fun quantile(q: Double): Double {
            val pos = q * (n - 1)
            val lo = pos.toInt()
            val hi = minOf(n - 1, lo + 1)
            val frac = pos - lo
            return sorted[lo] * (1.0 - frac) + sorted[hi] * frac
        }
        return DescriptiveStats(name, n, mean, sd, quantile(0.5), quantile(0.25), quantile(0.75), sorted.first(), sorted.last())
    }

    fun pearson(xs: List<Double>, ys: List<Double>): Double {
        val n = minOf(xs.size, ys.size)
        if (n < 2) return 0.0
        val mx = xs.take(n).average()
        val my = ys.take(n).average()
        var num = 0.0
        var dx2 = 0.0
        var dy2 = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - mx
            val dy = ys[i] - my
            num += dx * dy
            dx2 += dx * dx
            dy2 += dy * dy
        }
        val denom = sqrt(dx2 * dy2)
        return if (denom < 1e-12) 0.0 else num / denom
    }

    fun olsFit(xs: List<Double>, ys: List<Double>): OlsFit? {
        val n = minOf(xs.size, ys.size)
        if (n < 2) return null
        val mx = xs.take(n).average()
        val my = ys.take(n).average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            num += (xs[i] - mx) * (ys[i] - my)
            den += (xs[i] - mx) * (xs[i] - mx)
        }
        if (abs(den) < 1e-12) return null
        val slope = num / den
        val intercept = my - slope * mx
        val r = pearson(xs.take(n), ys.take(n))
        return OlsFit(slope, intercept, r * r)
    }
}
