package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.data.network.PdbStructureData
import com.example.domain.DifferentialProteinRow
import com.example.domain.DigestedPeptide
import com.example.domain.FragmentIon
import com.example.ui.theme.LocalImmunoColors
import kotlin.math.abs
import kotlin.math.log10

@Composable
fun VerifiedPdbMetadataCard(
    data: PdbStructureData,
    pickedResidue: String?,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val helixCount = remember(data) { data.caAtoms.count { it.secStruct == 'H' } }
    val sheetCount = remember(data) { data.caAtoms.count { it.secStruct == 'E' } }
    val loopCount = remember(data) { (data.caAtoms.size - helixCount - sheetCount).coerceAtLeast(0) }

    GlassCard(modifier = modifier, accentColor = if (data.isAlphaFoldModel) MaterialTheme.colorScheme.tertiary else semantic.established) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (data.isAlphaFoldModel) "AlphaFold Model: ${data.pdbId}" else "Experimental PDB: ${data.pdbId}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            EvidencePill(if (data.isAlphaFoldModel) "AlphaFold2 DB" else "Verified Empirical")
        }
        Spacer(Modifier.height(4.dp))
        Text(data.title, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Method: ${data.method} • Resolution/Score: ${data.resolution}" +
                (data.rFree?.let { " • R-free: $it" } ?: ""),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Chains: ${data.chains.joinToString()} • Parsed C-α: ${data.caAtoms.size} (${data.totalAtomsParsed} raw) • S-S Bonds: ${data.disulfides.size} • Bound Ligands: ${data.heteroAtoms.size}",
            style = MaterialTheme.typography.bodySmall,
            color = semantic.mutedText
        )
        if (!data.isAlphaFoldModel && data.caAtoms.isNotEmpty()) {
            val hPct = 100.0 * helixCount / data.caAtoms.size
            val ePct = 100.0 * sheetCount / data.caAtoms.size
            val cPct = 100.0 * loopCount / data.caAtoms.size
            Text(
                text = "Experimental PDB Sec. Structure: α-Helix ${"%.0f".format(hPct)}% • β-Sheet ${"%.0f".format(ePct)}% • Loop/Turn ${"%.0f".format(cPct)}%",
                style = MaterialTheme.typography.labelSmall,
                color = semantic.established
            )
        }
        if (data.meanPlddt != null) {
            Text(
                text = "Mean AlphaFold pLDDT Confidence: ${"%.1f".format(data.meanPlddt)} / 100",
                style = MaterialTheme.typography.labelSmall,
                color = semantic.established
            )
        }
        if (!data.citationAuthors.isNullOrBlank() || !data.doi.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Citation: ${data.citationAuthors ?: ""} ${data.citationJournal ?: ""} ${data.doi?.let { "• DOI: $it" } ?: ""}".trim(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (!pickedResidue.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Selected 3D Residue / Ligand: $pickedResidue",
                style = MaterialTheme.typography.labelLarge,
                color = semantic.established
            )
        }
    }
}

@Composable
fun TitrationCurveCanvas(
    curve: List<Pair<Double, Double>>,
    theoreticalPi: Double,
    height: Dp = 175.dp,
    modifier: Modifier = Modifier
) {
    if (curve.isEmpty()) return
    val semantic = LocalImmunoColors.current
    val tm = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest

    GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("pH vs Net Charge Titration Curve", style = MaterialTheme.typography.titleSmall, color = primary)
            EvidencePill("pI = ${"%.2f".format(theoreticalPi)}")
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(bg)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.small)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                val minQ = (curve.minOf { it.second } - 1.0).coerceAtMost(-2.0)
                val maxQ = (curve.maxOf { it.second } + 1.0).coerceAtLeast(2.0)
                val spanQ = (maxQ - minQ).coerceAtLeast(1.0)
                val zeroY = h - ((0.0 - minQ) / spanQ).toFloat() * h

                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                val piX = ((theoreticalPi / 14.0).toFloat() * w).coerceIn(0f, w)
                drawLine(
                    color = Color(0xFF3DDC97).copy(alpha = 0.7f),
                    start = Offset(piX, 0f),
                    end = Offset(piX, h),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                val path = Path()
                curve.forEachIndexed { idx, (ph, q) ->
                    val px = (ph / 14.0).toFloat() * w
                    val py = h - ((q - minQ) / spanQ).toFloat() * h
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, primary, style = Stroke(width = 3.2f))
                drawCircle(Color(0xFF3DDC97), radius = 6f, center = Offset(piX, zeroY))

                val label = tm.measure("pI ${"%.2f".format(theoreticalPi)}", TextStyle(color = Color(0xFF3DDC97), fontSize = TextUnit(10f, TextUnitType.Sp), fontWeight = FontWeight.Bold))
                drawText(label, topLeft = Offset((piX + 6f).coerceAtMost(w - label.size.width), (zeroY - 18f).coerceAtLeast(2f)))
            }
        }
    }
}

