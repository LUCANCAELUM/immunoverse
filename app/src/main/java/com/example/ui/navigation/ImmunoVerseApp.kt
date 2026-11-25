package com.example.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.ui.components.AppRoute
import com.example.ui.components.CommandPaletteDialog
import com.example.ui.components.DeveloperCreditsFooter
import com.example.ui.components.GlassCard
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.NotificationsCenterDialog
import com.example.ui.components.OrbitalLogo3D
import com.example.ui.components.PlatformTopSearchBar
import com.example.ui.screens.AcademyScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AtlasScreen
import com.example.ui.screens.BioStructures3DScreen
import com.example.ui.screens.CellsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiseasesScreen
import com.example.ui.screens.GenomicsScreen
import com.example.ui.screens.GlossaryScreen
import com.example.ui.screens.ImmunoAiScreen
import com.example.ui.screens.LiteratureScreen
import com.example.ui.screens.MoleculesScreen
import com.example.ui.screens.MyLibraryScreen
import com.example.ui.screens.PathogenLabScreen
import com.example.ui.screens.PharmacologyScreen
import com.example.ui.screens.ProfileAdminScreen
import com.example.ui.screens.ProteomicsScreen
import com.example.ui.screens.SignalingScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.VirtualLabScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.viewmodel.ImmunoAiViewModel
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmunoVerseApp(
    mainVm: MainViewModel,
    aiVm: ImmunoAiViewModel,
    isSignedIn: Boolean = false,
    onRequestSignIn: () -> Unit = {}
) {
    val profileState by mainVm.profileState.collectAsStateWithLifecycle()
    val bookmarksState by mainVm.bookmarksState.collectAsStateWithLifecycle()
    val simulationsState by mainVm.simulationsState.collectAsStateWithLifecycle()
    val projectsState by mainVm.projectsState.collectAsStateWithLifecycle()
    val progressState by mainVm.academyProgressState.collectAsStateWithLifecycle()
    val customState by mainVm.customContentState.collectAsStateWithLifecycle()
    val logsState by mainVm.auditLogsState.collectAsStateWithLifecycle()
    val entriesState by mainVm.activeProjectEntries.collectAsStateWithLifecycle()
    val recentItems by mainVm.recentItems.collectAsStateWithLifecycle()
    val liveFeed by mainVm.liveFeed.collectAsStateWithLifecycle()
    val selectedProjectId by mainVm.selectedProjectId.collectAsStateWithLifecycle()
    val sharedCsv by mainVm.sharedAnalyticsCsv.collectAsStateWithLifecycle()
    val localProjectCount by mainVm.localProjectCount.collectAsStateWithLifecycle()
    val savedInterests by mainVm.savedInterests.collectAsStateWithLifecycle()
    val effectiveInterests by mainVm.effectiveInterests.collectAsStateWithLifecycle()
    val readAlertIds by mainVm.readAlertIds.collectAsStateWithLifecycle()
    val researchAlerts by mainVm.researchAlerts.collectAsStateWithLifecycle()
    val unreadAlertCount = remember(researchAlerts, readAlertIds) {
        researchAlerts.count { it.id !in readAlertIds }
    }

    val profile = (profileState as? UiState.Success)?.data
    val bookmarks = (bookmarksState as? UiState.Success)?.data ?: emptyList()
    val simulations = (simulationsState as? UiState.Success)?.data ?: emptyList()
    val projects = (projectsState as? UiState.Success)?.data ?: emptyList()
    val academyProgress = (progressState as? UiState.Success)?.data ?: emptyList()
    val customContent = (customState as? UiState.Success)?.data ?: emptyList()
    val auditLogs = (logsState as? UiState.Success)?.data ?: emptyList()
    val projectEntries = (entriesState as? UiState.Success)?.data ?: emptyList()

    val reduceMotion = profile?.reduceMotion ?: false
    var currentRoute by remember { mutableStateOf(AppRoute.DASHBOARD) }
    var showSearch by remember { mutableStateOf(false) }
    var showCommands by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mainVm) {
        mainVm.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navigateTo: (AppRoute) -> Unit = { r ->
        currentRoute = r
        mainVm.recordRecent(r.label)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { currentRoute = AppRoute.DASHBOARD }
                        ) {
                            OrbitalLogo3D(reduceMotion = reduceMotion, size = 32.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentRoute.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ImmunoVerse 2.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }, modifier = Modifier.size(48.dp).testTag("topbar_search_btn")) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = { showCommands = true }, modifier = Modifier.size(48.dp).testTag("topbar_cmd_btn")) {
                            Icon(Icons.Filled.Terminal, contentDescription = stringResource(R.string.cd_commands))
                        }
                        IconButton(onClick = { showNotifications = true }, modifier = Modifier.size(48.dp).testTag("topbar_notif_btn")) {
                            BadgedBox(
                                badge = {
                                    if (unreadAlertCount > 0) {
                                        Badge { Text(if (unreadAlertCount > 9) "9+" else "$unreadAlertCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.cd_notifications))
                            }
                        }
                        IconButton(onClick = { currentRoute = AppRoute.PROFILE_ADMIN }, modifier = Modifier.size(48.dp).testTag("topbar_profile_btn")) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = stringResource(R.string.cd_profile), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        val primaryTabs = listOf(
                            Triple(AppRoute.DASHBOARD, "Home", Icons.Filled.Home),
                            Triple(AppRoute.ATLAS, "Atlas", Icons.Filled.Public),
                            Triple(AppRoute.SIMULATOR, "Sim", Icons.Filled.Timeline),
                            Triple(AppRoute.IMMUNO_AI, "AI", Icons.Filled.AutoAwesome)
                        )
                        primaryTabs.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                selected = currentRoute == route,
                                onClick = { navigateTo(route) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${route.route}")
                            )
                        }
                        NavigationBarItem(
                            selected = currentRoute !in primaryTabs.map { it.first },
                            onClick = { showMoreSheet = true },
                            icon = { Icon(Icons.Filled.Apps, contentDescription = "More modules") },
                            label = { Text("Modules", maxLines = 1) },
                            modifier = Modifier.testTag("nav_more_modules")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        AppRoute.entries.forEach { route ->
                            NavigationRailItem(
                                selected = currentRoute == route,
                                onClick = { navigateTo(route) },
                                icon = { Icon(Icons.Filled.Apps, contentDescription = route.label, modifier = Modifier.size(18.dp)) },
                                label = { Text(route.shortLabel, maxLines = 1) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    PlatformTopSearchBar(onNavigate = navigateTo, libraryCount = bookmarks.size)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentRoute) {
                        AppRoute.DASHBOARD -> DashboardScreen(
                            profile = profile,
                            bookmarks = bookmarks,
                            simulations = simulations,
                            projects = projects,
                            academyProgress = academyProgress,
                            recentItems = recentItems,
                            liveFeed = liveFeed,
                            onRefreshFeed = { mainVm.refreshLiveFeed() },
                            onNavigate = navigateTo,
                            onSaveWidgets = { widgets -> mainVm.updatePreferences(dashboardWidgets = widgets) },
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            isSignedIn = isSignedIn,
                            onRequestSignIn = onRequestSignIn,
                            onToast = { mainVm.emitToast(it) }
                        )
                        AppRoute.ATLAS -> AtlasScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub -> mainVm.toggleBookmark(t, id, title, sub) },
                            onRecordRecent = { mainVm.recordRecent(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.CELLS -> CellsScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.MOLECULES -> MoleculesScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub -> mainVm.toggleBookmark(t, id, title, sub) },
                            onRecordRecent = { mainVm.recordRecent(it) },
                            onOpenSignaling = { navigateTo(AppRoute.SIGNALING) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.SIGNALING -> SignalingScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub -> mainVm.toggleBookmark(t, id, title, sub) },
                            onRecordRecent = { mainVm.recordRecent(it) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.SIMULATOR -> SimulatorScreen(
                            reduceMotion = reduceMotion,
                            savedSimulations = simulations,
                            onSaveSimulation = { mainVm.saveSimulation(it) },
                            onDeleteSimulation = { mainVm.deleteSimulation(it) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.PATHOGEN_LAB -> PathogenLabScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            onOpenD3Genomics = { navigateTo(AppRoute.GENOMICS) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.BIO_STRUCTURES_3D -> BioStructures3DScreen(
                            reduceMotion = reduceMotion,
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.PHARMACOLOGY -> PharmacologyScreen(
                            reduceMotion = reduceMotion,
                            onOpenBio3D = { navigateTo(AppRoute.BIO_STRUCTURES_3D) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.DISEASES -> DiseasesScreen(
                            reduceMotion = reduceMotion,
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub -> mainVm.toggleBookmark(t, id, title, sub) },
                            onRecordRecent = { mainVm.recordRecent(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.VIRTUAL_LAB -> VirtualLabScreen(
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub -> mainVm.toggleBookmark(t, id, title, sub) },
                            onRecordRecent = { mainVm.recordRecent(it) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.IMMUNO_AI -> ImmunoAiScreen(
                            aiVm = aiVm,
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.WORKSPACE -> WorkspaceScreen(
                            projects = projects,
                            selectedProjectId = selectedProjectId,
                            entries = projectEntries,
                            bookmarks = bookmarks,
                            onSelectProject = { mainVm.selectProject(it) },
                            onCreateProject = { n, d, f, c -> mainVm.createProject(n, d, f, c) },
                            onDeleteProject = { mainVm.deleteProject(it) },
                            onAddEntry = { pid, type, t, c, s, tag -> mainVm.addProjectEntry(pid, type, t, c, s, tag) },
                            onUpdateEntry = { pid, entry, t, c, s -> mainVm.updateProjectEntry(pid, entry, t, c, s) },
                            onDeleteEntry = { pid, eid -> mainVm.deleteProjectEntry(pid, eid) },
                            onSendToAnalytics = { csv ->
                                mainVm.sendCsvToAnalytics(csv)
                                currentRoute = AppRoute.ANALYTICS
                                mainVm.emitToast("Loaded dataset into Data Analytics")
                            },
                            onToast = { mainVm.emitToast(it) },
                            isSignedIn = isSignedIn,
                            localProjectCount = localProjectCount,
                            onSyncLocalToCloud = { mainVm.syncLocalProjectsToCloud() },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.GENOMICS -> GenomicsScreen(
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.PROTEOMICS -> ProteomicsScreen(
                            reduceMotion = reduceMotion,
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.LITERATURE -> LiteratureScreen(
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.ACADEMY -> AcademyScreen(
                            progressList = academyProgress,
                            customContent = customContent,
                            onMarkLessonDone = { tr, les, existing -> mainVm.markLessonCompleted(tr, les, existing) },
                            onRecordQuizScore = { tr, sc, existing -> mainVm.recordQuizScore(tr, sc, existing) },
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.GLOSSARY -> GlossaryScreen(
                            bookmarks = bookmarks,
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.MY_LIBRARY -> MyLibraryScreen(
                            bookmarks = bookmarks,
                            savedInterests = savedInterests,
                            onToggleBookmark = { t, id, title, sub, meta -> mainVm.toggleBookmark(t, id, title, sub, meta) },
                            onToggleInterest = { mainVm.toggleResearchInterest(it) },
                            onNavigate = navigateTo,
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.ANALYTICS -> AnalyticsScreen(
                            sharedCsv = sharedCsv,
                            onToast = { mainVm.emitToast(it) },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                        AppRoute.PROFILE_ADMIN -> ProfileAdminScreen(
                            profile = profile,
                            bookmarks = bookmarks,
                            simulations = simulations,
                            projects = projects,
                            progress = academyProgress,
                            customContent = customContent,
                            auditLogs = auditLogs,
                            backupJsonBuilder = { mainVm.repo.buildBackupJson(profile, bookmarks, simulations, projects, academyProgress) },
                            onUpdatePreferences = { cat, rm, hm, dn -> mainVm.updatePreferences(cat, rm, hm, null, dn) },
                            onAddCustomContent = { mainVm.addCustomContent(it) },
                            onDeleteCustomContent = { mainVm.deleteCustomContent(it) },
                            onToast = { mainVm.emitToast(it) },
                            isSignedIn = isSignedIn,
                            localProjectCount = localProjectCount,
                            onRequestSignIn = onRequestSignIn,
                            onSyncLocalProjects = { mainVm.syncLocalProjectsToCloud() },
                            onBack = { currentRoute = AppRoute.DASHBOARD }
                        )
                    }
                }
                }
            }
        }
    }

    if (showSearch) {
        GlobalSearchDialog(onDismiss = { showSearch = false }, onNavigate = navigateTo)
    }
    if (showCommands) {
        CommandPaletteDialog(onDismiss = { showCommands = false }, onNavigate = navigateTo)
    }
    if (showNotifications) {
        NotificationsCenterDialog(
            alerts = researchAlerts,
            readAlertIds = readAlertIds,
            savedInterests = savedInterests,
            effectiveInterests = effectiveInterests,
            logs = auditLogs,
            onToggleInterest = { mainVm.toggleResearchInterest(it) },
            onRefreshAlerts = { mainVm.refreshInterestAlerts(silent = false) },
            onMarkAlertRead = { mainVm.markAlertRead(it) },
            onMarkAllRead = { mainVm.markAllAlertsRead() },
            onSavePaperBookmark = { alert ->
                val alertMeta = listOfNotNull(
                    alert.doi?.let { "DOI: $it" },
                    alert.url,
                    alert.summary.take(120)
                ).joinToString(" • ").ifBlank { null }
                mainVm.toggleBookmark("Literature Alert", alert.id, alert.title, alert.matchedInterest, alertMeta)
            },
            onNavigate = navigateTo,
            onDismiss = { showNotifications = false }
        )
    }
    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("All ImmunoVerse Modules", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                AppRoute.entries.forEach { route ->
                    GlassCard(
                        onClick = {
                            navigateTo(route)
                            showMoreSheet = false
                        },
                        modifier = Modifier.testTag("sheet_nav_${route.route}")
                    ) {
                        Text(route.label, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(6.dp))
                DeveloperCreditsFooter(onCopied = { mainVm.emitToast(it) })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
