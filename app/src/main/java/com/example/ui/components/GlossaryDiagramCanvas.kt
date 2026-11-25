package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.data.kb.GlossaryDiagramType
import com.example.data.kb.GlossaryTerm
import com.example.data.kb.ImmunologyGlossaryKb
import com.example.data.model.BookmarkItem
import com.example.ui.theme.LocalImmunoColors

private data class DiagramHotspot(
    val partName: String,
    val cxFrac: Float,
    val cyFrac: Float,
    val note: String
)

private fun hotspotsFor(type: GlossaryDiagramType): List<DiagramHotspot> = when (type) {
    GlossaryDiagramType.ANTIBODY_Y_STRUCTURE -> listOf(
        DiagramHotspot("Fab Antigen Site", 0.25f, 0.25f, "Two identical Fab arms bind conformational epitopes with high specificity."),
        DiagramHotspot("CDR Hypervariable Loop", 0.75f, 0.25f, "Six CDR1–CDR3 loops (VH + VL) form the antigen-contacting paratope."),
        DiagramHotspot("Hinge Disulfide (S-S)", 0.50f, 0.52f, "Flexible proline-rich hinge with inter-heavy S–S bonds permits bivalent reach."),
        DiagramHotspot("Fc Effector & C1q Domain", 0.50f, 0.78f, "CH2–CH3 stem binds FcγRs on NK/macrophages, FcRn, and C1q complement.")
    )
    GlossaryDiagramType.MHC_PRESENTATION -> listOf(
        DiagramHotspot("MHC-I + β2m (Endogenous)", 0.26f, 0.34f, "α1/α2/α3 + β2-microglobulin displays 8–10 aa cytosolic/viral peptides."),
        DiagramHotspot("CD8+ CTL Synapse", 0.26f, 0.74f, "CD8 co-receptor stabilizes MHC-I α3 so cytotoxic T cells lyse infected targets."),
        DiagramHotspot("MHC-II (Exogenous)", 0.74f, 0.34f, "α/β heterodimer presents 13–25 aa phagolysosomal peptides on professional APCs."),
        DiagramHotspot("CD4+ Helper Synapse", 0.74f, 0.74f, "CD4 co-receptor binds MHC-II β2, licensing Th1/Th2/Tfh differentiation.")
    )
    GlossaryDiagramType.THREE_SIGNAL_ACTIVATION -> listOf(
        DiagramHotspot("Signal 1: pMHC-TCR", 0.25f, 0.32f, "Specificity check: APC peptide–MHC engages TCRαβ–CD3 ITAMs."),
        DiagramHotspot("Signal 2: CD28-CD80/86", 0.52f, 0.50f, "Survival check: APC B7 (CD80/86) activates CD28 to prevent T-cell anergy."),
        DiagramHotspot("CTLA-4 / PD-1 Brake", 0.78f, 0.32f, "Inhibitory checkpoints terminate proliferation and preserve peripheral tolerance."),
        DiagramHotspot("Signal 3: Polarizing Cytokines", 0.52f, 0.80f, "IL-12, IL-4, or IL-6/TGF-β instruct Th1, Th2, or Th17/Treg fate.")
    )
    GlossaryDiagramType.COMPLEMENT_CASCADE -> listOf(
        DiagramHotspot("Classical/Lectin/Alt Triggers", 0.20f, 0.24f, "C1q–IgG/IgM, MBL–mannose, or C3 tick-over initiate enzymatic amplification."),
        DiagramHotspot("C3 Convertase (C3b + C3a)", 0.48f, 0.45f, "Cleaves abundant serum C3 into inflammatory C3a and covalent C3b."),
        DiagramHotspot("Opsonization", 0.26f, 0.78f, "C3b/iC3b coats pathogens for rapid CR1/CR3 phagocyte engulfment."),
        DiagramHotspot("C5b-C9 MAC Pore", 0.78f, 0.68f, "C5b678 recruits poly-C9 transmembrane barrels causing osmotic cell lysis.")
    )
    GlossaryDiagramType.VDJ_RECOMBINATION -> listOf(
        DiagramHotspot("Germline V/D/J Segments", 0.20f, 0.24f, "Dispersed germline Variable, Diversity, and Joining gene cassettes."),
        DiagramHotspot("RAG1/RAG2 Synapsis", 0.46f, 0.44f, "RAG complex loops DNA at 12/23 RSS hairpins and introduces DSBs."),
        DiagramHotspot("TdT Junctional Diversity", 0.68f, 0.44f, "Terminal deoxynucleotidyl transferase adds random N-nucleotides to CDR3."),
        DiagramHotspot("Rearranged Ig/TCR Exon", 0.52f, 0.78f, "Continuous fused VDJ exon transcribed with Constant (C) exon.")
    )
    GlossaryDiagramType.GERMINAL_CENTER -> listOf(
        DiagramHotspot("Dark Zone (AID SHM)", 0.25f, 0.36f, "CXCR4+ centroblasts rapidly divide while AID mutates Ig V-regions."),
        DiagramHotspot("FDC Antigen Capture", 0.72f, 0.28f, "Follicular dendritic cells hold intact native antigen complexes for affinity testing."),
        DiagramHotspot("Tfh CD40L Selection", 0.72f, 0.56f, "High-affinity centrocytes present pMHC-II to Tfh cells and win CD40L/IL-21 rescue."),
        DiagramHotspot("Plasma / Memory B Output", 0.50f, 0.82f, "Selected clones class-switch and exit as Plasma Cells or Memory B cells.")
    )
    GlossaryDiagramType.CYTOKINE_JAK_STAT -> listOf(
        DiagramHotspot("Cytokine Receptor Dimer", 0.30f, 0.22f, "Ligand binding dimerizes receptor chains across the plasma membrane."),
        DiagramHotspot("JAK1/JAK2/TYK2 Phosphorylation", 0.50f, 0.42f, "Trans-phosphorylated Janus kinases phosphorylate receptor Tyr docking motifs."),
        DiagramHotspot("STAT Dimerization", 0.50f, 0.62f, "Recruited STATs dimerize via reciprocal SH2–pTyr interactions."),
        DiagramHotspot("Nuclear ISRE/GAS Transcription", 0.50f, 0.84f, "STAT dimers enter the nucleus to activate interferon/cytokine effector genes.")
    )
    GlossaryDiagramType.INFLAMMASOME_PYROPTOSIS -> listOf(
        DiagramHotspot("TLR4/NF-κB Priming", 0.22f, 0.28f, "PAMPs upregulate pro-IL-1β, pro-IL-18, and cytosolic NLRP3 sensor."),
        DiagramHotspot("NLRP3-ASC-Caspase-1", 0.52f, 0.48f, "Danger signals (K+ efflux/ATP) assemble ASC specks that activate Caspase-1."),
        DiagramHotspot("Mature IL-1β / IL-18", 0.28f, 0.78f, "Caspase-1 cleaves pro-IL-1β/IL-18 into potent secreted inflammatory cytokines."),
        DiagramHotspot("GSDMD Membrane Pore", 0.78f, 0.74f, "Cleaved Gasdermin D N-termini oligomerize into lytic plasma-membrane pores.")
    )
}

