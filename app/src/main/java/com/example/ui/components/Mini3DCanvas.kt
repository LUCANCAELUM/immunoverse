package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Sphere3D(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float = 0.92f,
    val groupTag: String = ""
)

data class Bond3D(
    val x1: Float,
    val y1: Float,
    val z1: Float,
    val x2: Float,
    val y2: Float,
    val z2: Float,
    val color: Color,
    val strokePx: Float = 3.5f
)

data class LegendItem3D(
    val label: String,
    val color: Color,
    val targetId: String? = null
)

data class Scene3DModel(
    val spheres: List<Sphere3D> = emptyList(),
    val bonds: List<Bond3D> = emptyList(),
    val backboneChain: List<List<Sphere3D>> = emptyList(),
    val caption: String = "",
    val title: String = "",
    val legendItems: List<LegendItem3D> = emptyList()
)

private data class ProjectedPoint(
    val sx: Float,
    val sy: Float,
    val depth: Float,
    val scale: Float,
    val zRot: Float
)

private fun project3D(
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    w: Float,
    h: Float
): ProjectedPoint {
    val cy = cos(yaw)
    val sy = sin(yaw)
    val cp = cos(pitch)
    val sp = sin(pitch)

    val rx = x * cy + z * sy
    val rz0 = -x * sy + z * cy
    val ry = y * cp - rz0 * sp
    val rz = y * sp + rz0 * cp

    val camDist = 3.2f
    val denom = (camDist - rz).coerceAtLeast(0.45f)
    val factor = (minOf(w, h) * 0.42f * zoom) * (camDist / denom) * 0.55f
    return ProjectedPoint(
        sx = w * 0.5f + rx * factor,
        sy = h * 0.5f - ry * factor,
        depth = rz,
        scale = (camDist / denom) * zoom,
        zRot = rz
    )
}

