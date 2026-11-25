package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.domain.AminoAcid3DInfo
import com.example.domain.DnaBasePairAnalysis
import com.example.domain.ProteinFoldAnalysis
import com.example.domain.RnaFoldAnalysis
import com.example.domain.SecStructType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun buildDnaHelix3DScene(analysis: DnaBasePairAnalysis): Scene3DModel {
    val seq = analysis.cleanedSeq.take(24)
    val comp = analysis.complementSeq.take(24)
    val n = seq.length.coerceAtLeast(1)

    val twistRad = when (analysis.helixForm) {
        "A-DNA" -> (32.7 * PI / 180.0).toFloat()
        "Z-DNA" -> (-30.0 * PI / 180.0).toFloat()
        else -> (34.3 * PI / 180.0).toFloat()
    }
    val radius = if (analysis.helixForm == "A-DNA") 0.54f else 0.44f
    val strandA = mutableListOf<Sphere3D>()
    val strandB = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()
    val bpSpheres = mutableListOf<Sphere3D>()

    fun baseColor(b: Char) = when (b) {
        'A' -> Color(0xFF22E5FF)
        'T' -> Color(0xFFFFC857)
        'G' -> Color(0xFF3DDC97)
        else -> Color(0xFFE04FB0)
    }

    for (i in 0 until n) {
        val t = (i - (n - 1) / 2f) / (n / 2f).coerceAtLeast(1f)
        val y = t * 0.86f
        val angle = i * twistRad
        val x1 = radius * cos(angle)
        val z1 = radius * sin(angle)
        val x2 = radius * cos(angle + PI.toFloat() * 0.88f)
        val z2 = radius * sin(angle + PI.toFloat() * 0.88f)

        val b1 = seq[i]
        val b2 = comp[i]
        strandA += Sphere3D("dna_53_$i", "5' ${b1}${i + 1}", x1, y, z1, 0.055f, Color(0xFF8B6BFF), 0.94f, "5'→3' Backbone")
        strandB += Sphere3D("dna_35_$i", "3' ${b2}${i + 1}", x2, y, z2, 0.055f, Color(0xFF2EE6C5), 0.94f, "3'→5' Backbone")

        val mx = (x1 + x2) * 0.5f
        val mz = (z1 + z2) * 0.5f
        val rx1 = (x1 + mx) * 0.5f
        val rz1 = (z1 + mz) * 0.5f
        val rx2 = (x2 + mx) * 0.5f
        val rz2 = (z2 + mz) * 0.5f

        val hBondCount = if (b1 == 'G' || b1 == 'C') 3 else 2
        bpSpheres += Sphere3D("bpA_$i", "bp ${i + 1}: $b1=$b2 ($hBondCount H-bonds)", rx1, y, rz1, 0.05f, baseColor(b1), 0.96f, "Base $b1")
        bpSpheres += Sphere3D("bpB_$i", "bp ${i + 1}: $b2=$b1 ($hBondCount H-bonds)", rx2, y, rz2, 0.05f, baseColor(b2), 0.96f, "Base $b2")

        bonds += Bond3D(x1, y, z1, rx1, y, rz1, baseColor(b1).copy(alpha = 0.7f), 2.2f)
        bonds += Bond3D(x2, y, z2, rx2, y, rz2, baseColor(b2).copy(alpha = 0.7f), 2.2f)
        bonds += Bond3D(rx1, y, rz1, rx2, y, rz2, Color.White.copy(alpha = if (hBondCount == 3) 0.75f else 0.42f), if (hBondCount == 3) 2.5f else 1.5f)
    }

    return Scene3DModel(
        title = "3D ${analysis.helixForm} Double Helix (${n} bp shown)",
        spheres = bpSpheres,
        bonds = bonds,
        backboneChain = listOf(strandA, strandB),
        caption = "GC ${"%.1f".format(analysis.gcPct)}% • Tm ${"%.1f".format(analysis.tmCelsius)}°C • Tap any base pair",
        legendItems = listOf(
            LegendItem3D("Adenine (A)", Color(0xFF22E5FF), "bpA_0"),
            LegendItem3D("Thymine (T)", Color(0xFFFFC857), "bpB_0"),
            LegendItem3D("Guanine (G)", Color(0xFF3DDC97), null),
            LegendItem3D("Cytosine (C)", Color(0xFFE04FB0), null),
            LegendItem3D("5'→3' Sugar-P", Color(0xFF8B6BFF), null)
        )
    )
}