@Composable
fun GlossaryDiagramCanvas(
    diagramType: GlossaryDiagramType,
    focusedPart: String,
    onSelectPart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val textMeasurer = rememberTextMeasurer()
    val spots = remember(diagramType) { hotspotsFor(diagramType) }
    val activeHotspot = spots.firstOrNull { it.partName == focusedPart } ?: spots.first()

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val bgCol = MaterialTheme.colorScheme.surfaceContainerLowest

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Brush.verticalGradient(listOf(bgCol, MaterialTheme.colorScheme.background)))
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.medium)
                .pointerInput(spots) {
                    detectTapGestures { tap ->
                        val nearest = spots.minByOrNull { sp ->
                            val dx = sp.cxFrac * size.width - tap.x
                            val dy = sp.cyFrac * size.height - tap.y
                            dx * dx + dy * dy
                        }
                        if (nearest != null) onSelectPart(nearest.partName)
                    }
                }
                .testTag("glossary_diagram_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (diagramType) {
                    GlossaryDiagramType.ANTIBODY_Y_STRUCTURE ->
                        drawAntibodyYDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.MHC_PRESENTATION ->
                        drawMhcPresentationDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.THREE_SIGNAL_ACTIVATION ->
                        drawThreeSignalDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.COMPLEMENT_CASCADE ->
                        drawComplementDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.VDJ_RECOMBINATION ->
                        drawVdjDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.GERMINAL_CENTER ->
                        drawGerminalCenterDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.CYTOKINE_JAK_STAT ->
                        drawJakStatDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                    GlossaryDiagramType.INFLAMMASOME_PYROPTOSIS ->
                        drawInflammasomeDiagram(textMeasurer, activeHotspot.partName, primary, secondary, tertiary)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = diagramType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Interactive Diagram Part Spotlight Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            diagramType.parts.forEach { part ->
                val selected = part == activeHotspot.partName
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPart(part) },
                    label = { Text(part, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = 40.dp)
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = primary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Diagram Spotlight (${activeHotspot.partName}): ${activeHotspot.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

private fun DrawScope.drawDiagramLabel(
    tm: TextMeasurer,
    text: String,
    centerX: Float,
    centerY: Float,
    accent: Color,
    highlighted: Boolean
) {
    val measured = tm.measure(
        text = text,
        style = TextStyle(
            color = if (highlighted) Color.White else Color(0xFFE2E8F0),
            fontSize = TextUnit(9.8f, TextUnitType.Sp),
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium
        )
    )
    val padX = 10f
    val padY = 5f
    val bw = measured.size.width + padX * 2
    val bh = measured.size.height + padY * 2
    val left = (centerX - bw * 0.5f).coerceIn(4f, (size.width - bw - 4f).coerceAtLeast(4f))
    val top = (centerY - bh * 0.5f).coerceIn(24f, (size.height - bh - 4f).coerceAtLeast(24f))
    drawRoundRect(
        color = if (highlighted) accent.copy(alpha = 0.28f) else Color(0xCC0B1324),
        topLeft = Offset(left, top),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = if (highlighted) accent else accent.copy(alpha = 0.45f),
        topLeft = Offset(left, top),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = if (highlighted) 2.5f else 1.2f)
    )
    drawText(measured, topLeft = Offset(left + padX, top + padY))
}

private fun DrawScope.drawAntibodyYDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f

    // Heavy Chains (Fc stem + left/right Fab heavy)
    val heavyColor = Color(0xFF22E5FF)
    val lightColor = secondary
    val hingeColor = Color(0xFFFFC857)

    // Left & Right Heavy stems
    drawLine(heavyColor, Offset(cx - 12f, h * 0.84f), Offset(cx - 12f, h * 0.52f), 11f, cap = StrokeCap.Round)
    drawLine(heavyColor, Offset(cx + 12f, h * 0.84f), Offset(cx + 12f, h * 0.52f), 11f, cap = StrokeCap.Round)
    // Heavy Fab arms
    drawLine(heavyColor, Offset(cx - 12f, h * 0.52f), Offset(cx - w * 0.22f, h * 0.24f), 11f, cap = StrokeCap.Round)
    drawLine(heavyColor, Offset(cx + 12f, h * 0.52f), Offset(cx + w * 0.22f, h * 0.24f), 11f, cap = StrokeCap.Round)
    // Light Chains parallel to Fab
    drawLine(lightColor, Offset(cx - w * 0.14f, h * 0.45f), Offset(cx - w * 0.28f, h * 0.28f), 9f, cap = StrokeCap.Round)
    drawLine(lightColor, Offset(cx + w * 0.14f, h * 0.45f), Offset(cx + w * 0.28f, h * 0.28f), 9f, cap = StrokeCap.Round)
    // Hinge S-S bridges
    drawLine(hingeColor, Offset(cx - 16f, h * 0.53f), Offset(cx + 16f, h * 0.53f), 4f)
    drawLine(hingeColor, Offset(cx - 16f, h * 0.57f), Offset(cx + 16f, h * 0.57f), 4f)
    // Bound antigens at CDR tips
    drawCircle(tertiary.copy(alpha = 0.8f), radius = 13f, center = Offset(cx - w * 0.27f, h * 0.20f))
    drawCircle(tertiary.copy(alpha = 0.8f), radius = 13f, center = Offset(cx + w * 0.27f, h * 0.20f))

    drawDiagramLabel(tm, "Fab (VH + VL)", cx - w * 0.27f, h * 0.34f, primary, focus.contains("Fab"))
    drawDiagramLabel(tm, "CDR Paratope", cx + w * 0.27f, h * 0.24f, tertiary, focus.contains("CDR"))
    drawDiagramLabel(tm, "Hinge S-S", cx, h * 0.48f, hingeColor, focus.contains("Hinge"))
    drawDiagramLabel(tm, "Fc (C1q / FcγR)", cx, h * 0.80f, heavyColor, focus.contains("Fc"))
}

private fun DrawScope.drawMhcPresentationDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    drawLine(Color.White.copy(alpha = 0.2f), Offset(w * 0.5f, 26f), Offset(w * 0.5f, h - 10f), 2f, pathEffect = dashed)

    // Left: MHC-I + CD8
    drawRoundRect(Color(0xFF22E5FF).copy(alpha = 0.22f), Offset(w * 0.14f, h * 0.24f), Size(w * 0.24f, 36f), CornerRadius(10f, 10f))
    drawCircle(Color(0xFFFFC857), 9f, Offset(w * 0.26f, h * 0.42f)) // Peptide
    drawLine(primary, Offset(w * 0.26f, h * 0.46f), Offset(w * 0.26f, h * 0.64f), 6f)
    drawDiagramLabel(tm, "MHC-I + β2m (8-10aa)", w * 0.25f, h * 0.28f, Color(0xFF22E5FF), focus.contains("MHC-I"))
    drawDiagramLabel(tm, "CD8+ CTL Synapse", w * 0.25f, h * 0.74f, primary, focus.contains("CD8"))

    // Right: MHC-II + CD4
    drawRoundRect(secondary.copy(alpha = 0.22f), Offset(w * 0.62f, h * 0.24f), Size(w * 0.24f, 36f), CornerRadius(10f, 10f))
    drawCircle(Color(0xFF3DDC97), 9f, Offset(w * 0.74f, h * 0.42f)) // Exogenous peptide
    drawLine(tertiary, Offset(w * 0.74f, h * 0.46f), Offset(w * 0.74f, h * 0.64f), 6f)
    drawDiagramLabel(tm, "MHC-II (13-25aa)", w * 0.75f, h * 0.28f, secondary, focus.contains("MHC-II"))
    drawDiagramLabel(tm, "CD4+ Th Cell Synapse", w * 0.75f, h * 0.74f, tertiary, focus.contains("CD4"))
}

