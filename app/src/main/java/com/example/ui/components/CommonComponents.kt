package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.kb.DiseaseKb
import com.example.data.kb.ImmunologyGlossaryKb
import com.example.data.kb.ImmunologyKb
import com.example.data.model.AuditLogEntry
import com.example.domain.PharmacologyEngine
import com.example.domain.VirtualLabEngine
import com.example.ui.theme.LocalImmunoColors

enum class AppRoute(val route: String, val label: String, val shortLabel: String) {
    DASHBOARD("dashboard", "Dashboard", "Home"),
    ATLAS("atlas", "Immune Atlas", "Atlas"),
    CELLS("cells", "Cellular Intelligence", "Cells"),
    MOLECULES("molecules", "Molecules & Pathways", "Molecules"),
    SIGNALING("signaling", "Signaling Pathways", "Signaling"),
    SIMULATOR("simulator", "Immune Simulator", "Simulator"),
    PATHOGEN_LAB("pathogen_lab", "Pathogen & Host Lab", "Pathogen"),
    BIO_STRUCTURES_3D("bio3d", "RNA/DNA/AA/Protein 3D", "3D MolBio"),
    PHARMACOLOGY("pharmacology", "Experimental Pharmacology", "Pharma"),
    DISEASES("diseases", "Diseases & HIV", "Diseases"),
    VIRTUAL_LAB("lab", "Virtual Laboratory", "Lab"),
    IMMUNO_AI("ai", "Immuno AI", "AI"),
    WORKSPACE("workspace", "Research Workspace", "Workspace"),
    GENOMICS("genomics", "Genomics & Bioinfo", "Genomics"),
    PROTEOMICS("proteomics", "Proteomics Research", "Proteomics"),
    LITERATURE("literature", "Literature Hub", "Literature"),
    ACADEMY("academy", "Learning Academy", "Academy"),
    GLOSSARY("glossary", "Immunology Glossary", "Glossary"),
    MY_LIBRARY("my_library", "My Library", "Library"),
    ANALYTICS("analytics", "Data Analytics", "Analytics"),
    PROFILE_ADMIN("admin", "Profile & Admin", "Profile")
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val semantic = LocalImmunoColors.current
    val shape = MaterialTheme.shapes.medium
    val borderColor = accentColor?.copy(alpha = 0.45f) ?: semantic.glassBorder
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    semantic.glassBg,
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                )
            )
        )
        .border(1.dp, borderColor, shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .padding(14.dp)

    Column(modifier = baseModifier, content = content)
}

@Composable
fun EvidencePill(
    label: String,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val color = when {
        label.contains("Established", ignoreCase = true) -> semantic.established
        label.contains("Illustrative", ignoreCase = true) -> semantic.illustrative
        label.contains("Hypothesis", ignoreCase = true) || label.contains("Experimental", ignoreCase = true) -> semantic.hypothesis
        label.contains("Simulated", ignoreCase = true) -> semantic.simulated
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.48f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BannerDisclaimer(
    text: String,
    isSimulation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    val tone = if (isSimulation) semantic.simulated else semantic.illustrative
    Surface(
        shape = MaterialTheme.shapes.small,
        color = tone.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = tone
            )
        }
    }
}

@Composable
fun PillSelectorRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "pill"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            FilterChip(
                selected = isSel,
                onClick = { onSelect(opt) },
                label = { Text(opt, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("${tagPrefix}_${opt.lowercase().replace(Regex("[^a-z0-9]+"), "_")}")
            )
        }
    }
}

@Composable
fun ModuleHeader(
    title: String,
    subtitle: String,
    badge: String? = null,
    isBookmarked: Boolean? = null,
    onToggleBookmark: (() -> Unit)? = null
) {
    val semantic = LocalImmunoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) EvidencePill(badge)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = semantic.mutedText
            )
        }
        if (onToggleBookmark != null && isBookmarked != null) {
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_bookmark_btn")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = stringResource(R.string.cd_bookmark),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else semantic.mutedText
                )
            }
        }
    }
}

data class SearchHit(
    val title: String,
    val category: String,
    val route: AppRoute,
    val detail: String
)

