package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.kb.PathwayInfo
import com.example.domain.OlsFit
import com.example.domain.TreeNode
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay
import kotlin.math.abs

data class ChartSeries(
    val label: String,
    val color: Color,
    val points: List<Pair<Double, Double>>
)

@Composable
fun LineChartCanvas(
    series: List<ChartSeries>,
    title: String = "",
    markerX: Double? = null,
    height: Dp = 190.dp,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val textMeasurer = rememberTextMeasurer()
    val axisColor = semantic.mutedText

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title.isNotBlank()) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val allPts = series.flatMap { it.points }
            if (allPts.isEmpty()) return@Canvas
            val minX = allPts.minOf { it.first }
            val maxX = (allPts.maxOf { it.first }).coerceAtLeast(minX + 1e-6)
            val maxY = (allPts.maxOf { it.second }).coerceAtLeast(0.1)
            val padL = 42f
            val padB = 24f
            val plotW = size.width - padL - 12f
            val plotH = size.height - padB - 12f

            for (g in 0..4) {
                val y = 8f + plotH * g / 4f
                drawLine(semantic.glassBorder, Offset(padL, y), Offset(padL + plotW, y), strokeWidth = 1f)
                val valY = maxY * (1.0 - g / 4.0)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "%.1f".format(valY),
                    topLeft = Offset(2f, (y - 10f).coerceAtLeast(0f)),
                    style = androidx.compose.ui.text.TextStyle(color = axisColor, fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
            }

            if (markerX != null && markerX in minX..maxX) {
                val mx = padL + ((markerX - minX) / (maxX - minX)).toFloat() * plotW
                drawLine(semantic.illustrative, Offset(mx, 8f), Offset(mx, 8f + plotH), strokeWidth = 2.5f)
            }

            series.forEach { s ->
                if (s.points.size < 2) return@forEach
                val path = Path()
                s.points.forEachIndexed { idx, (xv, yv) ->
                    val sx = padL + ((xv - minX) / (maxX - minX)).toFloat() * plotW
                    val sy = 8f + plotH - (yv / maxY).toFloat().coerceIn(0f, 1f) * plotH
                    if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                }
                drawPath(path, s.color, style = Stroke(width = 3.5f))
            }

            drawText(
                textMeasurer = textMeasurer,
                text = "%.0f".format(minX),
                topLeft = Offset(padL, 8f + plotH + 4f),
                style = androidx.compose.ui.text.TextStyle(color = axisColor, fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp))
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "%.0f".format(maxX),
                topLeft = Offset(padL + plotW - 24f, 8f + plotH + 4f),
                style = androidx.compose.ui.text.TextStyle(color = axisColor, fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Canvas(Modifier.height(8.dp).padding(end = 2.dp)) {
                        drawCircle(s.color, radius = 5f, center = Offset(4f, 4f))
                    }
                    Text("  ${s.label}", style = MaterialTheme.typography.labelSmall, color = s.color)
                }
            }
        }
    }
}

@Composable
fun ScatterOlsCanvas(
    points: List<Pair<Double, Double>>,
    fit: OlsFit?,
    xLabel: String,
    yLabel: String,
    height: Dp = 190.dp
) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("$yLabel vs $xLabel" + (fit?.let { " • R² = ${"%.3f".format(it.rSquared)}" } ?: ""), style = MaterialTheme.typography.titleSmall, color = primary)
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (points.isEmpty()) return@Canvas
            val minX = points.minOf { it.first }
            val maxX = points.maxOf { it.first }.coerceAtLeast(minX + 1e-6)
            val minY = points.minOf { it.second }
            val maxY = points.maxOf { it.second }.coerceAtLeast(minY + 1e-6)
            val pad = 20f
            val w = size.width - pad * 2
            val h = size.height - pad * 2

            fun mapPt(px: Double, py: Double): Offset = Offset(
                pad + ((px - minX) / (maxX - minX)).toFloat() * w,
                pad + h - ((py - minY) / (maxY - minY)).toFloat() * h
            )

            if (fit != null) {
                val p1 = mapPt(minX, fit.slope * minX + fit.intercept)
                val p2 = mapPt(maxX, fit.slope * maxX + fit.intercept)
                drawLine(accent, p1, p2, strokeWidth = 3f)
            }
            points.forEach { (xv, yv) ->
                drawCircle(primary, radius = 6.5f, center = mapPt(xv, yv))
            }
        }
    }
}