private fun DrawScope.drawThreeSignalDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    // APC Membrane top, T-cell membrane bottom
    drawLine(Color(0xFF3DDC97).copy(alpha = 0.5f), Offset(24f, h * 0.20f), Offset(w - 24f, h * 0.20f), 6f)
    drawLine(primary.copy(alpha = 0.5f), Offset(24f, h * 0.78f), Offset(w - 24f, h * 0.78f), 6f)

    // Bridges
    drawLine(primary, Offset(w * 0.22f, h * 0.20f), Offset(w * 0.22f, h * 0.78f), 5f)
    drawLine(Color(0xFFFFC857), Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.78f), 5f)
    drawLine(Color(0xFFFF5D73), Offset(w * 0.78f, h * 0.20f), Offset(w * 0.78f, h * 0.78f), 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))

    drawDiagramLabel(tm, "1: pMHC–TCR", w * 0.22f, h * 0.36f, primary, focus.contains("Signal 1"))
    drawDiagramLabel(tm, "2: CD28–B7", w * 0.50f, h * 0.50f, Color(0xFFFFC857), focus.contains("Signal 2"))
    drawDiagramLabel(tm, "CTLA-4 / PD-1", w * 0.78f, h * 0.36f, Color(0xFFFF5D73), focus.contains("CTLA-4"))
    drawDiagramLabel(tm, "3: Polarizing Cytokines", w * 0.50f, h * 0.80f, tertiary, focus.contains("Signal 3"))
}