@Composable
fun Ms1FingerprintCanvas(
    peptides: List<DigestedPeptide>,
    selectedSequence: String?,
    onSelectPeptide: (DigestedPeptide) -> Unit,
    height: Dp = 185.dp,
    modifier: Modifier = Modifier
) {
    if (peptides.isEmpty()) return
    val semantic = LocalImmunoColors.current
    val tm = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest
    var projected by remember { mutableStateOf<List<Pair<DigestedPeptide, Float>>>(emptyList()) }

    GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MS1 Peptide Mass Fingerprint (PMF)", style = MaterialTheme.typography.titleSmall, color = primary)
            Text("Tap peak to fragment", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(bg)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.small)
                .pointerInput(peptides) {
                    detectTapGestures { tap ->
                        val hit = projected.minByOrNull { (_, px) -> abs(px - tap.x) }
                        if (hit != null && abs(hit.second - tap.x) < 42f) {
                            onSelectPeptide(hit.first)
                        }
                    }
                }
                .testTag("ms1_pmf_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 18.dp)) {
                val w = size.width
                val h = size.height
                val minMz = (peptides.minOf { it.mzPlus1 } - 80.0).coerceAtLeast(200.0)
                val maxMz = (peptides.maxOf { it.mzPlus1 } + 100.0).coerceAtLeast(minMz + 200.0)
                val span = maxMz - minMz

                drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, h), Offset(w, h), 1.8f)

                val cache = mutableListOf<Pair<DigestedPeptide, Float>>()
                peptides.take(45).forEach { pep ->
                    val px = (((pep.mzPlus1 - minMz) / span).toFloat() * w).coerceIn(4f, w - 4f)
                    val topY = h - (pep.estimatedIntensity.toFloat() * h * 0.88f)
                    val isSel = pep.sequence == selectedSequence
                    val col = if (isSel) Color(0xFFFFC857) else if (pep.missedCleavages == 0) primary else Color(0xFF8B6BFF)
                    drawLine(col, Offset(px, h), Offset(px, topY), strokeWidth = if (isSel) 4.2f else 2.6f)
                    cache += pep to px
                    if (isSel || pep.estimatedIntensity >= 0.78) {
                        val txt = tm.measure(
                            "${"%.1f".format(pep.mzPlus1)}",
                            TextStyle(color = col, fontSize = TextUnit(8.8f, TextUnitType.Sp), fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        )
                        drawText(txt, topLeft = Offset((px - txt.size.width / 2f).coerceIn(0f, (w - txt.size.width).coerceAtLeast(0f)), (topY - 14f).coerceAtLeast(0f)))
                    }
                }
                projected = cache
            }
        }
    }
}

@Composable
fun MsMsFragmentSpectrumCanvas(
    ions: List<FragmentIon>,
    matchedIons: Set<String>,
    peptide: String,
    height: Dp = 195.dp,
    modifier: Modifier = Modifier
) {
    if (ions.isEmpty()) return
    val semantic = LocalImmunoColors.current
    val tm = rememberTextMeasurer()
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest

    GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Annotated MS/MS Spectrum ($peptide)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text("b-ions Cyan • y-ions Emerald", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(bg)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.small)
                .testTag("msms_spectrum_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 14.dp)) {
                val w = size.width
                val h = size.height
                val minMz = (ions.minOf { it.mz } - 40.0).coerceAtLeast(50.0)
                val maxMz = (ions.maxOf { it.mz } + 50.0).coerceAtLeast(minMz + 100.0)
                val span = maxMz - minMz

                drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, h), Offset(w, h), 1.8f)
                val singleIons = ions.filter { it.charge == 1 }
                singleIons.forEach { ion ->
                    val key = "${ion.series}${ion.number}+${ion.charge}"
                    val matched = key in matchedIons
                    val px = (((ion.mz - minMz) / span).toFloat() * w).coerceIn(4f, w - 4f)
                    val relH = if (matched) 0.92f else if (ion.series == "y") 0.74f else 0.56f
                    val topY = h - h * relH
                    val col = when {
                        matched -> Color(0xFFFFC857)
                        ion.series == "y" -> Color(0xFF3DDC97)
                        else -> Color(0xFF22E5FF)
                    }
                    drawLine(col, Offset(px, h), Offset(px, topY), strokeWidth = if (matched) 3.8f else 2.2f)
                    if (matched || ion.number % 2 == 1 || singleIons.size <= 14) {
                        val lbl = tm.measure(
                            "${ion.series}${ion.number}",
                            TextStyle(color = col, fontSize = TextUnit(8.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                        )
                        drawText(lbl, topLeft = Offset((px - lbl.size.width / 2f).coerceIn(0f, (w - lbl.size.width).coerceAtLeast(0f)), (topY - 13f).coerceAtLeast(0f)))
                    }
                }
            }
        }
    }
}

