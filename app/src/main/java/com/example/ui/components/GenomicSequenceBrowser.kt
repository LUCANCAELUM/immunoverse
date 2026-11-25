package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.domain.BioinformaticsEngine
import com.example.domain.DigestFragment
import com.example.domain.OrfHit
import com.example.ui.theme.LocalImmunoColors
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Computes normalized base color and complementary Watson-Crick base.
 */
object GenomicBrowserHelper {
    fun normalizeDna(raw: String): String =
        raw.uppercase().replace('U', 'T').filter { it in "ATGCN" }

    fun complementBase(base: Char): Char = when (base) {
        'A' -> 'T'
        'T' -> 'A'
        'G' -> 'C'
        'C' -> 'G'
        else -> 'N'
    }

    fun hBondCount(base: Char): Int = when (base) {
        'G', 'C' -> 3
        'A', 'T' -> 2
        else -> 0
    }

    fun findMotifPositions(sequence: String, query: String): List<Int> {
        val cleanSeq = normalizeDna(sequence)
        val cleanQuery = normalizeDna(query)
        if (cleanSeq.isEmpty() || cleanQuery.isEmpty()) return emptyList()
        val hits = mutableListOf<Int>()
        var idx = cleanSeq.indexOf(cleanQuery)
        while (idx >= 0 && hits.size < 250) {
            hits += idx
            idx = cleanSeq.indexOf(cleanQuery, idx + 1)
        }
        return hits
    }

    fun windowGcFraction(sequence: String, index: Int, halfWin: Int = 5): Double {
        if (sequence.isEmpty()) return 0.0
        val start = max(0, index - halfWin)
        val end = min(sequence.length, index + halfWin + 1)
        if (end <= start) return 0.0
        var gc = 0
        for (i in start until end) {
            if (sequence[i] == 'G' || sequence[i] == 'C') gc++
        }
        return gc.toDouble() / (end - start)
    }

    fun codonAt(sequence: String, bpIndex: Int, frameOffset: Int = 0): Pair<String, String> {
        if (bpIndex !in sequence.indices) return "---" to "-"
        val rel = bpIndex - frameOffset
        if (rel < 0) return "---" to "-"
        val codonStart = frameOffset + (rel / 3) * 3
        if (codonStart + 3 > sequence.length) return "---" to "-"
        val codon = sequence.substring(codonStart, codonStart + 3)
        val aa = BioinformaticsEngine.translate(codon)
        return codon to aa.ifEmpty { "?" }
    }
}

