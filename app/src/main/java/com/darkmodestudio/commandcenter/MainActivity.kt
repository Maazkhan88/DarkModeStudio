package com.darkmodestudio.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.theme.DarkModeStudioTheme
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.navigation.DmsNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DarkModeStudioTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DmsColors.OledBlack),
                    color = DmsColors.OledBlack
                ) {
                    val navController = rememberNavController()

                    val projectRepository = remember { ProjectRepository() }
                    val taskRepository = remember { TaskRepository() }
                    val agentRepository = remember { AgentRepository() }
                    val healthRepository = remember { HealthRepository() }
                    val notificationRepository = remember { NotificationRepository() }
                    val settingsRepository = remember { SettingsRepository() }

                    DmsNavHost(
                        navController = navController,
                        projectRepository = projectRepository,
                        taskRepository = taskRepository,
                        agentRepository = agentRepository,
                        healthRepository = healthRepository,
                        notificationRepository = notificationRepository,
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}
