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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.BookmarkItem
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.components.buildCell3DScene
import com.example.ui.components.buildOrgans3DScene
import com.example.ui.theme.LocalImmunoColors

@Composable
fun AtlasScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?) -> Unit,
    onRecordRecent: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("Organs 3D") }
    var organFilter by remember { mutableStateOf("All") }
    var selectedOrganId by remember { mutableStateOf(ImmunologyKb.ORGANS.first().id) }
    var selectedCellId by remember { mutableStateOf(ImmunologyKb.CELLS.first().id) }
    var compareCellId by remember { mutableStateOf(ImmunologyKb.CELLS[1].id) }
    var explanationLevel by remember { mutableStateOf("Undergraduate") }
    var showReceptors by remember { mutableStateOf(true) }

    val selectedOrgan = remember(selectedOrganId) { ImmunologyKb.ORGANS.first { it.id == selectedOrganId } }
    val selectedCell = remember(selectedCellId) { ImmunologyKb.CELLS.first { it.id == selectedCellId } }
    val compareCell = remember(compareCellId) { ImmunologyKb.CELLS.first { it.id == compareCellId } }

    val isBookmarked = bookmarks.any {
        (subTab == "Organs 3D" && it.itemId == selectedOrgan.id) ||
            (subTab != "Organs 3D" && it.itemId == selectedCell.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Immune Atlas",
            subtitle = "Anatomical 3D organs, 15 immune cells, receptors & cellular crosstalk.",
            badge = "Established + Illustrative 3D",
            isBookmarked = isBookmarked,
            onToggleBookmark = {
                if (subTab == "Organs 3D") {
                    onToggleBookmark("organ", selectedOrgan.id, selectedOrgan.name, selectedOrgan.summary)
                } else {
                    onToggleBookmark("cell", selectedCell.id, selectedCell.name, selectedCell.group)
                }
            }
        )

        PillSelectorRow(
            options = listOf("Organs 3D", "Cells 3D", "Interactions", "Compare"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "atlas_tab"
        )

        BannerDisclaimer(
            text = "Spatial organ & cell geometries are illustrative educational models; biological specifications reflect established textbook physiology.",
            isSimulation = false
        )

        when (subTab) {
            "Organs 3D" -> {
                PillSelectorRow(
                    options = listOf("All", "Primary", "Secondary", "Mucosal"),
                    selected = organFilter,
                    onSelect = { organFilter = it },
                    tagPrefix = "organ_filter"
                )
                Interactive3DScene(
                    scene = buildOrgans3DScene(selectedOrganId, organFilter),
                    reduceMotion = reduceMotion,
                    selectedId = selectedOrganId,
                    onSelectObject = { id ->
                        if (ImmunologyKb.ORGANS.any { it.id == id }) {
                            selectedOrganId = id
                            onRecordRecent(ImmunologyKb.ORGANS.first { it.id == id }.name)
                        }
                    }
                )
                PillSelectorRow(
                    options = ImmunologyKb.ORGANS.map { it.name },
                    selected = selectedOrgan.name,
                    onSelect = { name ->
                        val found = ImmunologyKb.ORGANS.first { it.name == name }
                        selectedOrganId = found.id
                        onRecordRecent(found.name)
                    },
                    tagPrefix = "organ_pick"
                )
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedOrgan.name, style = MaterialTheme.typography.titleLarge)
                        EvidencePill(selectedOrgan.kind)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(selectedOrgan.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text("Physiology: ${selectedOrgan.functionText}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Clinical Relevance: ${selectedOrgan.clinicalNote}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                }
                Simulation3DSyllabusCard(
                    unit = Simulation3DSyllabusKb.byId("organs_3d"),
                    selectedObjectId = selectedOrganId,
                    onFocusObject = { oid ->
                        if (ImmunologyKb.ORGANS.any { it.id == oid }) {
                            selectedOrganId = oid
                            onRecordRecent(ImmunologyKb.ORGANS.first { it.id == oid }.name)
                        }
                    }
                )
            }

            "Cells 3D" -> {
                PillSelectorRow(
                    options = ImmunologyKb.CELLS.map { it.name },
                    selected = selectedCell.name,
                    onSelect = { name ->
                        val found = ImmunologyKb.CELLS.first { it.name == name }
                        selectedCellId = found.id
                        onRecordRecent(found.name)
                    },
                    tagPrefix = "cell_pick"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Surface Receptors Overlay", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = showReceptors, onCheckedChange = { showReceptors = it }, modifier = Modifier.testTag("receptors_switch"))
                }
                Interactive3DScene(
                    scene = buildCell3DScene(selectedCell, showReceptors),
                    reduceMotion = reduceMotion,
                    selectedId = null
                )
                PillSelectorRow(
                    options = ImmunologyKb.EXPLANATION_LEVELS,
                    selected = explanationLevel,
                    onSelect = { explanationLevel = it },
                    tagPrefix = "exp_level"
                )
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedCell.name, style = MaterialTheme.typography.titleLarge)
                        EvidencePill("Established")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$explanationLevel Perspective: ${selectedCell.levels[explanationLevel].orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    AtlasField("Lineage", selectedCell.group)
                    AtlasField("Morphology", selectedCell.morphology)
                    AtlasField("Development", selectedCell.development)
                    AtlasField("Location", selectedCell.location)
                    AtlasField("Surface Markers", selectedCell.markers.joinToString(" • "))
                    AtlasField("Receptors", selectedCell.receptors.joinToString(" • "))
                    AtlasField("Secreted Molecules", selectedCell.secreted.joinToString(" • "))
                    AtlasField("Activation", selectedCell.activation)
                    AtlasField("Clinical Relevance", selectedCell.clinical)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "References: ${selectedCell.refIds.mapNotNull { ImmunologyKb.REFS[it]?.citation }.joinToString(" | ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.mutedText
                    )
                }
                Simulation3DSyllabusCard(
                    unit = Simulation3DSyllabusKb.byId("cells_3d")
                )
            }

            "Interactions" -> {
                ImmunologyKb.INTERACTIONS.forEach { inter ->
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${inter.fromName}  →  ${inter.toName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            EvidencePill("Established")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Synapse Mechanism: ${inter.mechanism}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                        Text("Outcome: ${inter.outcome}", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                    }
                }
            }

            "Compare" -> {
                Text("Select Primary & Comparison Cells", style = MaterialTheme.typography.titleSmall)
                PillSelectorRow(
                    options = ImmunologyKb.CELLS.map { it.name },
                    selected = selectedCell.name,
                    onSelect = { name -> selectedCellId = ImmunologyKb.CELLS.first { it.name == name }.id },
                    tagPrefix = "cmp_a"
                )
                PillSelectorRow(
                    options = ImmunologyKb.CELLS.map { it.name },
                    selected = compareCell.name,
                    onSelect = { name -> compareCellId = ImmunologyKb.CELLS.first { it.name == name }.id },
                    tagPrefix = "cmp_b"
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(selectedCell.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        AtlasField("Group", selectedCell.group)
                        AtlasField("Markers", selectedCell.markers.joinToString())
                        AtlasField("Secreted", selectedCell.secreted.joinToString())
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(compareCell.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        AtlasField("Group", compareCell.group)
                        AtlasField("Markers", compareCell.markers.joinToString())
                        AtlasField("Secreted", compareCell.secreted.joinToString())
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AtlasField(label: String, value: String) {
    val semantic = LocalImmunoColors.current
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
