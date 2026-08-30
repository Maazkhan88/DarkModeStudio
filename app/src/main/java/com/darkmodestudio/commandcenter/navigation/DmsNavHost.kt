package com.darkmodestudio.commandcenter.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsBottomNavigation
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.feature.agents.AgentsScreen
import com.darkmodestudio.commandcenter.feature.connectstack.ConnectStackScreen
import com.darkmodestudio.commandcenter.feature.execution.ExecutionScreen
import com.darkmodestudio.commandcenter.feature.health.PlatformHealthScreen
import com.darkmodestudio.commandcenter.feature.home.HomeScreen
import com.darkmodestudio.commandcenter.feature.projectdetail.ProjectDetailScreen
import com.darkmodestudio.commandcenter.feature.projects.ProjectsScreen
import com.darkmodestudio.commandcenter.feature.settings.SettingsScreen
import com.darkmodestudio.commandcenter.feature.updates.UpdatesScreen

@Composable
fun DmsNavHost(
    navController: NavHostController,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    agentRepository: AgentRepository,
    healthRepository: HealthRepository,
    notificationRepository: NotificationRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

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
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 02: Connect Your Stack
            composable(Screen.ConnectStack.route) {
                ConnectStackScreen(
                    onContinueClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
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
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 05: Agents
            composable(Screen.Agents.route) {
                AgentsScreen(
                    agentRepository = agentRepository,
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 06: Platform Health
            composable(Screen.PlatformHealth.route) {
                PlatformHealthScreen(
                    healthRepository = healthRepository,
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Screen 07: Execution
            composable(Screen.Execution.route) {
                ExecutionScreen(
                    taskRepository = taskRepository,
                    onNotificationClick = { navController.navigate(Screen.Updates.route) },
                    onAvatarClick = { navController.navigate(Screen.Settings.route) }
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
                    onBackClick = { navController.popBackStack() }
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
                    navController.navigate(Screen.ConnectStack.route)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