private fun DrawScope.drawComplementDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    drawLine(primary, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.48f, h * 0.45f), 4f)
    drawLine(secondary, Offset(w * 0.48f, h * 0.45f), Offset(w * 0.26f, h * 0.76f), 4f)
    drawLine(Color(0xFFFF5D73), Offset(w * 0.48f, h * 0.45f), Offset(w * 0.76f, h * 0.66f), 4f)
    drawCircle(Color(0xFFFF5D73).copy(alpha = 0.25f), 28f, Offset(w * 0.78f, h * 0.66f))

    drawDiagramLabel(tm, "Classical / Lectin / Alt", w * 0.24f, h * 0.24f, primary, focus.contains("Triggers"))
    drawDiagramLabel(tm, "C3 Convertase → C3a+C3b", w * 0.50f, h * 0.45f, secondary, focus.contains("C3"))
    drawDiagramLabel(tm, "C3b Opsonization", w * 0.26f, h * 0.76f, Color(0xFF3DDC97), focus.contains("Opsonization"))
    drawDiagramLabel(tm, "C5b–C9 MAC Pore", w * 0.76f, h * 0.68f, Color(0xFFFF5D73), focus.contains("MAC"))
}

private fun DrawScope.drawVdjDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    // Germline DNA line
    drawLine(Color.White.copy(alpha = 0.45f), Offset(28f, h * 0.26f), Offset(w - 28f, h * 0.26f), 5f)
    drawRoundRect(primary, Offset(w * 0.10f, h * 0.21f), Size(46f, 20f), CornerRadius(4f, 4f))
    drawRoundRect(secondary, Offset(w * 0.34f, h * 0.21f), Size(38f, 20f), CornerRadius(4f, 4f))
    drawRoundRect(tertiary, Offset(w * 0.58f, h * 0.21f), Size(38f, 20f), CornerRadius(4f, 4f))

    // Rearrangement loop arrows
    drawLine(Color(0xFFFFC857), Offset(w * 0.42f, h * 0.34f), Offset(w * 0.50f, h * 0.64f), 4f)

    // Rearranged VDJ Exon
    drawLine(Color(0xFF3DDC97), Offset(w * 0.22f, h * 0.76f), Offset(w * 0.78f, h * 0.76f), 8f, cap = StrokeCap.Round)

    drawDiagramLabel(tm, "Germline V / D / J", w * 0.22f, h * 0.24f, primary, focus.contains("Germline"))
    drawDiagramLabel(tm, "RAG1/RAG2 Cut", w * 0.36f, h * 0.46f, Color(0xFFFFC857), focus.contains("RAG"))
    drawDiagramLabel(tm, "TdT N-Addition", w * 0.70f, h * 0.46f, secondary, focus.contains("TdT"))
    drawDiagramLabel(tm, "Fused VDJ–C Exon", w * 0.50f, h * 0.76f, Color(0xFF3DDC97), focus.contains("Exon"))
}