fun buildRnaFold3DScene(analysis: RnaFoldAnalysis): Scene3DModel {
    val seq = analysis.cleanedSeq.take(48)
    val n = seq.length.coerceAtLeast(1)
    val backbone = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()

    fun ntColor(b: Char) = when (b) {
        'A' -> Color(0xFF22E5FF)
        'U' -> Color(0xFFFF5D73)
        'G' -> Color(0xFF3DDC97)
        else -> Color(0xFFE04FB0)
    }

    // Arrange folded hairpin/stem-loop in 3D: paired stems rise in helical antiparallel arms, unpaired residues arc in apical loop
    val coords = Array(n) { Triple(0f, 0f, 0f) }
    for (i in 0 until n) {
        val frac = i.toFloat() / (n - 1).coerceAtLeast(1)
        val angle = frac * (PI.toFloat() * 1.65f) - PI.toFloat() * 0.82f
        val foldDepth = if (analysis.dotBracket.getOrNull(i) != '.') 0.30f else 0.58f
        val x = sin(angle) * foldDepth
        val y = cos(angle) * 0.74f - 0.08f
        val z = 0.22f * sin(i * 0.55f)
        coords[i] = Triple(x, y, z)
        val stTag = when (analysis.dotBracket.getOrNull(i)) {
            '(' -> "5' Stem"
            ')' -> "3' Stem"
            else -> "Loop/Bulge"
        }
        backbone += Sphere3D(
            id = "rna_$i",
            label = "${seq[i]}${i + 1} ($stTag)",
            x = x,
            y = y,
            z = z,
            radius = 0.055f,
            color = ntColor(seq[i]),
            alpha = 0.95f,
            groupTag = stTag
        )
    }

    analysis.pairs.forEach { (i, j) ->
        if (i < n && j < n) {
            val (x1, y1, z1) = coords[i]
            val (x2, y2, z2) = coords[j]
            val isGu = (seq[i] == 'G' && seq[j] == 'U') || (seq[i] == 'U' && seq[j] == 'G')
            bonds += Bond3D(
                x1, y1, z1, x2, y2, z2,
                color = if (isGu) Color(0xFFFFC857) else Color(0xFF22E5FF),
                strokePx = if (isGu) 1.8f else 2.6f
            )
        }
    }

    return Scene3DModel(
        title = "3D RNA Fold (${analysis.wcPairs} WC + ${analysis.wobbleGuPairs} G·U)",
        spheres = emptyList(),
        bonds = bonds,
        backboneChain = listOf(backbone),
        caption = "ΔG ≈ ${"%.1f".format(analysis.estimatedDeltaGKcal)} kcal/mol • Stem-loop H-bonds shown",
        legendItems = listOf(
            LegendItem3D("Adenine (A)", Color(0xFF22E5FF), null),
            LegendItem3D("Uracil (U)", Color(0xFFFF5D73), null),
            LegendItem3D("Guanine (G)", Color(0xFF3DDC97), null),
            LegendItem3D("Cytosine (C)", Color(0xFFE04FB0), null),
            LegendItem3D("G·U Wobble Pair", Color(0xFFFFC857), null)
        )
    )
}