@Composable
fun GlobalSearchDialog(
    onDismiss: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allHits = remember { buildPlatformSearchIndex() }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allHits.take(15)
        else allHits.filter {
            it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.detail.lowercase().contains(q)
        }.take(20)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Global Biomedical Search")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cells, PDB IDs, pathways, diseases, labs...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { hit ->
                        GlassCard(
                            onClick = {
                                onNavigate(hit.route)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(hit.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(hit.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(hit.detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CommandPaletteDialog(
    onDismiss: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val commands = remember {
        listOf(
            Triple("Launch 3D Immune Atlas", AppRoute.ATLAS, "Explore 9 organs & 15 cells"),
            Triple("Fetch Live RCSB PDB Structure", AppRoute.MOLECULES, "Inspect C-α backbones & ligands"),
            Triple("Open Dynamic Signaling Canvas", AppRoute.SIGNALING, "Live pathway cascades & bio CSV data"),
            Triple("Run Immune ODE Simulator", AppRoute.SIMULATOR, "9 scenarios, CSV & PNG export"),
            Triple("Open Categorized 3D Pathogen & Spike Dashboard", AppRoute.PATHOGEN_LAB, "6 Kingdoms, 3D Organelles, Variants/Strains & Spike-Receptor Comparator"),
            Triple("3D DNA, RNA, Amino Acids & Protein Studio", AppRoute.BIO_STRUCTURES_3D, "Helix geometry, RNA fold, 20 CPK amino acids & PDB"),
            Triple("Experimental Pharmacology & Labeled 3D Mols", AppRoute.PHARMACOLOGY, "Structure elucidation (IR/NMR/MS), labeled 3D & PD/PK"),
            Triple("Open HIV Immunology Research", AppRoute.DISEASES, "3D virion & latency reversal model"),
            Triple("Enter Virtual Laboratory (22 Labs)", AppRoute.VIRTUAL_LAB, "ELISA, Flow, PCR, Gram stain"),
            Triple("Ask Immuno AI Assistant", AppRoute.IMMUNO_AI, "Multi-model Gemini + live Web & PMC search"),
            Triple("D3.js Genomic Strains / FASTA / Phylogeny", AppRoute.GENOMICS, "D3 strain viz, ORFs, NW/SW, UPGMA, SNVs"),
            Triple("Open Proteomics Research Suite", AppRoute.PROTEOMICS, "UniProtKB, Verified PDB 3D, ProtParam, Trypsin MS1, MS/MS b/y, Volcano"),
            Triple("Search PubMed / Europe PMC / Crossref", AppRoute.LITERATURE, "Scholarly papers & BibTeX"),
            Triple("Open Interactive Immunology Glossary", AppRoute.GLOSSARY, "18 definitions & 8 interactive schematic diagrams"),
            Triple("Open Research Workspace", AppRoute.WORKSPACE, "Notes, hypotheses, logs, PDF export"),
            Triple("System Health & Live API Status", AppRoute.PROFILE_ADMIN, "Admin telemetry & feature flags")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Command Palette")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(commands) { (title, route, sub) ->
                    GlassCard(onClick = {
                        onNavigate(route)
                        onDismiss()
                    }) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(sub, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun NotificationsDialog(
    logs: List<AuditLogEntry>,
    onDismiss: () -> Unit
) {
    val semantic = LocalImmunoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification & Activity Center") },
        text = {
            if (logs.isEmpty()) {
                Text("No recorded activity yet. Explore modules, save simulations, or create workspace entries.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs.take(15)) { entry ->
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.module, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(entry.level.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (entry.level == "error") semantic.critical else semantic.established)
                            }
                            Text(entry.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun DeveloperCreditsFooter(
    modifier: Modifier = Modifier,
    onCopied: (String) -> Unit = {}
) {
    val semantic = LocalImmunoColors.current
    val clipboard = LocalClipboardManager.current

    GlassCard(
        accentColor = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.testTag("developer_credits_footer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Credits and Donations",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Credits & Support",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            EvidencePill("ImmunoVerse 2.0")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Developer: Byron Oyoo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Support continued open biomedical research & simulation tools via M-Pesa / KCB Bank Donations:",
            style = MaterialTheme.typography.bodySmall,
            color = semantic.mutedText
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Paybill: 522522",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "KCB Account Number: 1355643775",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString("522522"))
                            onCopied("Copied Paybill: 522522")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("copy_paybill_btn")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Paybill", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString("1355643775"))
                            onCopied("Copied KCB Account: 1355643775")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("copy_kcb_acc_btn")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Account", maxLines = 1)
                    }
                }
            }
        }
    }
}

