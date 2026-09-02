package com.darkmodestudio.commandcenter.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.darkmodestudio.commandcenter.core.agent.DesktopHostBridge
import com.darkmodestudio.commandcenter.core.auth.ConnectAuthCoordinator
import com.darkmodestudio.commandcenter.core.auth.ConnectAuthState
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.RepositoryFilesRepository
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsBottomNavigation
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.sync.SyncCoordinator
import com.darkmodestudio.commandcenter.core.sync.SyncMode
import com.darkmodestudio.commandcenter.feature.agents.AgentsScreen
import com.darkmodestudio.commandcenter.feature.connectstack.ConnectStackScreen
import com.darkmodestudio.commandcenter.feature.execution.ExecutionScreen
import com.darkmodestudio.commandcenter.feature.health.PlatformHealthScreen
import com.darkmodestudio.commandcenter.feature.home.HomeScreen
import com.darkmodestudio.commandcenter.feature.projectdetail.ProjectDetailScreen
import com.darkmodestudio.commandcenter.feature.projects.ProjectsScreen
import com.darkmodestudio.commandcenter.feature.settings.SettingsScreen
import com.darkmodestudio.commandcenter.feature.sheets.ActionType
import com.darkmodestudio.commandcenter.feature.sheets.ConnectServiceSheet
import com.darkmodestudio.commandcenter.feature.sheets.CreateAutomationSheet
import com.darkmodestudio.commandcenter.feature.sheets.CreateProjectSheet
import com.darkmodestudio.commandcenter.feature.sheets.CreateReminderSheet
import com.darkmodestudio.commandcenter.feature.sheets.CreateTaskSheet
import com.darkmodestudio.commandcenter.feature.sheets.GlobalActionSheet
import com.darkmodestudio.commandcenter.feature.sheets.ManageAgentsSheet
import com.darkmodestudio.commandcenter.feature.sheets.PairDesktopHostSheet
import com.darkmodestudio.commandcenter.feature.updates.UpdatesScreen
import kotlinx.coroutines.launch