fun buildAminoAcid3DScene(aa: AminoAcid3DInfo): Scene3DModel {
    fun elemCol(el: String) = when (el) {
        "N" -> Color(0xFF3B82F6) // CPK Nitrogen Blue
        "O" -> Color(0xFFFF4D5A) // CPK Oxygen Red
        "S" -> Color(0xFFFFC857) // CPK Sulfur Gold
        "H" -> Color(0xFFE2E8F0) // CPK Hydrogen White
        else -> Color(0xFF94A3B8) // CPK Carbon Slate
    }

    // Core N - CA - C(=O) - OXT zwitterion backbone centered near (-0.35, -0.15, 0)
    val spheres = mutableListOf(
        Sphere3D("aa_n", "N-Terminus (NH3+)", -0.68f, -0.22f, 0.14f, 0.11f, elemCol("N"), 0.96f, "Backbone"),
        Sphere3D("aa_ca", "C-α Chiral Center", -0.30f, -0.05f, 0.0f, 0.12f, Color(0xFF22E5FF), 0.98f, "C-alpha"),
        Sphere3D("aa_c", "Carbonyl C", -0.28f, -0.45f, -0.15f, 0.105f, elemCol("C"), 0.96f, "Backbone"),
        Sphere3D("aa_o1", "Carboxyl =O", -0.52f, -0.68f, -0.22f, 0.10f, elemCol("O"), 0.96f, "Backbone"),
        Sphere3D("aa_o2", "Carboxyl -O⁻", 0.02f, -0.58f, -0.22f, 0.10f, elemCol("O"), 0.96f, "Backbone")
    )
    val bonds = mutableListOf(
        Bond3D(-0.68f, -0.22f, 0.14f, -0.30f, -0.05f, 0.0f, Color.White.copy(alpha = 0.7f), 3.0f),
        Bond3D(-0.30f, -0.05f, 0.0f, -0.28f, -0.45f, -0.15f, Color.White.copy(alpha = 0.7f), 3.0f),
        Bond3D(-0.28f, -0.45f, -0.15f, -0.52f, -0.68f, -0.22f, elemCol("O"), 2.8f),
        Bond3D(-0.28f, -0.45f, -0.15f, 0.02f, -0.58f, -0.22f, elemCol("O"), 2.8f)
    )

    val caX = -0.30f
    val caY = -0.05f
    val caZ = 0.0f
    val scPositions = mutableListOf<Triple<Float, Float, Float>>()
    aa.sideChainAtoms.forEachIndexed { idx, spec ->
        val px = (caX + spec.dx * 0.72f).coerceIn(-0.92f, 0.92f)
        val py = (caY + spec.dy * 0.72f).coerceIn(-0.92f, 0.92f)
        val pz = (caZ + spec.dz * 0.72f).coerceIn(-0.92f, 0.92f)
        scPositions += Triple(px, py, pz)
        spheres += Sphere3D(
            id = "sc_$idx",
            label = "${spec.label} (${spec.element})",
            x = px,
            y = py,
            z = pz,
            radius = if (spec.element == "S") 0.12f else if (spec.element == "H") 0.075f else 0.10f,
            color = elemCol(spec.element),
            alpha = 0.96f,
            groupTag = "R-Group"
        )
        val (ax, ay, az) = if (spec.parentIndex < 0) Triple(caX, caY, caZ) else scPositions[spec.parentIndex]
        bonds += Bond3D(ax, ay, az, px, py, pz, elemCol(spec.element).copy(alpha = 0.85f), 2.8f)
    }

    // Proline cyclizes CD back to backbone N
    if (aa.code1 == 'P' && scPositions.isNotEmpty()) {
        val (dx, dy, dz) = scPositions.last()
        bonds += Bond3D(dx, dy, dz, -0.68f, -0.22f, 0.14f, Color(0xFF94A3B8), 2.6f)
    }

    return Scene3DModel(
        title = "3D Amino Acid: ${aa.name} (${aa.code3} / ${aa.code1})",
        spheres = spheres,
        bonds = bonds,
        caption = "${aa.category} • MW ${"%.1f".format(aa.mwDa)} Da • pI ${"%.2f".format(aa.pI)} • CPK Colors",
        legendItems = listOf(
            LegendItem3D("C-α Center", Color(0xFF22E5FF), "aa_ca"),
            LegendItem3D("Nitrogen (N)", elemCol("N"), "aa_n"),
            LegendItem3D("Oxygen (O)", elemCol("O"), "aa_o1"),
            LegendItem3D("Carbon (C)", elemCol("C"), null),
            LegendItem3D("Sulfur (S)", elemCol("S"), null)
        )
    )
}