@Composable
fun VolcanoPlotCanvas(
    rows: List<DifferentialProteinRow>,
    fcCutoff: Double,
    pCutoff: Double,
    selectedProtein: String?,
    onSelect: (DifferentialProteinRow) -> Unit,
    height: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) return
    val semantic = LocalImmunoColors.current
    val tm = rememberTextMeasurer()
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest
    var dotPositions by remember { mutableStateOf<List<Pair<DifferentialProteinRow, Offset>>>(emptyList()) }

    GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Differential Proteomics Volcano Plot", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text("Tap protein dot", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(bg)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.small)
                .pointerInput(rows) {
                    detectTapGestures { tap ->
                        val hit = dotPositions.minByOrNull { (_, pt) ->
                            val dx = pt.x - tap.x
                            val dy = pt.y - tap.y
                            dx * dx + dy * dy
                        }
                        if (hit != null) {
                            val dx = hit.second.x - tap.x
                            val dy = hit.second.y - tap.y
                            if (dx * dx + dy * dy < 48f * 48f) {
                                onSelect(hit.first)
                            }
                        }
                    }
                }
                .testTag("volcano_plot_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val w = size.width
                val h = size.height
                val maxAbsFc = (rows.maxOf { abs(it.log2Fc) } + 0.6).coerceAtLeast(2.5)
                val maxNegP = (rows.maxOf { it.negLog10P } + 0.6).coerceAtLeast(3.0)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))

                fun toX(fc: Double): Float = (((fc + maxAbsFc) / (2.0 * maxAbsFc)) * w).toFloat().coerceIn(0f, w)
                fun toY(nlp: Double): Float = (h - (nlp / maxNegP) * h).toFloat().coerceIn(0f, h)

                val pLineY = toY(-log10(pCutoff.coerceIn(1e-12, 1.0)))
                val fcPlusX = toX(fcCutoff)
                val fcMinusX = toX(-fcCutoff)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, pLineY), Offset(w, pLineY), 1.5f, pathEffect = dash)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(fcPlusX, 0f), Offset(fcPlusX, h), 1.5f, pathEffect = dash)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(fcMinusX, 0f), Offset(fcMinusX, h), 1.5f, pathEffect = dash)

                val pts = mutableListOf<Pair<DifferentialProteinRow, Offset>>()
                rows.forEach { r ->
                    val pt = Offset(toX(r.log2Fc), toY(r.negLog10P))
                    pts += r to pt
                    val col = when (r.regulation) {
                        "Up" -> Color(0xFFFF5D73)
                        "Down" -> Color(0xFF22E5FF)
                        else -> Color(0xFF64748B)
                    }
                    val isSel = r.proteinName == selectedProtein
                    drawCircle(col, radius = if (isSel) 9f else 5.5f, center = pt)
                    if (isSel) {
                        drawCircle(Color.White, radius = 12f, center = pt, style = Stroke(2.2f))
                    }
                    if (isSel || r.regulation != "NS") {
                        val lbl = tm.measure(
                            r.proteinName,
                            TextStyle(color = if (isSel) Color.White else col, fontSize = TextUnit(8.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                        )
                        drawText(lbl, topLeft = Offset((pt.x + 7f).coerceAtMost(w - lbl.size.width), (pt.y - 10f).coerceAtLeast(0f)))
                    }
                }
                dotPositions = pts
            }
        }
    }
}
