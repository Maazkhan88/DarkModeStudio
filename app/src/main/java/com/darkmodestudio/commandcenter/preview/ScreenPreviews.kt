package com.darkmodestudio.commandcenter.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsBottomNavigation
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsOrbitalCanvas
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsUsageLineGraph
import com.darkmodestudio.commandcenter.core.designsystem.theme.DarkModeStudioTheme
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

@Preview(name = "01 - Command Center", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewHomeScreen() {
    DarkModeStudioTheme {
        HomeScreen(
            projectRepository = remember { ProjectRepository() },
            taskRepository = remember { TaskRepository() },
            agentRepository = remember { AgentRepository() },
            healthRepository = remember { HealthRepository() },
            onNavigateToProject = {},
            onNavigateToProjects = {},
            onNavigateToAgents = {},
            onNavigateToHealth = {},
            onNavigateToExecution = {},
            onNavigateToUpdates = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "02 - Connect Stack", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewConnectStackScreen() {
    DarkModeStudioTheme {
        ConnectStackScreen(
            healthRepository = remember { HealthRepository() },
            notificationRepository = remember { NotificationRepository() },
            onContinueClick = {}
        )
    }
}

@Preview(name = "03 - Projects", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewProjectsScreen() {
    DarkModeStudioTheme {
        ProjectsScreen(
            projectRepository = remember { ProjectRepository() },
            onProjectClick = {},
            onNavigateToUpdates = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "04 - Project Detail", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewProjectDetailScreen() {
    DarkModeStudioTheme {
        ProjectDetailScreen(
            projectId = "secondme",
            projectRepository = remember { ProjectRepository() },
            onBackClick = {},
            onNotificationClick = {},
            onAvatarClick = {}
        )
    }
}

@Preview(name = "05 - Agents", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewAgentsScreen() {
    DarkModeStudioTheme {
        AgentsScreen(
            agentRepository = remember { AgentRepository() },
            onNotificationClick = {},
            onAvatarClick = {}
        )
    }
}

@Preview(name = "06 - Platform Health", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewPlatformHealthScreen() {
    DarkModeStudioTheme {
        PlatformHealthScreen(
            healthRepository = remember { HealthRepository() },
            onNotificationClick = {},
            onAvatarClick = {}
        )
    }
}

@Preview(name = "07 - Execution", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewExecutionScreen() {
    DarkModeStudioTheme {
        ExecutionScreen(
            taskRepository = remember { TaskRepository() },
            onNotificationClick = {},
            onAvatarClick = {}
        )
    }
}

@Preview(name = "08 - Updates", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewUpdatesScreen() {
    DarkModeStudioTheme {
        UpdatesScreen(
            notificationRepository = remember { NotificationRepository() },
            onAvatarClick = {}
        )
    }
}

@Preview(name = "09 - Settings", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    DarkModeStudioTheme {
        SettingsScreen(
            settingsRepository = remember { SettingsRepository() },
            onBackClick = {}
        )
    }
}

@Preview(name = "Component - Floating Bottom Navigation", widthDp = 360, heightDp = 100)
@Composable
fun PreviewBottomNavigation() {
    DarkModeStudioTheme {
        Box(modifier = Modifier.background(DmsColors.OledBlack)) {
            DmsBottomNavigation(
                selectedRoute = "home",
                onNavigate = {},
                onCreateClick = {}
            )
        }
    }
}

@Preview(name = "Component - Orbital Graphic Canvas", widthDp = 200, heightDp = 200)
@Composable
fun PreviewOrbitalCanvas() {
    DarkModeStudioTheme {
        Box(modifier = Modifier.background(DmsColors.OledBlack)) {
            DmsOrbitalCanvas()
        }
    }
}

@Preview(name = "Component - Usage Line Graph", widthDp = 200, heightDp = 100)
@Composable
fun PreviewUsageLineGraph() {
    DarkModeStudioTheme {
        Box(modifier = Modifier.background(DmsColors.OledBlack)) {
            DmsUsageLineGraph(dataPoints = listOf(0.2f, 0.35f, 0.5f, 0.4f, 0.7f, 0.65f, 0.85f))
        }
    }
}