@Composable
fun BarChartCanvas(
    labels: List<String>,
    values: List<Double>,
    title: String,
    height: Dp = 180.dp
) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = primary)
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val maxVal = (values.maxOf { abs(it) }).coerceAtLeast(0.1)
            val padB = 26f
            val plotH = size.height - padB - 12f
            val barW = (size.width / values.size.coerceAtLeast(1)) * 0.62f
            val gap = size.width / values.size.coerceAtLeast(1)

            values.forEachIndexed { idx, v ->
                val bh = (abs(v) / maxVal).toFloat() * plotH
                val bx = idx * gap + (gap - barW) / 2f
                val by = 8f + plotH - bh
                drawRect(
                    color = primary.copy(alpha = 0.82f),
                    topLeft = Offset(bx, by),
                    size = Size(barW, bh)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = labels.getOrElse(idx) { "${idx + 1}" }.take(6),
                    topLeft = Offset(bx, 10f + plotH),
                    style = androidx.compose.ui.text.TextStyle(color = semantic.mutedText, fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
            }
        }
    }
}

@Composable
fun HeatmapCanvas(
    rowLabels: List<String>,
    colLabels: List<String>,
    matrix: List<List<Double>>,
    title: String,
    height: Dp = 210.dp
) {
    val semantic = LocalImmunoColors.current
    val textMeasurer = rememberTextMeasurer()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (matrix.isEmpty() || matrix.first().isEmpty()) return@Canvas
            val rows = matrix.size
            val cols = matrix.first().size
            val padL = 68f
            val padB = 24f
            val cw = (size.width - padL) / cols
            val ch = (size.height - padB) / rows
            val maxAbs = (matrix.flatten().maxOfOrNull { abs(it) } ?: 1.0).coerceAtLeast(0.5)

            for (r in 0 until rows) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = rowLabels.getOrElse(r) { "R$r" }.take(8),
                    topLeft = Offset(2f, r * ch + ch * 0.25f),
                    style = androidx.compose.ui.text.TextStyle(color = semantic.mutedText, fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
                for (c in 0 until cols) {
                    val v = matrix[r][c]
                    val norm = (v / maxAbs).toFloat().coerceIn(-1f, 1f)
                    val cellColor = if (norm >= 0f) {
                        Color(0.08f, 0.72f * norm + 0.15f, 0.92f * norm + 0.2f, 0.9f)
                    } else {
                        Color(0.90f * -norm + 0.18f, 0.20f, 0.50f * -norm + 0.2f, 0.9f)
                    }
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(padL + c * cw + 1f, r * ch + 1f),
                        size = Size((cw - 2f).coerceAtLeast(1f), (ch - 2f).coerceAtLeast(1f))
                    )
                }
            }
            for (c in 0 until cols) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = colLabels.getOrElse(c) { "C$c" }.take(7),
                    topLeft = Offset(padL + c * cw + 2f, rows * ch + 4f),
                    style = androidx.compose.ui.text.TextStyle(color = semantic.mutedText, fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
            }
        }
    }
}