@Composable
fun GenomicSequenceBrowser(
    rawSequence: String,
    sequenceTitle: String = "DNA Strand Browser",
    orfs: List<OrfHit> = emptyList(),
    restrictionFragments: List<DigestFragment> = emptyList(),
    selectedEnzymeLabel: String = "EcoRI",
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val textMeasurer = rememberTextMeasurer()

    val dna = remember(rawSequence) {
        GenomicBrowserHelper.normalizeDna(rawSequence).ifEmpty {
            "ATGGCGTACGTAGCTAGCATGCGCGTATAAATCGATCGGCTAGCAATGGAATTCATCG"
        }
    }

    // pxPerBp controls zoom: 2.5f (overview) -> 48f (deep single-base resolution)
    var pxPerBp by remember(dna) { mutableFloatStateOf(28f) }
    var scrollBpOffset by remember(dna) { mutableFloatStateOf(0f) }
    var canvasWidthPx by remember { mutableFloatStateOf(900f) }
    var selectedBpIndex by remember(dna) { mutableIntStateOf(0) }

    var showComplement by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    var showAnnotations by remember { mutableStateOf(true) }
    var readingFrame by remember { mutableIntStateOf(0) } // 0, 1, 2

    var motifQuery by remember { mutableStateOf("ATG") }
    var activeHitIdx by remember { mutableIntStateOf(0) }
    val motifHits = remember(dna, motifQuery) {
        GenomicBrowserHelper.findMotifPositions(dna, motifQuery)
    }

    val visibleBpCount = remember(canvasWidthPx, pxPerBp) {
        (canvasWidthPx / pxPerBp.coerceAtLeast(2f)).coerceAtLeast(4f)
    }
    val maxOffset = (dna.length - visibleBpCount).coerceAtLeast(0f)

    fun clampOffset(rawOffset: Float): Float = rawOffset.coerceIn(0f, maxOffset.coerceAtLeast(0f))

    fun jumpToBp(bpIdx: Int, zoomToBase: Boolean = false) {
        val target = bpIdx.coerceIn(0, (dna.length - 1).coerceAtLeast(0))
        selectedBpIndex = target
        if (zoomToBase && pxPerBp < 26f) pxPerBp = 30f
        val vis = (canvasWidthPx / pxPerBp.coerceAtLeast(2f)).coerceAtLeast(4f)
        val maxOff = (dna.length - vis).coerceAtLeast(0f)
        scrollBpOffset = (target - vis * 0.35f).coerceIn(0f, maxOff)
    }

    fun baseColor(ch: Char): Color = when (ch) {
        'A' -> Color(0xFF22E5FF)
        'T' -> Color(0xFFFFC857)
        'G' -> Color(0xFF3DDC97)
        'C' -> Color(0xFFE04FB0)
        else -> Color(0xFF94A3B8)
    }

    val zoomTierLabel = when {
        pxPerBp >= 22f -> "Base + H-Bond Detail (${"%.0f".format(pxPerBp)} px/bp)"
        pxPerBp >= 10f -> "Codon Block View (${"%.0f".format(pxPerBp)} px/bp)"
        else -> "Chromosome Overview (${"%.1f".format(pxPerBp)} px/bp)"
    }

    GlassCard(
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.testTag("genomic_sequence_browser")
    ) {
        // Header Row + Zoom Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sequenceTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${dna.length} bp • $zoomTierLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.mutedText
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        pxPerBp = (pxPerBp / 1.35f).coerceIn(2.5f, 52f)
                        scrollBpOffset = clampOffset(scrollBpOffset)
                    },
                    modifier = Modifier.size(48.dp).testTag("dna_zoom_out_btn")
                ) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out DNA strand", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = {
                        pxPerBp = (pxPerBp * 1.35f).coerceIn(2.5f, 52f)
                        scrollBpOffset = clampOffset(scrollBpOffset)
                    },
                    modifier = Modifier.size(48.dp).testTag("dna_zoom_in_btn")
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in DNA strand", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = {
                        pxPerBp = 28f
                        scrollBpOffset = 0f
                        selectedBpIndex = 0
                    },
                    modifier = Modifier.size(48.dp).testTag("dna_zoom_reset_btn")
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Reset DNA zoom and scroll", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Motif Search & Next Hit Jump Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = motifQuery,
                onValueChange = {
                    motifQuery = it.uppercase().filter { ch -> ch in "ATGCN" }.take(18)
                    activeHitIdx = 0
                },
                label = { Text("Find Motif (e.g. ATG, GAATTC)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("dna_motif_search_input")
            )
            if (motifHits.isNotEmpty()) {
                IconButton(
                    onClick = {
                        activeHitIdx = (activeHitIdx - 1 + motifHits.size) % motifHits.size
                        jumpToBp(motifHits[activeHitIdx], zoomToBase = true)
                    },
                    modifier = Modifier.size(48.dp).testTag("dna_motif_prev_btn")
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous motif match")
                }
                Text(
                    "${activeHitIdx + 1}/${motifHits.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFC857)
                )
                IconButton(
                    onClick = {
                        activeHitIdx = (activeHitIdx + 1) % motifHits.size
                        jumpToBp(motifHits[activeHitIdx], zoomToBase = true)
                    },
                    modifier = Modifier.size(48.dp).testTag("dna_motif_next_btn")
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next motif match")
                }
            } else if (motifQuery.isNotEmpty()) {
                Text("0 hits", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Track Filter & Reading Frame Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = showComplement,
                onClick = { showComplement = !showComplement },
                label = { Text("3'←5' Complement") },
                modifier = Modifier.heightIn(min = 40.dp)
            )
            FilterChip(
                selected = showTranslation,
                onClick = { showTranslation = !showTranslation },
                label = { Text("Frame +${readingFrame + 1} AA") },
                modifier = Modifier.heightIn(min = 40.dp)
            )
            FilterChip(
                selected = false,
                onClick = { readingFrame = (readingFrame + 1) % 3 },
                label = { Text("Cycle Frame (+1/+2/+3)") },
                modifier = Modifier.heightIn(min = 40.dp)
            )
            FilterChip(
                selected = showAnnotations,
                onClick = { showAnnotations = !showAnnotations },
                label = { Text("ORFs & $selectedEnzymeLabel") },
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Whole-Sequence Interactive Scrubber Minimap (Drag or Tap to Scroll Anywhere)
        val minimapHeight = 36.dp
        val primaryCol = MaterialTheme.colorScheme.primary
        val surfCol = MaterialTheme.colorScheme.surfaceContainerLowest
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minimapHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(surfCol)
                .border(1.dp, semantic.glassBorder, RoundedCornerShape(8.dp))
                .pointerInput(dna, visibleBpCount) {
                    detectTapGestures { tap ->
                        val frac = (tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val targetBp = (frac * dna.length).roundToInt()
                        jumpToBp(targetBp, zoomToBase = false)
                    }
                }
                .pointerInput(dna, visibleBpCount) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val targetBp = (frac * dna.length).roundToInt()
                        scrollBpOffset = clampOffset(targetBp - visibleBpCount * 0.5f)
                    }
                }
                .testTag("dna_minimap_scrubber")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val n = dna.length.coerceAtLeast(1)
                val step = max(1, n / 90)
                var i = 0
                while (i < n) {
                    val x = (i.toFloat() / n) * w
                    val gc = GenomicBrowserHelper.windowGcFraction(dna, i, 4)
                    val barH = (h * 0.65f * gc.toFloat()).coerceAtLeast(3f)
                    drawRect(
                        color = primaryCol.copy(alpha = 0.42f),
                        topLeft = Offset(x, h - barH),
                        size = Size(max(2f, (step.toFloat() / n) * w), barH)
                    )
                    i += step
                }
                // ORF bands in minimap
                orfs.forEach { orf ->
                    val sx = ((orf.startNt - 1).coerceAtLeast(0).toFloat() / n) * w
                    val ex = (orf.endNt.coerceAtMost(n).toFloat() / n) * w
                    drawRect(
                        color = Color(0xFF3DDC97).copy(alpha = 0.65f),
                        topLeft = Offset(sx, 2f),
                        size = Size((ex - sx).coerceAtLeast(3f), 5f)
                    )
                }
                // Motif hits on minimap
                motifHits.forEach { hit ->
                    val hx = (hit.toFloat() / n) * w
                    drawRect(
                        color = Color(0xFFFFC857),
                        topLeft = Offset(hx, 8f),
                        size = Size(3f, h - 10f)
                    )
                }
                // Current Viewport Window Box
                val vStart = (scrollBpOffset / n) * w
                val vWidth = ((visibleBpCount / n) * w).coerceIn(14f, w)
                drawRect(
                    color = primaryCol.copy(alpha = 0.16f),
                    topLeft = Offset(vStart, 1f),
                    size = Size(vWidth, h - 2f)
                )
                drawRect(
                    color = primaryCol,
                    topLeft = Offset(vStart, 1f),
                    size = Size(vWidth, h - 2f),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Main Interactive Zoom + Pan DNA Strand Canvas
        val bgCol = MaterialTheme.colorScheme.background
        val mutedCol = semantic.mutedText
        val cutStarts = remember(restrictionFragments) {
            restrictionFragments.drop(1).map { it.start - 1 }.toSet()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(bgCol)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
                .onSizeChanged { sz ->
                    if (sz.width > 0) canvasWidthPx = sz.width.toFloat()
                }
                .pointerInput(dna) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val anchorBp = scrollBpOffset + centroid.x / pxPerBp.coerceAtLeast(2f)
                        val newPx = (pxPerBp * gestureZoom).coerceIn(2.5f, 52f)
                        pxPerBp = newPx
                        val nextVis = (size.width / newPx).coerceAtLeast(4f)
                        val nextMax = (dna.length - nextVis).coerceAtLeast(0f)
                        val zoomedOffset = anchorBp - centroid.x / newPx
                        scrollBpOffset = (zoomedOffset - pan.x / newPx).coerceIn(0f, nextMax)
                    }
                }
                .pointerInput(dna, pxPerBp, scrollBpOffset) {
                    detectTapGestures { tap ->
                        val bp = floor(scrollBpOffset + tap.x / pxPerBp.coerceAtLeast(2f)).toInt()
                        if (bp in dna.indices) {
                            selectedBpIndex = bp
                        }
                    }
                }
                .testTag("dna_strand_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val startIdx = floor(scrollBpOffset).toInt().coerceIn(0, (dna.length - 1).coerceAtLeast(0))
                val endIdx = min(dna.length - 1, floor(scrollBpOffset + w / pxPerBp).toInt() + 1)
                val firstX = (startIdx - scrollBpOffset) * pxPerBp

                // Annotation & ORF Track (Top 28 px)
                val rulerY = 24f
                drawLine(mutedCol.copy(alpha = 0.35f), Offset(0f, rulerY), Offset(w, rulerY), strokeWidth = 1.2f)

                if (showAnnotations) {
                    orfs.forEachIndexed { oIdx, orf ->
                        val os = (orf.startNt - 1).coerceAtLeast(0)
                        val oe = orf.endNt.coerceAtMost(dna.length)
                        if (oe >= startIdx && os <= endIdx) {
                            val sx = max(0f, (os - scrollBpOffset) * pxPerBp)
                            val ex = min(w, (oe - scrollBpOffset) * pxPerBp)
                            drawRoundRect(
                                color = Color(0xFF3DDC97).copy(alpha = 0.25f),
                                topLeft = Offset(sx, 28f),
                                size = Size((ex - sx).coerceAtLeast(4f), 14f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            if (ex - sx > 48f) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = "ORF ${orf.frame} (${orf.aaLength}aa)",
                                    topLeft = Offset(sx + 4f, 27f),
                                    style = TextStyle(color = Color(0xFF3DDC97), fontSize = TextUnit(9f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Ruler Ticks every dynamic genomic step
                val tickInterval = when {
                    pxPerBp >= 26f -> 5
                    pxPerBp >= 12f -> 10
                    pxPerBp >= 5f -> 25
                    else -> 50
                }

                // Strand Y coordinates
                val senseY = if (showComplement) h * 0.42f else h * 0.56f
                val compY = h * 0.72f
                val aaY = h * 0.87f

                // Sugar-Phosphate Backbones
                drawLine(Color(0xFF8B6BFF).copy(alpha = 0.65f), Offset(0f, senseY), Offset(w, senseY), strokeWidth = 4f)
                if (showComplement) {
                    drawLine(Color(0xFF2EE6C5).copy(alpha = 0.65f), Offset(0f, compY), Offset(w, compY), strokeWidth = 4f)
                }

                // Loop visible bases
                for (i in startIdx..endIdx) {
                    val x = (i - scrollBpOffset) * pxPerBp
                    val cx = x + pxPerBp * 0.5f
                    val b = dna[i]
                    val col = baseColor(b)
                    val isSel = i == selectedBpIndex
                    val isMotifHit = motifQuery.isNotEmpty() && motifHits.any { hit -> i in hit until (hit + motifQuery.length) }

                    // Dynamic ruler label
                    if ((i + 1) % tickInterval == 0 || i == 0) {
                        drawLine(mutedCol.copy(alpha = 0.6f), Offset(cx, 18f), Offset(cx, 25f), strokeWidth = 1.5f)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${i + 1}",
                            topLeft = Offset((cx - 8f).coerceIn(2f, w - 26f), 4f),
                            style = TextStyle(color = mutedCol, fontSize = TextUnit(9.5f, TextUnitType.Sp))
                        )
                    }

                    // Restriction Enzyme Cut Marker
                    if (showAnnotations && i in cutStarts) {
                        drawLine(Color(0xFFFF5D73), Offset(x, 26f), Offset(x, h - 8f), strokeWidth = 2.4f)
                    }

                    // Selected Base Highlight Column
                    if (isSel) {
                        drawRect(
                            color = primaryCol.copy(alpha = 0.22f),
                            topLeft = Offset(x, 26f),
                            size = Size(pxPerBp.coerceAtLeast(4f), h - 32f)
                        )
                    } else if (isMotifHit) {
                        drawRect(
                            color = Color(0xFFFFC857).copy(alpha = 0.18f),
                            topLeft = Offset(x, senseY - 18f),
                            size = Size(pxPerBp.coerceAtLeast(3f), if (showComplement) (compY - senseY + 36f) else 36f)
                        )
                    }

                    if (pxPerBp >= 10f) {
                        val boxW = (pxPerBp - 3f).coerceAtLeast(6f)
                        val boxH = if (pxPerBp >= 20f) 26f else 18f

                        // Watson-Crick H-bond rungs
                        if (showComplement) {
                            val hb = GenomicBrowserHelper.hBondCount(b)
                            val rungColor = if (hb == 3) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f)
                            if (hb == 3 && pxPerBp >= 22f) {
                                drawLine(rungColor, Offset(cx - 3f, senseY + boxH * 0.5f), Offset(cx - 3f, compY - boxH * 0.5f), 1.4f)
                                drawLine(rungColor, Offset(cx, senseY + boxH * 0.5f), Offset(cx, compY - boxH * 0.5f), 1.4f)
                                drawLine(rungColor, Offset(cx + 3f, senseY + boxH * 0.5f), Offset(cx + 3f, compY - boxH * 0.5f), 1.4f)
                            } else {
                                drawLine(rungColor, Offset(cx, senseY + boxH * 0.5f), Offset(cx, compY - boxH * 0.5f), if (hb == 3) 2.6f else 1.5f)
                            }
                        }

                        // Sense Base Block
                        drawRoundRect(
                            color = col.copy(alpha = 0.24f),
                            topLeft = Offset(cx - boxW * 0.5f, senseY - boxH * 0.5f),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                        drawRoundRect(
                            color = if (isSel) Color.White else col,
                            topLeft = Offset(cx - boxW * 0.5f, senseY - boxH * 0.5f),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(5f, 5f),
                            style = Stroke(width = if (isSel) 2.5f else 1.3f)
                        )
                        if (pxPerBp >= 18f) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = b.toString(),
                                topLeft = Offset(cx - 5f, senseY - 9f),
                                style = TextStyle(color = Color.White, fontSize = TextUnit(11.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                        }

                        // Complement Base Block
                        if (showComplement) {
                            val cb = GenomicBrowserHelper.complementBase(b)
                            val cCol = baseColor(cb)
                            drawRoundRect(
                                color = cCol.copy(alpha = 0.20f),
                                topLeft = Offset(cx - boxW * 0.5f, compY - boxH * 0.5f),
                                size = Size(boxW, boxH),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            drawRoundRect(
                                color = cCol.copy(alpha = 0.85f),
                                topLeft = Offset(cx - boxW * 0.5f, compY - boxH * 0.5f),
                                size = Size(boxW, boxH),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 1.2f)
                            )
                            if (pxPerBp >= 18f) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = cb.toString(),
                                    topLeft = Offset(cx - 5f, compY - 9f),
                                    style = TextStyle(color = Color.White, fontSize = TextUnit(11.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }

                        // Codon & Translation Track (Every 3 bases in active readingFrame)
                        if (showTranslation && pxPerBp >= 16f && (i - readingFrame) >= 0 && (i - readingFrame) % 3 == 0 && i + 2 < dna.length) {
                            val codon = dna.substring(i, i + 3)
                            val aa = BioinformaticsEngine.translate(codon).ifEmpty { "?" }
                            val isStop = aa == "*"
                            val isStart = codon == "ATG"
                            val pillColor = when {
                                isStop -> Color(0xFFFF5D73)
                                isStart -> Color(0xFF3DDC97)
                                else -> Color(0xFF8B6BFF)
                            }
                            val codonW = (pxPerBp * 3f - 4f).coerceAtLeast(18f)
                            drawRoundRect(
                                color = pillColor.copy(alpha = 0.22f),
                                topLeft = Offset(x + 2f, aaY - 9f),
                                size = Size(codonW, 20f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = pillColor.copy(alpha = 0.7f),
                                topLeft = Offset(x + 2f, aaY - 9f),
                                size = Size(codonW, 20f),
                                cornerRadius = CornerRadius(4f, 4f),
                                style = Stroke(width = 1f)
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = if (isStart) "Met*" else if (isStop) "STOP" else aa,
                                topLeft = Offset(x + codonW * 0.35f, aaY - 6f),
                                style = TextStyle(color = Color.White, fontSize = TextUnit(10f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                            )
                        }
                    } else {
                        // Compressed macro band rendering
                        drawRect(
                            color = col.copy(alpha = 0.85f),
                            topLeft = Offset(x, senseY - 12f),
                            size = Size(pxPerBp.coerceAtLeast(1.5f), 24f)
                        )
                        if (showComplement) {
                            val cb = GenomicBrowserHelper.complementBase(b)
                            drawRect(
                                color = baseColor(cb).copy(alpha = 0.65f),
                                topLeft = Offset(x, compY - 10f),
                                size = Size(pxPerBp.coerceAtLeast(1.5f), 20f)
                            )
                        }
                    }
                }

                // Strand Orientation Badges
                drawText(
                    textMeasurer = textMeasurer,
                    text = "5'→3'",
                    topLeft = Offset(4f, senseY - 26f),
                    style = TextStyle(color = Color(0xFF8B6BFF), fontSize = TextUnit(9.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                )
                if (showComplement) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "3'←5'",
                        topLeft = Offset(4f, compY + 14f),
                        style = TextStyle(color = Color(0xFF2EE6C5), fontSize = TextUnit(9.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Selected Base & Codon Inspector Bar
        val safeIdx = selectedBpIndex.coerceIn(0, (dna.length - 1).coerceAtLeast(0))
        val senseBase = dna[safeIdx]
        val compBase = GenomicBrowserHelper.complementBase(senseBase)
        val hbCount = GenomicBrowserHelper.hBondCount(senseBase)
        val (codon, aa) = GenomicBrowserHelper.codonAt(dna, safeIdx, readingFrame)
        val localGc = GenomicBrowserHelper.windowGcFraction(dna, safeIdx, 10) * 100.0
        val winStart = floor(scrollBpOffset).toInt() + 1
        val winEnd = min(dna.length, floor(scrollBpOffset + visibleBpCount).toInt() + 1)

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, semantic.glassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Base ${safeIdx + 1}: 5'-$senseBase ≡ $compBase-3' ($hbCount H-bonds) • Viewing bp $winStart–$winEnd",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Frame +${readingFrame + 1} Codon: $codon → $aa • Local GC (±10 bp): ${"%.1f".format(localGc)}% • Pinch to zoom, drag to scroll, tap any base",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.mutedText
                )
            }
        }
    }
}