fun buildProteinDeNovo3DScene(analysis: ProteinFoldAnalysis): Scene3DModel {
    val seq = analysis.sequence.take(90)
    val n = seq.length.coerceAtLeast(1)
    val chain = mutableListOf<Sphere3D>()
    val cysIndices = mutableListOf<Int>()
    val bonds = mutableListOf<Bond3D>()
    val coords = Array(n) { Triple(0f, 0f, 0f) }

    var x = -0.6f
    var y = -0.55f
    var z = 0f
    for (i in 0 until n) {
        val st = analysis.secStruct[i]
        when (st) {
            SecStructType.HELIX -> {
                val ang = i * 1.745f // ~100 deg per residue (alpha-helix 3.6 res/turn)
                x = 0.24f * cos(ang) + (i / n.toFloat() - 0.5f) * 0.45f
                y = (i / n.toFloat() - 0.5f) * 1.45f
                z = 0.24f * sin(ang)
            }
            SecStructType.SHEET -> {
                x = (i / n.toFloat() - 0.5f) * 1.35f
                y = ((i % 2) * 0.14f - 0.07f) + sin(i * 0.3f) * 0.28f
                z = cos(i * 0.25f) * 0.26f
            }
            SecStructType.COIL -> {
                x = 0.48f * sin(i * 0.55f)
                y = (i / n.toFloat() - 0.5f) * 1.35f
                z = 0.48f * cos(i * 0.42f)
            }
        }
        val cx = x.coerceIn(-0.88f, 0.88f)
        val cy = y.coerceIn(-0.88f, 0.88f)
        val cz = z.coerceIn(-0.88f, 0.88f)
        coords[i] = Triple(cx, cy, cz)
        if (seq[i] == 'C') cysIndices += i

        val col = when {
            seq[i] == 'C' -> Color(0xFFFFC857)
            st == SecStructType.HELIX -> Color(0xFF22E5FF)
            st == SecStructType.SHEET -> Color(0xFFE04FB0)
            else -> Color(0xFF3DDC97)
        }
        chain += Sphere3D(
            id = "res_${i + 1}_${seq[i]}",
            label = "${seq[i]}${i + 1} (${st.label})",
            x = cx,
            y = cy,
            z = cz,
            radius = if (seq[i] == 'C') 0.072f else 0.052f,
            color = col,
            alpha = 0.94f,
            groupTag = st.label
        )
    }

    // Disulfide bridges between paired Cysteines
    for (k in 0 until (cysIndices.size / 2)) {
        val a = cysIndices[k * 2]
        val b = cysIndices[k * 2 + 1]
        val (x1, y1, z1) = coords[a]
        val (x2, y2, z2) = coords[b]
        bonds += Bond3D(x1, y1, z1, x2, y2, z2, Color(0xFFFFC857), 2.5f)
    }

    return Scene3DModel(
        title = "3D Protein Fold Preview (${n} aa)",
        spheres = emptyList(),
        bonds = bonds,
        backboneChain = listOf(chain),
        caption = "α-Helix ${"%.0f".format(analysis.helixPct)}% • β-Sheet ${"%.0f".format(analysis.sheetPct)}% • Disulfides: ${analysis.cysDisulfidePairs}",
        legendItems = listOf(
            LegendItem3D("α-Helix", Color(0xFF22E5FF), null),
            LegendItem3D("β-Strand", Color(0xFFE04FB0), null),
            LegendItem3D("Turn/Coil", Color(0xFF3DDC97), null),
            LegendItem3D("Cys Disulfide", Color(0xFFFFC857), null)
        )
    )
}
