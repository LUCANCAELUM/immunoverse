package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.domain.SimParams
import com.example.domain.SimPreset
import com.example.domain.SimResult
import com.example.domain.SimStep
import com.example.ui.theme.LocalImmunoColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ImmunePresetBriefingCard(preset: SimPreset?) {
    if (preset == null) return
    val semantic = LocalImmunoColors.current
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preset.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            EvidencePill("Preset Scenario")
        }
        Spacer(Modifier.height(4.dp))
        Text(preset.summary, style = MaterialTheme.typography.bodySmall)
        if (preset.biologicalContext.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Biological Context: ${preset.biologicalContext}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
        }
        if (preset.dominantAxis.isNotBlank()) {
            Text("Dominant Axis: ${preset.dominantAxis}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        if (preset.expectedOutcome.isNotBlank()) {
            Text("Expected Kinetics: ${preset.expectedOutcome}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
        }
    }
}

@Composable
fun DetailedImmuneArenaCard(
    currentStep: SimStep,
    params: SimParams,
    reduceMotion: Boolean,
    replayDay: Float,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onScrubDay: (Float) -> Unit
) {
    val semantic = LocalImmunoColors.current
    var arenaMode by remember { mutableStateOf("2D Labeled Arena") }
    var selected3dNode by remember { mutableStateOf<String?>(null) }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sim 6: Immune Response Arena • Day ${"%.1f".format(currentStep.day)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    currentStep.phaseTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(44.dp).testTag("sim_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause simulation replay" else "Play simulation replay",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        PillSelectorRow(
            options = listOf("2D Labeled Arena", "3D Live Arena"),
            selected = arenaMode,
            onSelect = { arenaMode = it },
            tagPrefix = "sim_arena_mode"
        )
        Spacer(Modifier.height(6.dp))

        if (arenaMode == "2D Labeled Arena") {
            LabeledImmuneArena2DCanvas(currentStep = currentStep)
        } else {
            Interactive3DScene(
                scene = buildImmuneResponse3DScene(currentStep, params),
                reduceMotion = reduceMotion,
                selectedId = selected3dNode,
                onSelectObject = { selected3dNode = it },
                height = 240.dp
            )
            if (selected3dNode != null) {
                Text(
                    text = "Focused 3D Compartment: $selected3dNode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Slider(
            value = replayDay.coerceIn(0f, params.durationDays.toFloat()),
            onValueChange = onScrubDay,
            valueRange = 0f..params.durationDays.toFloat().coerceAtLeast(5f),
            modifier = Modifier.testTag("sim_replay_slider")
        )

        // Live 9+ Compartment Readout Strip
        Text(
            text = "Ag: ${"%.2f".format(currentStep.antigen)} | Innate: ${"%.2f".format(currentStep.innate)} | APC: ${"%.0f".format(currentStep.apcActivation * 100)}% | T: ${"%.2f".format(currentStep.tCell)} | B: ${"%.2f".format(currentStep.bCell)} | Ab: ${"%.2f".format(currentStep.antibody)} | Affinity: ${"%.2f".format(currentStep.affinityMaturation)}x | Mem: ${"%.2f".format(currentStep.memory)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LabeledImmuneArena2DCanvas(
    currentStep: SimStep,
    height: Dp = 156.dp
) {
    val semantic = LocalImmunoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val crit = semantic.critical
    val est = semantic.established
    val innateCol = semantic.illustrative
    val textMeasurer = rememberTextMeasurer()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.small)
        ) {
            val w = size.width
            val h = size.height
            val daySeed = currentStep.day.toFloat()

            // Spatial Compartment Bands
            drawLine(
                color = semantic.glassBorder,
                start = Offset(w * 0.36f, 8f),
                end = Offset(w * 0.36f, h - 8f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
            drawLine(
                color = semantic.glassBorder,
                start = Offset(w * 0.72f, 8f),
                end = Offset(w * 0.72f, h - 8f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            val bandStyle = TextStyle(
                color = semantic.mutedText.copy(alpha = 0.85f),
                fontSize = TextUnit(8.5f, TextUnitType.Sp),
                fontWeight = FontWeight.SemiBold
            )
            drawText(textMeasurer, "INFECTION NICHE", Offset(8f, 6f), bandStyle.copy(color = crit.copy(alpha = 0.85f)))
            drawText(textMeasurer, "LYMPH NODE / GC", Offset(w * 0.38f, 6f), bandStyle.copy(color = primary.copy(alpha = 0.85f)))
            drawText(textMeasurer, "HUMORAL / MEMORY", Offset(w * 0.74f, 6f), bandStyle.copy(color = est.copy(alpha = 0.9f)))

            // Cytokine glow halo scaled by cytokine level
            val cytAlpha = (currentStep.cytokine * 0.14).toFloat().coerceIn(0.02f, 0.32f)
            drawCircle(
                color = tertiary.copy(alpha = cytAlpha),
                radius = (26f + currentStep.cytokine.toFloat() * 14f).coerceAtMost(h * 0.45f),
                center = Offset(w * 0.20f, h * 0.56f)
            )

            // Pathogen / Antigen dots (Infection niche)
            val agCount = (currentStep.antigen * 8).toInt().coerceIn(0, 34)
            for (k in 0 until agCount) {
                val x = w * (0.06f + ((sin(daySeed * 0.9f + k * 1.7f) * 0.5f + 0.5f) * 0.25f))
                val y = h * (0.24f + ((cos(daySeed * 1.1f + k * 2.3f) * 0.5f + 0.5f) * 0.62f))
                drawCircle(crit, radius = 5.5f, center = Offset(x, y))
            }

            // Innate Neutrophils / Macrophages
            val innateCount = (currentStep.innate * 6).toInt().coerceIn(1, 20)
            for (k in 0 until innateCount) {
                val x = w * (0.14f + ((cos(daySeed * 0.6f + k * 1.4f) * 0.5f + 0.5f) * 0.34f))
                val y = h * (0.24f + ((sin(daySeed * 0.7f + k * 1.9f) * 0.5f + 0.5f) * 0.62f))
                drawCircle(innateCol, radius = 6.5f, center = Offset(x, y))
            }

            // T Effectors (migrating between LN and tissue)
            val tCount = (currentStep.tCell * 9).toInt().coerceIn(1, 28)
            for (k in 0 until tCount) {
                val x = w * (0.34f + ((cos(daySeed * 0.7f + k * 1.3f) * 0.5f + 0.5f) * 0.34f))
                val y = h * (0.22f + ((sin(daySeed * 0.8f + k * 1.9f) * 0.5f + 0.5f) * 0.64f))
                drawCircle(primary, radius = 6.8f, center = Offset(x, y))
            }

            // Antibodies (Humoral cloud across zones)
            val abCount = (currentStep.antibody * 6).toInt().coerceIn(0, 36)
            for (k in 0 until abCount) {
                val x = w * (0.12f + ((sin(daySeed * 1.3f + k * 2.1f) * 0.5f + 0.5f) * 0.80f))
                val y = h * (0.22f + ((cos(daySeed * 0.6f + k * 1.1f) * 0.5f + 0.5f) * 0.66f))
                drawCircle(est, radius = 3.8f, center = Offset(x, y))
            }

            // Memory Niche Cluster (right compartment)
            if (currentStep.memory > 0.06) {
                val memCenter = Offset(w * 0.86f, h * 0.54f)
                drawCircle(secondary.copy(alpha = 0.25f), radius = 22f, center = memCenter)
                drawCircle(secondary, radius = 10f, center = memCenter)
                drawCircle(Color.White.copy(alpha = 0.7f), radius = 14f, center = memCenter, style = Stroke(width = 1.5f))
            }

            // Floating Count Labels directly inside zones
            val countStyle = TextStyle(color = Color.White, fontSize = TextUnit(8.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold)
            drawText(textMeasurer, "Ag x$agCount", Offset(10f, h - 20f), countStyle.copy(color = crit))
            drawText(textMeasurer, "Innate x$innateCount • Cyt ${"%.1f".format(currentStep.cytokine)}", Offset(w * 0.38f, h - 20f), countStyle.copy(color = innateCol))
            drawText(textMeasurer, "IgG x$abCount • Mem ${"%.2f".format(currentStep.memory)}", Offset(w * 0.73f, h - 20f), countStyle.copy(color = est))
        }

        // Explicit Visual Legend Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArenaLegendDot("Pathogen (Ag)", crit)
            ArenaLegendDot("Innate Phagocytes", innateCol)
            ArenaLegendDot("Cytokine Cloud", tertiary)
            ArenaLegendDot("CD4/CD8 T Cells", primary)
            ArenaLegendDot("Antibodies (IgG)", est)
            ArenaLegendDot("Memory B/T Niche", secondary)
        }
    }
}

@Composable
private fun ArenaLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SimulationMetricsDetailCard(result: SimResult) {
    val semantic = LocalImmunoColors.current
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quantitative Biomarkers & Burden", style = MaterialTheme.typography.titleMedium)
            EvidencePill("Educational ODE")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = result.mechanisticNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        MetricRow("Peak Pathogen / Antigen", "${"%.2f".format(result.peakAntigen)} U", "Ag AUC: ${"%.1f".format(result.aucAntigen)} U·d")
        MetricRow(
            "Pathogen Clearance Timestamps",
            "t50: ${result.timeTo50ClearanceDays?.let { "Day ${"%.1f".format(it)}" } ?: "Not Reached"}",
            "Sterile (<0.05): ${result.timeToClearanceDays?.let { "Day ${"%.1f".format(it)}" } ?: "Persistent"}"
        )
        MetricRow("Adaptive Effector Peaks", "T: ${"%.2f".format(result.peakTCell)} | B: ${"%.2f".format(result.peakBCell)}", "Plasma: ${"%.2f".format(result.peakPlasma)} | Ab: ${"%.2f".format(result.peakAntibody)}")
        MetricRow("Germinal Center & Memory", "Affinity: ${"%.2f".format(result.peakAffinityMaturation)}x", "Memory Peak: ${"%.2f".format(result.peakMemory)} U")
        MetricRow("Effector : Pathogen Ratio (End)", "${"%.2f".format(result.effectorToPathogenRatio)} : 1", "Cytokine AUC: ${"%.1f".format(result.aucCytokine)} U·d")
        MetricRow(
            "Inflammatory & Tissue Stress",
            "CRS Tier: ${result.cytokineStormRisk}",
            "Damage Peak: ${"%.2f".format(result.peakDamage)} (AUC ${"%.1f".format(result.aucDamage)})",
            valueColor = if (result.cytokineStormRisk.contains("Severe") || result.cytokineStormRisk == "Elevated") semantic.critical else semantic.established
        )

        Spacer(Modifier.height(6.dp))
        Text("Kinetics Timeline Events:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        result.events.forEach { ev ->
            Text("• $ev", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    primaryValue: String,
    secondaryValue: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val semantic = LocalImmunoColors.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                Text(primaryValue, style = MaterialTheme.typography.labelLarge, color = valueColor)
            }
            Text(secondaryValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