@Composable
fun DmsNavHost(
    navController: NavHostController,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    agentRepository: AgentRepository,
    healthRepository: HealthRepository,
    notificationRepository: NotificationRepository,
    settingsRepository: SettingsRepository,
    repositoryFilesRepository: RepositoryFilesRepository? = null,
    keystoreCredentialManager: KeystoreCredentialManager,
    syncCoordinator: SyncCoordinator,
    connectAuthCoordinator: ConnectAuthCoordinator? = null,
    desktopHostBridge: DesktopHostBridge? = null,
    database: DmsDatabase? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // Ensure database, coordinator, and bridge instances
    val db = database ?: remember { DmsDatabase.getInstance(context) }
    val authCoordinator = connectAuthCoordinator ?: remember {
        ConnectAuthCoordinator(db, keystoreCredentialManager, syncCoordinator = syncCoordinator)
    }
    val hostBridge = desktopHostBridge ?: remember {
        DesktopHostBridge(db, keystoreCredentialManager)
    }

    // Sheet visibility states & provider preservation
    var showGlobalActionSheet by remember { mutableStateOf(false) }
    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var showCreateProjectSheet by remember { mutableStateOf(false) }
    var showCreateReminderSheet by remember { mutableStateOf(false) }
    var showConnectServiceSheet by remember { mutableStateOf(false) }
    var showCreateAutomationSheet by remember { mutableStateOf(false) }
    var showManageAgentsSheet by remember { mutableStateOf(false) }
    var showDesktopPairingSheet by remember { mutableStateOf(false) }
    var selectedConnectProviderId by remember { mutableStateOf<String?>(null) }

    val agentsList by agentRepository.agents.collectAsState(initial = emptyList())
    val activeHost by db.desktopHostDao().getHostFlow("primary_desktop").collectAsState(initial = null)

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Projects.route,
        Screen.Execution.route,
        Screen.Updates.route,
        Screen.Agents.route,
        Screen.PlatformHealth.route
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(180)) + slideInVertically(
                    animationSpec = tween(180),
                    initialOffsetY = { 20 }
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { 20 }
                )
            }
        ) {
            // Screen 01: Home / Command Center
            composable(Screen.Home.route) {
                HomeScreen(
                    projectRepository = projectRepository,
                    taskRepository = taskRepository,
                    agentRepository = agentRepository,
                    healthRepository = healthRepository,
                    onNavigateToProject = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToAgents = { navController.navigate(Screen.Agents.route) },
                    onNavigateToHealth = { navController.navigate(Screen.PlatformHealth.route) },
                    onNavigateToExecution = { navController.navigate(Screen.Execution.route) },
                    onNavigateToUpdates = { navController.navigate(Screen.Updates.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onManualSync = {
                        coroutineScope.launch {
                            syncCoordinator.syncAll(SyncMode.MANUAL)
                        }
                    }
                )
            }

            // Screen 02: Connect Your Stack
            composable(Screen.ConnectStack.route) {
                ConnectStackScreen(
                    healthRepository = healthRepository,
                    notificationRepository = notificationRepository,
                    onContinueClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onConnectServiceClick = { providerId ->
                        selectedConnectProviderId = providerId
                        showConnectServiceSheet = true
                    }
                )
            }

            // Screen 03: Projects
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    projectRepository = projectRepository,
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onNavigateToUpdates = { navController.navigate(Screen.Updates.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 04: Project Detail
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: "secondme"
                ProjectDetailScreen(
                    projectId = projectId,
                    projectRepository = projectRepository,
                    taskRepository = taskRepository,
                    repositoryFilesRepository = repositoryFilesRepository,
                    onConnectGitHubClick = {
                        selectedConnectProviderId = "github"
                        showConnectServiceSheet = true
                    },
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 05: Agents
            composable(Screen.Agents.route) {
                AgentsScreen(
                    agentRepository = agentRepository,
                    onManageAgentsClick = { showManageAgentsSheet = true },
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 06: Platform Health
            composable(Screen.PlatformHealth.route) {
                PlatformHealthScreen(
                    healthRepository = healthRepository,
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) },
                    onConnectServiceClick = {
                        selectedConnectProviderId = null
                        showConnectServiceSheet = true
                    },
                    onSyncNowClick = {
                        coroutineScope.launch {
                            syncCoordinator.syncAll(SyncMode.MANUAL)
                        }
                    }
                )
            }

            // Screen 07: Execution
            composable(Screen.Execution.route) {
                ExecutionScreen(
                    taskRepository = taskRepository,
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) },
                    onAddTaskClick = { showCreateTaskSheet = true }
                )
            }

            // Screen 08: Updates
            composable(Screen.Updates.route) {
                UpdatesScreen(
                    notificationRepository = notificationRepository,
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 09: Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    onBackClick = { navController.popBackStack() },
                    onManageAutomationsClick = { showCreateAutomationSheet = true },
                    onConnectServiceClick = {
                        selectedConnectProviderId = null
                        showConnectServiceSheet = true
                    }
                )
            }
        }

        // Floating Bottom Navigation
        if (showBottomBar) {
            DmsBottomNavigation(
                selectedRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateClick = {
                    showGlobalActionSheet = true
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Global Action Sheet
        if (showGlobalActionSheet) {
            GlobalActionSheet(
                currentRoute = currentRoute,
                onDismissRequest = { showGlobalActionSheet = false },
                onSelectAction = { action ->
                    showGlobalActionSheet = false
                    when (action) {
                        ActionType.NEW_TASK -> showCreateTaskSheet = true
                        ActionType.NEW_PROJECT -> showCreateProjectSheet = true
                        ActionType.NEW_REMINDER -> showCreateReminderSheet = true
                        ActionType.CONNECT_SERVICE -> {
                            selectedConnectProviderId = null
                            showConnectServiceSheet = true
                        }
                        ActionType.NEW_AUTOMATION -> showCreateAutomationSheet = true
                    }
                }
            )
        }

        // Manage Agents Modal Sheet
        if (showManageAgentsSheet) {
            ManageAgentsSheet(
                agents = agentsList,
                isHostOnline = activeHost?.isOnline ?: false,
                pairedHostName = activeHost?.hostName,
                onDismiss = { showManageAgentsSheet = false },
                onRefreshQuotas = {
                    coroutineScope.launch {
                        syncCoordinator.syncAll(SyncMode.MANUAL)
                    }
                },
                onPairDesktopHost = {
                    showManageAgentsSheet = false
                    showDesktopPairingSheet = true
                }
            )
        }

        // Desktop Host Pairing Sheet
        if (showDesktopPairingSheet) {
            PairDesktopHostSheet(
                desktopHostBridge = hostBridge,
                onDismiss = { showDesktopPairingSheet = false },
                onPairSuccess = { hostName ->
                    coroutineScope.launch {
                        syncCoordinator.syncAll(SyncMode.MANUAL)
                    }
                }
            )
        }

        // Modal Sheets for CRUD
        if (showCreateTaskSheet) {
            CreateTaskSheet(
                onDismissRequest = { showCreateTaskSheet = false },
                onSubmit = { title, desc, projId, projName, priority, agent, dueTime ->
                    coroutineScope.launch {
                        taskRepository.createTask(title, desc, projId, projName, priority, agent, dueTime)
                    }
                }
            )
        }

        if (showCreateProjectSheet) {
            CreateProjectSheet(
                onDismissRequest = { showCreateProjectSheet = false },
                onSubmit = { name, desc, icon, status, due, milestone, isMvp ->
                    coroutineScope.launch {
                        projectRepository.createProject(name, desc, icon, status, due, milestone, isMvp)
                    }
                }
            )
        }

        if (showCreateReminderSheet) {
            CreateReminderSheet(
                onDismissRequest = { showCreateReminderSheet = false },
                onSubmit = { title, dueText ->
                    coroutineScope.launch {
                        notificationRepository.createReminder(title, dueText)
                    }
                }
            )
        }

        if (showConnectServiceSheet) {
            val authState by authCoordinator.authState.collectAsState()
            val errorMsg = (authState as? ConnectAuthState.Error)?.message

            ConnectServiceSheet(
                initialProviderId = selectedConnectProviderId,
                errorMessage = errorMsg,
                onDismissRequest = {
                    showConnectServiceSheet = false
                    selectedConnectProviderId = null
                    authCoordinator.clearError()
                },
                onLaunchOAuth = { providerId ->
                    authCoordinator.startOAuthFlow(context, providerId)
                },
                onLaunchDesktopPairing = {
                    showConnectServiceSheet = false
                    showDesktopPairingSheet = true
                },
                onSaveTokenFallback = { provider, token, alias ->
                    coroutineScope.launch {
                        val key = "token_" + provider.name.lowercase()
                        keystoreCredentialManager.saveSecret(key, token)
                        syncCoordinator.syncAll(SyncMode.MANUAL)
                    }
                }
            )
        }

        if (showCreateAutomationSheet) {
            CreateAutomationSheet(
                onDismissRequest = { showCreateAutomationSheet = false },
                onSubmit = { name, trigger, action, humanText ->
                    coroutineScope.launch {
                        settingsRepository.createAutomationRule(name, trigger, null, null, action, humanText)
                    }
                }
            )
        }
    }
}
