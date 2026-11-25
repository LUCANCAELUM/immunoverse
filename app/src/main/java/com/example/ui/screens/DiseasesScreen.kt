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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.DiseaseKb
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.BookmarkItem
import com.example.ui.components.BannerDisclaimer
import com.example.ui.components.ChartSeries
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.Interactive3DScene
import com.example.ui.components.LineChartCanvas
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.components.buildHivVirion3DScene
import com.example.ui.theme.LocalImmunoColors
import kotlin.math.exp

@Composable
fun DiseasesScreen(
    reduceMotion: Boolean,
    bookmarks: List<BookmarkItem>,
    onToggleBookmark: (String, String, String, String?) -> Unit,
    onRecordRecent: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("Catalog & Compare") }
    var selectedDiseaseId by remember { mutableStateOf(DiseaseKb.DISEASES.first().id) }
    var compareDiseaseId by remember { mutableStateOf(DiseaseKb.DISEASES[1].id) }
    var compareMode by remember { mutableStateOf(false) }

    // HIV Latency-Reversal ("Shock and Kill") pedagogical toy model parameters
    var lraStrength by remember { mutableFloatStateOf(0.45f) }
    var immuneKill by remember { mutableFloatStateOf(0.55f) }

    val selected = remember(selectedDiseaseId) { DiseaseKb.DISEASES.first { it.id == selectedDiseaseId } }
    val compared = remember(compareDiseaseId) { DiseaseKb.DISEASES.first { it.id == compareDiseaseId } }
    val isBookmarked = bookmarks.any { it.itemId == selected.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Disease Intelligence & HIV",
            subtitle = "13 clinical disease profiles, comparison & dedicated HIV immunology lab.",
            badge = "Educational KB",
            isBookmarked = isBookmarked,
            onToggleBookmark = { onToggleBookmark("disease", selected.id, selected.name, selected.agent) }
        )

        PillSelectorRow(
            options = listOf("Catalog & Compare", "HIV Immunology Research"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "dis_tab"
        )

        if (subTab == "Catalog & Compare") {
            PillSelectorRow(
                options = DiseaseKb.DISEASES.map { it.name },
                selected = selected.name,
                onSelect = { name ->
                    val found = DiseaseKb.DISEASES.first { it.name == name }
                    selectedDiseaseId = found.id
                    onRecordRecent(found.name)
                },
                tagPrefix = "dis_pick"
            )

            PillSelectorRow(
                options = listOf("Single Profile", "Side-by-Side Compare"),
                selected = if (compareMode) "Side-by-Side Compare" else "Single Profile",
                onSelect = { compareMode = (it == "Side-by-Side Compare") },
                tagPrefix = "dis_cmp_mode"
            )

            if (!compareMode) {
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selected.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        EvidencePill(selected.category)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Agent / Etiology: ${selected.agent}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    DiseaseField("Overview", selected.overview)
                    DiseaseField("Transmission", selected.transmission)
                    DiseaseField("Pathophysiology", selected.pathophysiology)
                    DiseaseField("Immunological Mechanism", selected.immunoMechanism)
                    DiseaseField("Clinical Manifestations", selected.clinicalManifestations)
                    DiseaseField("Epidemiology", selected.epidemiology)
                    DiseaseField("Diagnostics", selected.diagnostics)
                    DiseaseField("Prevention", selected.prevention)
                    DiseaseField("Treatment Concepts", selected.treatment)
                    DiseaseField("Research Frontiers", selected.research)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sources: ${selected.refIds.mapNotNull { ImmunologyKb.REFS[it]?.citation }.joinToString(" | ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.mutedText
                    )
                }
            } else {
                PillSelectorRow(
                    options = DiseaseKb.DISEASES.map { it.name },
                    selected = compared.name,
                    onSelect = { name -> compareDiseaseId = DiseaseKb.DISEASES.first { it.name == name }.id },
                    tagPrefix = "dis_cmp_b"
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(selected.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        DiseaseField("Agent", selected.agent)
                        DiseaseField("Immune Axis", selected.immunoMechanism)
                        DiseaseField("Diagnostics", selected.diagnostics)
                        DiseaseField("Treatment", selected.treatment)
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(compared.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        DiseaseField("Agent", compared.agent)
                        DiseaseField("Immune Axis", compared.immunoMechanism)
                        DiseaseField("Diagnostics", compared.diagnostics)
                        DiseaseField("Treatment", compared.treatment)
                    }
                }
            }
        } else {
            BannerDisclaimer(
                text = "Dedicated HIV Immunology Research section — strictly educational biology, cART mechanisms & research hypotheses. Never provides actionable pathogen engineering or clinical dosing.",
                isSimulation = false
            )

            var selectedVirionPart by remember { mutableStateOf<String?>(null) }
            Interactive3DScene(
                scene = remember { buildHivVirion3DScene() },
                reduceMotion = reduceMotion,
                selectedId = selectedVirionPart,
                onSelectObject = { selectedVirionPart = it },
                height = 250.dp
            )
            Simulation3DSyllabusCard(
                unit = Simulation3DSyllabusKb.byId("hiv_virion_3d"),
                selectedObjectId = selectedVirionPart,
                onFocusObject = { selectedVirionPart = it }
            )

            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("6-Stage HIV-1 Life Cycle & ART Targets", style = MaterialTheme.typography.titleMedium)
                    EvidencePill("Established")
                }
                Spacer(Modifier.height(6.dp))
                DiseaseKb.HIV_STAGES.forEach { st ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${st.step}. ${st.title}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(st.detail, style = MaterialTheme.typography.bodySmall)
                        Text("Pharmacologic Class: ${st.targetClass}", style = MaterialTheme.typography.labelSmall, color = semantic.established)
                    }
                }
            }

            // Conceptual Latency-Reversal Toy Reservoir Model
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Conceptual Latent Reservoir Model", style = MaterialTheme.typography.titleMedium)
                    EvidencePill("Illustrative Hypothesis")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Compares slow natural reservoir half-life under ART alone (t1/2 ~44 months) vs conceptual LRA ('shock') + CTL/bNAb ('kill') clearance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.mutedText
                )
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Latency Reversal Activation", style = MaterialTheme.typography.labelMedium)
                    Text("${(lraStrength * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = lraStrength, onValueChange = { lraStrength = it }, valueRange = 0f..1f, modifier = Modifier.testTag("hiv_lra_slider"))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CTL / bNAb Elimination Rate", style = MaterialTheme.typography.labelMedium)
                    Text("${(immuneKill * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Slider(value = immuneKill, onValueChange = { immuneKill = it }, valueRange = 0f..1f, modifier = Modifier.testTag("hiv_kill_slider"))

                val artOnlyPts = remember {
                    (0..24).map { m -> m.toDouble() to 100.0 * exp(-0.0157 * m) }
                }
                val shockKillPts = remember(lraStrength, immuneKill) {
                    val effRate = 0.0157 + 0.14 * lraStrength * immuneKill
                    (0..24).map { m -> m.toDouble() to 100.0 * exp(-effRate * m) }
                }
                LineChartCanvas(
                    series = listOf(
                        ChartSeries("ART Alone (%)", semantic.mutedText, artOnlyPts),
                        ChartSeries("LRA + Immune Kill (%)", MaterialTheme.colorScheme.primary, shockKillPts)
                    ),
                    title = "Illustrative Reservoir Decay Over 24 Months",
                    height = 175.dp
                )
            }

            Text("HIV Immunology Research Briefings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DiseaseKb.HIV_TOPICS.forEach { topic ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(topic.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        EvidencePill(topic.status)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(topic.summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DiseaseField(label: String, value: String) {
    val semantic = LocalImmunoColors.current
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
