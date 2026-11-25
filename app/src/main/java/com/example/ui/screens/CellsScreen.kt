package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.BookmarkItem
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.MicroscopyCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.components.buildCellularProcess3DScene
import com.example.ui.components.buildOrganelle3DScene
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay

@Composable
fun CellsScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("Organelles 3D") }
    var selectedOrganelleId by remember { mutableStateOf(ImmunologyKb.ORGANELLES.first().id) }
    var selectedProcessId by remember { mutableStateOf(ImmunologyKb.PROCESSES.first().id) }
    var processStep by remember { mutableIntStateOf(0) }
    var livePhase by remember { mutableFloatStateOf(0.35f) }
    var selectedArenaNodeId by remember { mutableStateOf<String?>(null) }
    var autoAdvance by remember { mutableStateOf(true) }

    var stainMode by remember { mutableStateOf("Immunofluorescence") }
    var mag by remember { mutableFloatStateOf(40f) }

    val selectedOrg = remember(selectedOrganelleId) { ImmunologyKb.ORGANELLES.first { it.id == selectedOrganelleId } }
    val selectedProc = remember(selectedProcessId) { ImmunologyKb.PROCESSES.first { it.id == selectedProcessId } }
    val arenaScene = remember(selectedProc, livePhase, selectedArenaNodeId) {
        buildCellularProcess3DScene(selectedProc, livePhase, selectedArenaNodeId)
    }
    val focusedSphere = remember(arenaScene, selectedArenaNodeId) {
        arenaScene.spheres.firstOrNull { it.id == selectedArenaNodeId }
    }

    LaunchedEffect(autoAdvance, selectedProcessId, reduceMotion) {
        if (autoAdvance) {
            val stepDelay = if (reduceMotion) 180L else 55L
            val delta = if (reduceMotion) 0.14f else 0.045f
            while (true) {
                delay(stepDelay)
                val maxRange = selectedProc.steps.size.toFloat()
                val next = livePhase + delta
                livePhase = if (next >= maxRange) 0f else next
                processStep = livePhase.toInt().coerceIn(0, selectedProc.steps.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val activeId = if (subTab == "Organelles 3D") selectedOrg.id else selectedProc.id
        ModuleHeader(
            title = "Cellular Intelligence",
            subtitle = "Interactive organelle architecture, live 3D cellular processes arena & microscopy.",
            badge = "Established",
            isBookmarked = bookmarks.any { it.itemId == activeId },
            onToggleBookmark = {
                if (subTab == "Organelles 3D") {
                    onToggleBookmark("organelle", selectedOrg.id, selectedOrg.name, "3D Organelle", selectedOrg.role)
                } else {
                    onToggleBookmark("process", selectedProc.id, selectedProc.name, "3D Cellular Process", selectedProc.summary)
                }
            }
        )

        PillSelectorRow(
            options = listOf("Organelles 3D", "Cellular Processes", "Microscopy Stain"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "cells_tab"
        )

        when (subTab) {
            "Organelles 3D" -> {
                Interactive3DScene(
                    scene = buildOrganelle3DScene(selectedOrganelleId),
                    reduceMotion = reduceMotion,
                    selectedId = selectedOrganelleId,
                    onSelectObject = { id ->
                        if (ImmunologyKb.ORGANELLES.any { it.id == id }) {
                            selectedOrganelleId = id
                        }
                    }
                )
                PillSelectorRow(
                    options = ImmunologyKb.ORGANELLES.map { it.name },
                    selected = selectedOrg.name,
                    onSelect = { name -> selectedOrganelleId = ImmunologyKb.ORGANELLES.first { it.name == name }.id },
                    tagPrefix = "organelle_pick"
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedOrg.name, style = MaterialTheme.typography.titleLarge)
                        EvidencePill("Established")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(selectedOrg.role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(selectedOrg.detail, style = MaterialTheme.typography.bodySmall)
                }
                Simulation3DSyllabusCard(
                    unit = Simulation3DSyllabusKb.byId("organelles_3d"),
                    selectedObjectId = selectedOrganelleId,
                    onFocusObject = { oid ->
                        if (ImmunologyKb.ORGANELLES.any { it.id == oid }) {
                            selectedOrganelleId = oid
                        }
                    }
                )
            }

            "Cellular Processes" -> {
                PillSelectorRow(
                    options = ImmunologyKb.PROCESSES.map { it.name },
                    selected = selectedProc.name,
                    onSelect = { name ->
                        selectedProcessId = ImmunologyKb.PROCESSES.first { it.name == name }.id
                        processStep = 0
                        livePhase = 0.25f
                        selectedArenaNodeId = null
                    },
                    tagPrefix = "process_pick"
                )

                // Live 3D Cellular Process Arena
                Interactive3DScene(
                    scene = arenaScene,
                    reduceMotion = reduceMotion,
                    selectedId = selectedArenaNodeId,
                    onSelectObject = { oid -> selectedArenaNodeId = oid },
                    height = 265.dp
                )

                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Live 3D Arena Controls", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        EvidencePill("Stage ${processStep + 1} / ${selectedProc.steps.size}")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = focusedSphere?.let { "Focused Structure: ${it.label} (${it.groupTag})" }
                            ?: "Tap any moving organelle or molecule below (or directly in 3D) to highlight & inspect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (focusedSphere != null) MaterialTheme.colorScheme.secondary else semantic.mutedText
                    )
                    Spacer(Modifier.height(6.dp))
                    val legendTargets = arenaScene.legendItems.filter { it.targetId != null }
                    if (legendTargets.isNotEmpty()) {
                        PillSelectorRow(
                            options = legendTargets.map { it.label },
                            selected = legendTargets.firstOrNull { it.targetId == selectedArenaNodeId }?.label ?: "",
                            onSelect = { lbl ->
                                val tid = legendTargets.firstOrNull { it.label == lbl }?.targetId
                                selectedArenaNodeId = if (selectedArenaNodeId == tid) null else tid
                            },
                            tagPrefix = "arena_mol"
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Slider(
                        value = livePhase,
                        onValueChange = {
                            autoAdvance = false
                            livePhase = it
                            processStep = it.toInt().coerceIn(0, selectedProc.steps.size - 1)
                        },
                        valueRange = 0f..selectedProc.steps.size.toFloat(),
                        modifier = Modifier.testTag("cellular_arena_phase_slider")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                autoAdvance = false
                                val nextStep = (processStep + 1) % selectedProc.steps.size
                                processStep = nextStep
                                livePhase = nextStep + 0.35f
                            },
                            modifier = Modifier.weight(1f).testTag("next_process_step_btn")
                        ) {
                            Text("Next Stage")
                        }
                        OutlinedButton(
                            onClick = { autoAdvance = !autoAdvance },
                            modifier = Modifier.weight(1f).testTag("auto_process_btn")
                        ) {
                            Text(if (autoAdvance) "Pause 3D Loop" else "Play 3D Loop")
                        }
                    }
                }

                GlassCard {
                    Text(selectedProc.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(selectedProc.summary, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (livePhase / selectedProc.steps.size.toFloat()).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    selectedProc.steps.forEachIndexed { idx, stepText ->
                        val active = idx == processStep
                        GlassCard(
                            onClick = {
                                autoAdvance = false
                                processStep = idx
                                livePhase = idx + 0.35f
                            },
                            accentColor = if (active) MaterialTheme.colorScheme.primary else null,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = "Stage ${idx + 1}: $stepText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            "Microscopy Stain" -> {
                PillSelectorRow(
                    options = listOf("Immunofluorescence", "Gram", "H&E"),
                    selected = stainMode,
                    onSelect = { stainMode = it },
                    tagPrefix = "stain_mode"
                )
                MicroscopyCanvas(stainMode = stainMode, magnification = mag, height = 220.dp)
                GlassCard {
                    Text("Objective Magnification: ${mag.toInt()}x", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = mag,
                        onValueChange = { mag = it },
                        valueRange = 10f..100f,
                        modifier = Modifier.testTag("microscopy_mag_slider")
                    )
                    Text(
                        text = when (stainMode) {
                            "Immunofluorescence" -> "Simulated fluorescence field: cyan cytoplasm membrane & green Alexa488 nuclear transcription factor signal."
                            "Gram" -> "Simulated Gram smear: purple crystal-violet Gram-positive cocci vs pink safranin Gram-negative bacilli."
                            else -> "Simulated H&E section: hematoxylin basophilic nuclei (purple-blue) & eosinophilic cytoplasm (pink)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