@Composable
fun OrbitalLogo3D(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    var phase by remember { mutableFloatStateOf(0f) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            while (true) {
                phase += 0.045f
                delay(32L)
            }
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = minOf(cx, cy)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = 0.45f), secondary.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 0.95f
            ),
            radius = r * 0.95f
        )

        for (ringIdx in 0..1) {
            val tilt = if (ringIdx == 0) 0.42f else -0.42f
            val path = Path()
            val steps = 42
            for (i in 0..steps) {
                val a = (i.toFloat() / steps) * (Math.PI * 2.0).toFloat() + phase * (if (ringIdx == 0) 1f else -1f)
                val ox = cos(a) * r * 0.78f
                val oy = sin(a) * r * 0.28f
                val rx = ox * cos(tilt) - oy * sin(tilt)
                val ry = ox * sin(tilt) + oy * cos(tilt)
                if (i == 0) path.moveTo(cx + rx, cy + ry) else path.lineTo(cx + rx, cy + ry)
            }
            drawPath(
                path = path,
                color = (if (ringIdx == 0) primary else secondary).copy(alpha = 0.75f),
                style = Stroke(width = 2.4f)
            )
        }

        for (k in 0..5) {
            val a = phase * 1.3f + k * 1.05f
            val px = cx + cos(a) * r * 0.65f
            val py = cy + sin(a * 1.5f) * r * 0.42f
            drawCircle(
                color = if (k % 2 == 0) tertiary else primary,
                radius = r * 0.07f,
                center = Offset(px, py)
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, primary, secondary.copy(alpha = 0.85f)),
                center = Offset(cx - r * 0.1f, cy - r * 0.1f),
                radius = r * 0.38f
            ),
            radius = r * 0.34f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun Interactive3DScene(
    scene: Scene3DModel,
    reduceMotion: Boolean,
    selectedId: String? = null,
    onSelectObject: ((String) -> Unit)? = null,
    height: Dp = 270.dp,
    showSliceControl: Boolean = true,
    allowFullscreen: Boolean = true,
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(0.45f) }
    var pitch by remember { mutableFloatStateOf(0.25f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var sliceZ by remember { mutableFloatStateOf(1.1f) }
    var autoSpin by remember(reduceMotion) { mutableStateOf(!reduceMotion) }
    var showLabels by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(autoSpin, reduceMotion) {
        if (autoSpin && !reduceMotion) {
            while (true) {
                yaw += 0.014f
                delay(32L)
            }
        }
    }

    @Composable
    fun SceneBody(boxModifier: Modifier) {
        val semantic = LocalImmunoColors.current
        val bg = MaterialTheme.colorScheme.background
        val surf = MaterialTheme.colorScheme.surface
        val primary = MaterialTheme.colorScheme.primary
        val textMeasurer = rememberTextMeasurer()

        var projectedCache by remember { mutableStateOf<List<Pair<Sphere3D, ProjectedPoint>>>(emptyList()) }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = boxModifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brush.radialGradient(listOf(surf, bg)))
                    .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            yaw += drag.x * 0.009f
                            pitch = (pitch + drag.y * 0.009f).coerceIn(-1.35f, 1.35f)
                        }
                    }
                    .pointerInput(scene, onSelectObject) {
                        if (onSelectObject != null) {
                            detectTapGestures { tap ->
                                val hit = projectedCache.minByOrNull { (_, p) ->
                                    val dx = p.sx - tap.x
                                    val dy = p.sy - tap.y
                                    dx * dx + dy * dy
                                }
                                if (hit != null) {
                                    val dx = hit.second.sx - tap.x
                                    val dy = hit.second.sy - tap.y
                                    if (sqrt(dx * dx + dy * dy) < 76f) {
                                        onSelectObject(hit.first.id)
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Bonds / connections
                    scene.bonds.forEach { b ->
                        val p1 = project3D(b.x1, b.y1, b.z1, yaw, pitch, zoom, w, h)
                        val p2 = project3D(b.x2, b.y2, b.z2, yaw, pitch, zoom, w, h)
                        if (p1.zRot <= sliceZ && p2.zRot <= sliceZ) {
                            drawLine(
                                color = b.color.copy(alpha = 0.72f),
                                start = Offset(p1.sx, p1.sy),
                                end = Offset(p2.sx, p2.sy),
                                strokeWidth = b.strokePx
                            )
                        }
                    }

                    // Protein / Polymer C-alpha backbone chains
                    val chainTerminalLabels = mutableListOf<Pair<Sphere3D, ProjectedPoint>>()
                    val chainPickables = mutableListOf<Pair<Sphere3D, ProjectedPoint>>()
                    scene.backboneChain.forEachIndexed { cIdx, chain ->
                        if (chain.size > 1) {
                            for (idx in 0 until chain.lastIndex) {
                                val a = chain[idx]
                                val b = chain[idx + 1]
                                val pa = project3D(a.x, a.y, a.z, yaw, pitch, zoom, w, h)
                                val pb = project3D(b.x, b.y, b.z, yaw, pitch, zoom, w, h)
                                if (pa.zRot <= sliceZ || pb.zRot <= sliceZ) {
                                    drawLine(
                                        color = a.color.copy(alpha = 0.85f),
                                        start = Offset(pa.sx, pa.sy),
                                        end = Offset(pb.sx, pb.sy),
                                        strokeWidth = 3.6f * zoom.coerceIn(0.6f, 2f)
                                    )
                                }
                                if (pa.zRot <= sliceZ) {
                                    chainPickables += a to pa
                                    if (a.id == selectedId) {
                                        drawCircle(Color.White, radius = 8f, center = Offset(pa.sx, pa.sy), style = Stroke(2.5f))
                                        chainTerminalLabels += a to pa
                                    }
                                    if (idx == 0) {
                                        val chainLabel = a.groupTag.ifBlank { "Chain ${cIdx + 1}" }
                                        chainTerminalLabels += a.copy(label = chainLabel) to pa
                                    }
                                }
                            }
                            val lastA = chain.last()
                            val pLast = project3D(lastA.x, lastA.y, lastA.z, yaw, pitch, zoom, w, h)
                            if (pLast.zRot <= sliceZ) chainPickables += lastA to pLast
                        }
                    }

                    // Depth-sorted spheres
                    val projected = scene.spheres
                        .map { s -> s to project3D(s.x, s.y, s.z, yaw, pitch, zoom, w, h) }
                        .filter { (_, p) -> p.zRot <= sliceZ }
                        .sortedBy { (_, p) -> p.depth }
                    projectedCache = projected + chainPickables

                    projected.forEach { (s, p) ->
                        val rad = (s.radius * minOf(w, h) * 0.25f * p.scale).coerceIn(3f, minOf(w, h) * 0.44f)
                        val isSel = s.id == selectedId
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = (s.alpha * 0.82f).coerceIn(0f, 1f)),
                                    s.color.copy(alpha = s.alpha),
                                    s.color.copy(alpha = (s.alpha * 0.35f).coerceIn(0f, 1f))
                                ),
                                center = Offset(p.sx - rad * 0.28f, p.sy - rad * 0.28f),
                                radius = rad.coerceAtLeast(1f)
                            ),
                            radius = rad,
                            center = Offset(p.sx, p.sy)
                        )
                        if (isSel) {
                            drawCircle(
                                color = primary,
                                radius = rad + 5f,
                                center = Offset(p.sx, p.sy),
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    // Floating 3D Callout Labels
                    if (showLabels) {
                        val toLabel = buildList {
                            // Deduplicate identical repeating groups while keeping the selected or front-most sphere
                            val seenGroups = mutableSetOf<String>()
                            projected.asReversed().forEach { pair ->
                                val (s, _) = pair
                                val key = s.groupTag.ifBlank { s.label }
                                if (s.id == selectedId || (seenGroups.add(key) && size < 14)) {
                                    add(pair)
                                }
                            }
                            chainTerminalLabels.take(4).forEach { add(it) }
                        }

                        toLabel.forEachIndexed { idx, (s, p) ->
                            val rad = (s.radius * minOf(w, h) * 0.25f * p.scale).coerceIn(3f, minOf(w, h) * 0.44f)
                            val anchorOffsetY = if (s.alpha < 0.35f) -rad * 0.62f else if (idx % 2 == 0) -rad - 14f else rad + 8f
                            val anchorOffsetX = if (s.alpha < 0.35f) -rad * 0.55f else 6f
                            drawCalloutBadge(
                                textMeasurer = textMeasurer,
                                label = s.label,
                                origin = Offset(p.sx, p.sy),
                                target = Offset(p.sx + anchorOffsetX, p.sy + anchorOffsetY),
                                accent = if (s.id == selectedId) Color.White else s.color,
                                isSelected = s.id == selectedId,
                                canvasW = w,
                                canvasH = h
                            )
                        }
                    }
                }

                // Top-left Simulation Title Badge
                if (scene.title.isNotBlank()) {
                    Surface(
                        color = bg.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .border(1.dp, semantic.glassBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = scene.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Overlay controls (top-right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = { showLabels = !showLabels },
                        modifier = Modifier.size(38.dp).testTag("3d_labels_toggle")
                    ) {
                        Icon(
                            imageVector = if (showLabels) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (showLabels) "Hide 3D labels" else "Show 3D labels",
                            tint = primary
                        )
                    }
                    IconButton(
                        onClick = { autoSpin = !autoSpin },
                        modifier = Modifier.size(38.dp).testTag("3d_spin_toggle")
                    ) {
                        Icon(
                            imageVector = if (autoSpin) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Toggle auto rotate",
                            tint = primary
                        )
                    }
                    IconButton(
                        onClick = { zoom = (zoom * 1.18f).coerceAtMost(2.8f) },
                        modifier = Modifier.size(38.dp).testTag("3d_zoom_in")
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = primary)
                    }
                    IconButton(
                        onClick = { zoom = (zoom / 1.18f).coerceAtLeast(0.55f) },
                        modifier = Modifier.size(38.dp).testTag("3d_zoom_out")
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = primary)
                    }
                    IconButton(
                        onClick = { yaw = 0.45f; pitch = 0.25f; zoom = 1.0f; sliceZ = 1.1f },
                        modifier = Modifier.size(38.dp).testTag("3d_reset_cam")
                    ) {
                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Reset 3D camera", tint = primary)
                    }
                    if (allowFullscreen && !fullscreen) {
                        IconButton(
                            onClick = { fullscreen = true },
                            modifier = Modifier.size(38.dp).testTag("3d_fullscreen_btn")
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.cd_fullscreen), tint = primary)
                        }
                    }
                }

                if (scene.caption.isNotBlank()) {
                    Surface(
                        color = bg.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = scene.caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Explicit Legend Strip for Labeled Structures
            val computedLegend = remember(scene) {
                if (scene.legendItems.isNotEmpty()) {
                    scene.legendItems
                } else {
                    scene.spheres
                        .distinctBy { it.groupTag.ifBlank { it.label } }
                        .take(8)
                        .map { LegendItem3D(it.groupTag.ifBlank { it.label }, it.color, it.id) }
                }
            }
            if (computedLegend.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    computedLegend.forEach { item ->
                        val isSel = item.targetId != null && item.targetId == selectedId
                        Surface(
                            color = if (isSel) item.color.copy(alpha = 0.24f) else surf.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSel) item.color else semantic.glassBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .then(
                                    if (onSelectObject != null && item.targetId != null) {
                                        Modifier.clickable { onSelectObject(item.targetId) }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (showSliceControl) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Slice Z (${"%.2f".format(sliceZ)}) • ${"%.1f".format(zoom)}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.mutedText
                    )
                    Slider(
                        value = sliceZ,
                        onValueChange = { sliceZ = it },
                        valueRange = -0.8f..1.1f,
                        modifier = Modifier.weight(1f).testTag("3d_slice_slider")
                    )
                }
            }
        }
    }

    Box(modifier = modifier) {
        SceneBody(Modifier.height(height))
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(scene.title.ifBlank { "Fullscreen 3D Exploration" }, style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { fullscreen = false }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close fullscreen")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SceneBody(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun DrawScope.drawCalloutBadge(
    textMeasurer: TextMeasurer,
    label: String,
    origin: Offset,
    target: Offset,
    accent: Color,
    isSelected: Boolean,
    canvasW: Float,
    canvasH: Float
) {
    val cleanLabel = label.take(22)
    val measured = textMeasurer.measure(
        text = cleanLabel,
        style = TextStyle(
            color = Color.White,
            fontSize = TextUnit(9.5f, TextUnitType.Sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    )
    val pillW = measured.size.width + 12f
    val pillH = measured.size.height + 6f
    val left = (target.x - pillW * 0.5f).coerceIn(6f, (canvasW - pillW - 6f).coerceAtLeast(6f))
    val top = (target.y - pillH * 0.5f).coerceIn(34f, (canvasH - pillH - 28f).coerceAtLeast(34f))

    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = origin,
        end = Offset(left + pillW * 0.5f, top + pillH * 0.5f),
        strokeWidth = if (isSelected) 2.2f else 1.3f
    )
    drawRoundRect(
        color = Color(0xDA08101E),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = accent.copy(alpha = if (isSelected) 0.95f else 0.55f),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = if (isSelected) 2f else 1f)
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(left + 6f, top + 3f)
    )
}