@Composable
fun FlowCytometryCanvas(points: List<Pair<Float, Float>>, height: Dp = 200.dp) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp)
    ) {
        drawLine(semantic.glassBorder, Offset(size.width * 0.48f, 0f), Offset(size.width * 0.48f, size.height), 2f)
        drawLine(semantic.glassBorder, Offset(0f, size.height * 0.52f), Offset(size.width, size.height * 0.52f), 2f)
        points.forEach { (x, y) ->
            drawCircle(
                color = if (x > 0.48f && y < 0.52f) primary else if (x < 0.48f && y > 0.52f) semantic.hypothesis else semantic.mutedText,
                radius = 4f,
                center = Offset(x * size.width, (1f - y) * size.height)
            )
        }
        drawText(textMeasurer, "CD8+", Offset(8f, 8f), androidx.compose.ui.text.TextStyle(color = semantic.hypothesis, fontSize = androidx.compose.ui.unit.TextUnit(10.5f, androidx.compose.ui.unit.TextUnitType.Sp)))
        drawText(textMeasurer, "CD4+", Offset(size.width - 42f, size.height - 20f), androidx.compose.ui.text.TextStyle(color = primary, fontSize = androidx.compose.ui.unit.TextUnit(10.5f, androidx.compose.ui.unit.TextUnitType.Sp)))
    }
}

@Composable
fun GelElectrophoresisCanvas(lanes: List<List<Int>>, height: Dp = 180.dp) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp)
    ) {
        val laneW = size.width / lanes.size.coerceAtLeast(1)
        lanes.forEachIndexed { idx, bands ->
            val xCenter = idx * laneW + laneW * 0.5f
            drawRect(semantic.mutedText.copy(alpha = 0.35f), Offset(xCenter - 16f, 6f), Size(32f, 10f))
            bands.forEach { bp ->
                val normY = (1.0 - (kotlin.math.ln(bp.toDouble().coerceAtLeast(50.0)) - kotlin.math.ln(50.0)) / (kotlin.math.ln(6000.0) - kotlin.math.ln(50.0))).toFloat().coerceIn(0.12f, 0.92f)
                val y = normY * size.height
                drawRect(primary, Offset(xCenter - 18f, y), Size(36f, 5f))
                if (idx == 0) {
                    drawText(textMeasurer, "$bp", Offset(xCenter + 22f, y - 6f), androidx.compose.ui.text.TextStyle(color = semantic.mutedText, fontSize = androidx.compose.ui.unit.TextUnit(8.5f, androidx.compose.ui.unit.TextUnitType.Sp)))
                }
            }
        }
    }
}

@Composable
fun PhylogenyTreeCanvas(root: TreeNode?, height: Dp = 180.dp) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(10.dp)
    ) {
        if (root == null) return@Canvas
        val leaves = mutableListOf<TreeNode>()
        fun collect(n: TreeNode) {
            if (n.left == null && n.right == null) leaves += n
            else {
                n.left?.let(::collect)
                n.right?.let(::collect)
            }
        }
        collect(root)
        val leafY = leaves.mapIndexed { idx, node ->
            node to (20f + idx * ((size.height - 40f) / (leaves.size - 1).coerceAtLeast(1)))
        }.toMap()
        val maxH = root.height.coerceAtLeast(0.05)
        val treeW = size.width - 130f

        fun drawSub(n: TreeNode): Float {
            val nx = 16f + ((maxH - n.height) / maxH).toFloat() * treeW
            if (n.left == null && n.right == null) {
                val ny = leafY[n] ?: 20f
                drawCircle(primary, 5f, Offset(nx, ny))
                drawText(
                    textMeasurer = textMeasurer,
                    text = n.label.take(18),
                    topLeft = Offset(nx + 10f, ny - 8f),
                    style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp))
                )
                return ny
            }
            val ly = drawSub(n.left!!)
            val ry = drawSub(n.right!!)
            val lx = 16f + ((maxH - n.left.height) / maxH).toFloat() * treeW
            val rx = 16f + ((maxH - n.right.height) / maxH).toFloat() * treeW
            drawLine(primary, Offset(nx, ly), Offset(nx, ry), 2.5f)
            drawLine(primary, Offset(nx, ly), Offset(lx, ly), 2.5f)
            drawLine(primary, Offset(nx, ry), Offset(rx, ry), 2.5f)
            return (ly + ry) / 2f
        }
        drawSub(root)
    }
}