private fun DrawScope.drawGerminalCenterDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    // Germinal center oval split DZ / LZ
    drawOval(primary.copy(alpha = 0.14f), Offset(w * 0.08f, h * 0.18f), Size(w * 0.84f, h * 0.64f))
    drawOval(primary.copy(alpha = 0.55f), Offset(w * 0.08f, h * 0.18f), Size(w * 0.84f, h * 0.64f), style = Stroke(2.2f))
    drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.48f, h * 0.20f), Offset(w * 0.48f, h * 0.78f), 2f)

    drawDiagramLabel(tm, "Dark Zone (AID SHM)", w * 0.27f, h * 0.38f, primary, focus.contains("Dark Zone"))
    drawDiagramLabel(tm, "FDC Native Antigen", w * 0.70f, h * 0.28f, secondary, focus.contains("FDC"))
    drawDiagramLabel(tm, "Tfh CD40L Help", w * 0.70f, h * 0.54f, tertiary, focus.contains("Tfh"))
    drawDiagramLabel(tm, "Plasma / Memory B", w * 0.50f, h * 0.82f, Color(0xFF3DDC97), focus.contains("Plasma"))
}

private fun DrawScope.drawJakStatDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    drawLine(Color(0xFF3DDC97).copy(alpha = 0.5f), Offset(24f, h * 0.25f), Offset(w - 24f, h * 0.25f), 5f)
    drawLine(primary, Offset(w * 0.44f, h * 0.18f), Offset(w * 0.44f, h * 0.44f), 8f)
    drawLine(primary, Offset(w * 0.56f, h * 0.18f), Offset(w * 0.56f, h * 0.44f), 8f)
    drawLine(Color(0xFFFFC857), Offset(w * 0.50f, h * 0.44f), Offset(w * 0.50f, h * 0.82f), 3.5f)
    drawOval(secondary.copy(alpha = 0.20f), Offset(w * 0.20f, h * 0.74f), Size(w * 0.60f, h * 0.22f))

    drawDiagramLabel(tm, "Cytokine Receptor", w * 0.28f, h * 0.24f, primary, focus.contains("Receptor"))
    drawDiagramLabel(tm, "JAK1/2 p-Tyr", w * 0.50f, h * 0.44f, Color(0xFFFFC857), focus.contains("JAK"))
    drawDiagramLabel(tm, "STAT Dimer (SH2)", w * 0.50f, h * 0.62f, secondary, focus.contains("STAT"))
    drawDiagramLabel(tm, "Nuclear GAS/ISRE", w * 0.50f, h * 0.84f, tertiary, focus.contains("Nuclear"))
}

