package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.kb.ImmunologyKb
import com.example.data.model.AcademyProgress
import com.example.data.model.BookmarkItem
import com.example.data.model.ResearchProject
import com.example.data.model.SavedSimulation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.network.LiteraturePaper
import com.example.ui.components.AppRoute
import com.example.ui.components.DeveloperCreditsFooter
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbitalLogo3D
import com.example.ui.theme.LocalImmunoColors
import java.util.Calendar

private data class QuickModuleItem(
    val route: AppRoute,
    val title: String,
    val sub: String,
    val icon: ImageVector
)

@Composable
fun DashboardScreen(
    profile: UserProfile?,
    bookmarks: List<BookmarkItem>,
    simulations: List<SavedSimulation>,
    projects: List<ResearchProject>,
    academyProgress: List<AcademyProgress>,
    recentItems: List<String>,
    liveFeed: UiState<List<LiteraturePaper>>,
    onRefreshFeed: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSaveWidgets: (List<String>) -> Unit,
    onToggleBookmark: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    isSignedIn: Boolean = false,
    onRequestSignIn: () -> Unit = {},
    onToast: (String) -> Unit = {}
) {
    val semantic = LocalImmunoColors.current
    val reduceMotion = profile?.reduceMotion ?: false
    val hidden = profile?.hiddenModules ?: emptyList()
    val activeWidgets = profile?.dashboardWidgets?.ifEmpty { listOf("3d", "daily", "quick", "activity", "feed") }
        ?: listOf("3d", "daily", "quick", "activity", "feed")
    var showCustomize by remember { mutableStateOf(false) }

    val dayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % ImmunologyKb.DAILY.size }
    val daily = ImmunologyKb.DAILY[dayIndex]

    val modules = remember(hidden) {
        listOf(
            QuickModuleItem(AppRoute.ATLAS, "Immune Atlas", "9 organs • 15 cells 3D", Icons.Filled.Public),
            QuickModuleItem(AppRoute.CELLS, "Cellular Intel", "Organelles • 10 processes", Icons.Filled.Biotech),
            QuickModuleItem(AppRoute.MOLECULES, "Molecules & PDB", "15 mols • 11 pathways", Icons.Filled.Hub),
            QuickModuleItem(AppRoute.SIGNALING, "Signaling Canvas", "Dynamic pathways • Bio CSV", Icons.Filled.AccountTree),
            QuickModuleItem(AppRoute.SIMULATOR, "Immune Simulator", "ODE kinetics • 9 scenarios", Icons.Filled.Timeline),
            QuickModuleItem(AppRoute.PATHOGEN_LAB, "Pathogen & Host", "Design • Infect • Treat", Icons.Filled.Coronavirus),
            QuickModuleItem(AppRoute.BIO_STRUCTURES_3D, "3D RNA/DNA/AA", "Helix • Fold • 20 AAs • PDB", Icons.Filled.Hub),
            QuickModuleItem(AppRoute.PHARMACOLOGY, "Exp. Pharmacology", "Elucidation • 3D Mols • PD/PK", Icons.Filled.Medication),
            QuickModuleItem(AppRoute.DISEASES, "Diseases & HIV", "13 profiles + HIV lab", Icons.Filled.Coronavirus),
            QuickModuleItem(AppRoute.VIRTUAL_LAB, "Virtual Lab", "22 experiments + PDF", Icons.Filled.Science),
            QuickModuleItem(AppRoute.IMMUNO_AI, "Immuno AI", "Multi-model + Search", Icons.Filled.AutoAwesome),
            QuickModuleItem(AppRoute.WORKSPACE, "Workspace", "Projects • Notes • Logs", Icons.Filled.FolderShared),
            QuickModuleItem(AppRoute.GENOMICS, "Genomics & D3", "D3 Strains • NW/SW • UPGMA", Icons.Filled.Biotech),
            QuickModuleItem(AppRoute.PROTEOMICS, "Proteomics Suite", "UniProt • MS/MS • Volcano • 3D", Icons.Filled.Science),
            QuickModuleItem(AppRoute.LITERATURE, "Literature Hub", "6 APIs • BibTeX", Icons.AutoMirrored.Filled.MenuBook),
            QuickModuleItem(AppRoute.MY_LIBRARY, "My Library", "${bookmarks.size} saved items", Icons.Filled.CollectionsBookmark),
            QuickModuleItem(AppRoute.ACADEMY, "Academy", "10 tracks • Timed quizzes", Icons.Filled.School),
            QuickModuleItem(AppRoute.GLOSSARY, "Glossary & Diagrams", "18 terms • 8 schematics", Icons.AutoMirrored.Filled.MenuBook),
            QuickModuleItem(AppRoute.ANALYTICS, "Data Analytics", "CSV • OLS • Heatmaps", Icons.Filled.Analytics)
        ).filterNot { it.route.route in hidden }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Futuristic 3D Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, semantic.glassBorder, MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(R.drawable.img_immuno_hero),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${profile?.userCategory ?: "Researcher"} Command Center",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Welcome back, ${profile?.displayName?.substringBefore(" ") ?: "Scientist"}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Explore 3D immunology, live RCSB/Europe PMC feeds, ODE modeling & Gemini RAG.",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigate(AppRoute.ATLAS) },
                            modifier = Modifier.testTag("hero_launch_atlas_btn")
                        ) {
                            Text("3D Atlas")
                        }
                        OutlinedButton(
                            onClick = { showCustomize = !showCustomize },
                            modifier = Modifier.testTag("customize_widgets_btn")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Widgets")
                        }
                        if (!isSignedIn) {
                            OutlinedButton(
                                onClick = onRequestSignIn,
                                modifier = Modifier.testTag("hero_signin_btn")
                            ) {
                                Text("Sign in")
                            }
                        }
                    }
                }
                if ("3d" in activeWidgets) {
                    Spacer(Modifier.width(10.dp))
                    OrbitalLogo3D(reduceMotion = reduceMotion, size = 84.dp)
                }
            }
        }

        if (showCustomize) {
            GlassCard {
                Text("Customize Dashboard Widgets", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                val widgetDefs = listOf(
                    "3d" to "3D Orbital Emblem",
                    "daily" to "Daily Discovery",
                    "quick" to "Quick-Launch Grid",
                    "activity" to "Research & Progress",
                    "feed" to "Live Europe PMC Feed"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    widgetDefs.take(3).forEach { (k, label) ->
                        val active = k in activeWidgets
                        FilterChip(
                            selected = active,
                            onClick = {
                                val next = if (active) activeWidgets - k else activeWidgets + k
                                onSaveWidgets(next.ifEmpty { listOf("quick") })
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    widgetDefs.drop(3).forEach { (k, label) ->
                        val active = k in activeWidgets
                        FilterChip(
                            selected = active,
                            onClick = {
                                val next = if (active) activeWidgets - k else activeWidgets + k
                                onSaveWidgets(next.ifEmpty { listOf("quick") })
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if ("daily" in activeWidgets) {
            val dailyId = "topic_daily_${daily.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(32)}"
            val isDailySaved = bookmarks.any { it.itemId == dailyId }
            GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Scientific Discovery", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EvidencePill(daily.status)
                        IconButton(
                            onClick = { onToggleBookmark("topic", dailyId, daily.title, "Research Topic", daily.body) },
                            modifier = Modifier.size(48.dp).testTag("bookmark_daily_topic_btn")
                        ) {
                            Icon(
                                imageVector = if (isDailySaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Save daily topic to My Library",
                                tint = if (isDailySaved) MaterialTheme.colorScheme.primary else semantic.mutedText
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(daily.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(daily.body, style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Source: ${ImmunologyKb.REFS[daily.refId]?.citation ?: "Immunology KB"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if ("quick" in activeWidgets) {
            Text("Platform Modules", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            val rows = modules.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { item ->
                        GlassCard(
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tile_${item.route.route}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if ("activity" in activeWidgets) {
            val totalLessonsDone = academyProgress.sumOf { it.completedLessons.size }
            val progressFrac = (totalLessonsDone / 20f).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    onClick = { onNavigate(AppRoute.ACADEMY) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Academy Progress", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("$totalLessonsDone / 20 lessons", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { progressFrac }, modifier = Modifier.fillMaxWidth())
                }
                GlassCard(
                    onClick = { onNavigate(AppRoute.WORKSPACE) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Workspace & Runs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    Text("${projects.size} projects (${if (isSignedIn) "cloud" else "local"})", style = MaterialTheme.typography.titleMedium)
                    Text("${simulations.size} saved sims • ${bookmarks.size} bookmarks", style = MaterialTheme.typography.bodySmall, color = semantic.mutedText)
                }
            }

            if (bookmarks.isNotEmpty() || recentItems.isNotEmpty()) {
                GlassCard(onClick = { onNavigate(AppRoute.MY_LIBRARY) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("My Library & Recent", style = MaterialTheme.typography.titleSmall)
                        }
                        Text("Open Library →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    if (recentItems.isNotEmpty()) {
                        Text(
                            "Recently explored: ${recentItems.take(4).joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.mutedText
                        )
                    }
                    if (bookmarks.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Saved (${bookmarks.size}): ${bookmarks.take(4).joinToString(" • ") { it.title }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if ("feed" in activeWidgets) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Immunology Research (Europe PMC)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onRefreshFeed, modifier = Modifier.size(48.dp).testTag("refresh_feed_btn")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh live feed", tint = MaterialTheme.colorScheme.primary)
                }
            }
            when (liveFeed) {
                is UiState.Loading -> GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Fetching live biomedical literature from Europe PMC...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is UiState.Error -> GlassCard {
                    Text("Live feed unavailable: ${liveFeed.message}", style = MaterialTheme.typography.bodySmall, color = semantic.illustrative)
                    Text("Use Literature Hub to search PubMed, Europe PMC, Crossref & OpenAlex once online.", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                }
                is UiState.Success -> {
                    liveFeed.data.take(4).forEach { paper ->
                        val isPaperSaved = bookmarks.any { it.itemId == paper.id }
                        GlassCard(onClick = { onNavigate(AppRoute.LITERATURE) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(paper.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(2.dp))
                                    Text("${paper.authors} • ${paper.journal} (${paper.year})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = {
                                        val meta = listOfNotNull(paper.doi?.let { "DOI: $it" }, paper.url).joinToString(" • ").ifBlank { null }
                                        onToggleBookmark("paper", paper.id, paper.title, "${paper.journal} (${paper.year})", meta)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPaperSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Save article to My Library",
                                        tint = if (isPaperSaved) MaterialTheme.colorScheme.primary else semantic.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        DeveloperCreditsFooter(
            onCopied = onToast
        )
        Spacer(Modifier.height(24.dp))
    }
}