@Composable
fun PathwayGraphCanvas(
    pathway: PathwayInfo,
    inhibitedNodeIds: Set<String>,
    reduceMotion: Boolean,
    onToggleNode: (String) -> Unit,
    height: Dp = 220.dp
) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    var pulse by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            while (true) {
                pulse = (pulse + 0.03f) % 1f
                delay(32L)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
            .padding(12.dp)
            .pointerInput(pathway) {
                detectTapGestures { tap ->
                    val hit = pathway.nodes.minByOrNull { n ->
                        val dx = n.x * size.width - tap.x
                        val dy = n.y * size.height - tap.y
                        dx * dx + dy * dy
                    }
                    if (hit != null) {
                        val dx = hit.x * size.width - tap.x
                        val dy = hit.y * size.height - tap.y
                        if (kotlin.math.sqrt(dx * dx + dy * dy) < 64f) {
                            onToggleNode(hit.id)
                        }
                    }
                }
            }
    ) {
        val pos = pathway.nodes.associate { it.id to Offset(it.x * size.width, it.y * size.height) }
        pathway.edges.forEach { e ->
            val p1 = pos[e.from] ?: return@forEach
            val p2 = pos[e.to] ?: return@forEach
            val blocked = e.from in inhibitedNodeIds || e.to in inhibitedNodeIds
            val col = when {
                blocked -> semantic.mutedText.copy(alpha = 0.35f)
                e.action == "inhibits" -> semantic.critical
                else -> primary
            }
            drawLine(col, p1, p2, strokeWidth = 3f)
            if (!blocked) {
                val px = p1.x + (p2.x - p1.x) * pulse
                val py = p1.y + (p2.y - p1.y) * pulse
                drawCircle(col, radius = 5f, center = Offset(px, py))
            }
        }
        pathway.nodes.forEach { n ->
            val center = pos[n.id] ?: return@forEach
            val blocked = n.id in inhibitedNodeIds
            val col = if (blocked) semantic.critical else primary
            drawCircle(col.copy(alpha = 0.25f), radius = 22f, center = center)
            drawCircle(col, radius = 22f, center = center, style = Stroke(width = 2.5f))
            drawText(
                textMeasurer = textMeasurer,
                text = n.label.take(16),
                topLeft = Offset((center.x - 36f).coerceIn(2f, size.width - 90f), (center.y + 24f).coerceAtMost(size.height - 16f)),
                style = androidx.compose.ui.text.TextStyle(color = if (blocked) semantic.critical else Color.White, fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp))
            )
        }
    }
}

@Composable
fun MicroscopyCanvas(
    stainMode: String, // "H&E", "Gram", "Immunofluorescence"
    magnification: Float,
    height: Dp = 180.dp
) {
    val semantic = LocalImmunoColors.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(if (stainMode == "Immunofluorescence") Color(0xFF030710) else Color(0xFFEBF0FA))
            .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
    ) {
        val scale = (magnification / 40f).coerceIn(0.4f, 2.5f)
        for (i in 0 until 14) {
            val cx = ((i * 83 + 47) % 100) / 100f * size.width
            val cy = ((i * 59 + 29) % 100) / 100f * size.height
            when (stainMode) {
                "Immunofluorescence" -> {
                    drawCircle(Color(0xFF22E5FF).copy(alpha = 0.35f), 20f * scale, Offset(cx, cy))
                    drawCircle(Color(0xFF3DDC97), 9f * scale, Offset(cx + 3f, cy - 2f))
                }
                "Gram" -> {
                    val isPos = i % 2 == 0
                    drawCircle(
                        color = if (isPos) Color(0xFF4B2E83) else Color(0xFFE05286),
                        radius = (if (isPos) 6f else 8f) * scale,
                        center = Offset(cx, cy)
                    )
                }
                else -> {
                    drawCircle(Color(0xFFF3B8D4), 18f * scale, Offset(cx, cy))
                    drawCircle(Color(0xFF56328C), 8f * scale, Offset(cx, cy))
                }
            }
        }
    }
}