private fun DrawScope.drawInflammasomeDiagram(
    tm: TextMeasurer,
    focus: String,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val w = size.width
    val h = size.height
    drawLine(Color(0xFF3DDC97).copy(alpha = 0.45f), Offset(24f, h * 0.22f), Offset(w - 24f, h * 0.22f), 5f)
    drawCircle(Color(0xFFFFC857).copy(alpha = 0.25f), 34f, Offset(w * 0.50f, h * 0.48f))
    drawLine(primary, Offset(w * 0.24f, h * 0.28f), Offset(w * 0.48f, h * 0.48f), 3.5f)
    drawLine(secondary, Offset(w * 0.50f, h * 0.48f), Offset(w * 0.28f, h * 0.76f), 3.5f)
    drawLine(Color(0xFFFF5D73), Offset(w * 0.50f, h * 0.48f), Offset(w * 0.76f, h * 0.74f), 3.5f)

    drawDiagramLabel(tm, "1. TLR4/NF-κB Prime", w * 0.24f, h * 0.28f, primary, focus.contains("TLR4"))
    drawDiagramLabel(tm, "2. NLRP3–ASC–Casp1", w * 0.52f, h * 0.48f, Color(0xFFFFC857), focus.contains("NLRP3"))
    drawDiagramLabel(tm, "IL-1β / IL-18 Out", w * 0.26f, h * 0.78f, secondary, focus.contains("IL-1"))
    drawDiagramLabel(tm, "GSDMD Pyroptosis Pore", w * 0.76f, h * 0.74f, Color(0xFFFF5D73), focus.contains("GSDMD"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlossaryContentView(
    bookmarks: List<BookmarkItem> = emptyList(),
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTermId by remember { mutableStateOf(ImmunologyGlossaryKb.TERMS.first().id) }

    val filtered = remember(searchQuery, selectedCategory) {
        ImmunologyGlossaryKb.filterTerms(searchQuery, selectedCategory)
    }

    val activeTerm = remember(selectedTermId, filtered) {
        filtered.firstOrNull { it.id == selectedTermId }
            ?: ImmunologyGlossaryKb.byId(selectedTermId)
            ?: ImmunologyGlossaryKb.TERMS.first()
    }

    var focusedDiagramPart by remember(activeTerm.id) { mutableStateOf(activeTerm.defaultFocusedPart) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Interactive Immunology Glossary", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                EvidencePill("${filtered.size} / ${ImmunologyGlossaryKb.TERMS.size} terms")
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search term, acronym, or mechanism") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear glossary search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("glossary_search_input")
            )
            Spacer(Modifier.height(8.dp))
            PillSelectorRow(
                options = ImmunologyGlossaryKb.CATEGORIES,
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
                tagPrefix = "glossary_cat"
            )
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text(
                    "No glossary concepts match \"$searchQuery\" in $selectedCategory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.mutedText
                )
            } else {
                Text("Select Concept:", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filtered.forEach { term ->
                        val isSel = term.id == activeTerm.id
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTermId = term.id },
                            label = { Text(term.acronym, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("glossary_term_${term.id}")
                        )
                    }
                }
            }
        }

        // Expanded Interactive Definition + Visual Diagram Card
        val isTermSaved = bookmarks.any { it.itemId == activeTerm.id }
        GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(activeTerm.term, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("${activeTerm.acronym} • ${activeTerm.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvidencePill("Established")
                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                "glossary",
                                activeTerm.id,
                                "${activeTerm.term} (${activeTerm.acronym})",
                                activeTerm.category,
                                activeTerm.shortDefinition
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("bookmark_glossary_${activeTerm.id}")
                    ) {
                        Icon(
                            imageVector = if (isTermSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save immunology topic to My Library",
                            tint = if (isTermSaved) MaterialTheme.colorScheme.primary else semantic.mutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Definition", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(activeTerm.shortDefinition, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Molecular & Cellular Mechanism", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(activeTerm.detailedMechanism, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
            Spacer(Modifier.height(8.dp))
            Text("Clinical & Translational Relevance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Text(activeTerm.clinicalRelevance, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(10.dp))
            Text("Interactive Schematic Diagram (Tap Parts to Inspect)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            GlossaryDiagramCanvas(
                diagramType = activeTerm.diagramType,
                focusedPart = focusedDiagramPart,
                onSelectPart = { focusedDiagramPart = it }
            )

            val related = activeTerm.relatedTermIds.mapNotNull { ImmunologyGlossaryKb.byId(it) }
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Related Concepts (Tap to Jump):", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    related.forEach { rel ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedTermId = rel.id
                                searchQuery = ""
                                selectedCategory = "All"
                            },
                            label = { Text("${rel.acronym} (${rel.term.substringBefore(" (")})") },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }
            }
        }
    }
}
