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
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.designsystem.theme.DarkModeStudioTheme
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.sync.SyncCoordinator
import com.darkmodestudio.commandcenter.core.sync.SyncMode
import com.darkmodestudio.commandcenter.navigation.DmsNavHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DmsDatabase.getInstance(this)
        val keystoreManager = KeystoreCredentialManager(this)
        val syncCoordinator = SyncCoordinator(database, keystoreManager)

        val projectRepository = ProjectRepository(database.projectDao())
        val taskRepository = TaskRepository(database.taskDao())
        val agentRepository = AgentRepository(database.agentDao())
        val healthRepository = HealthRepository(database.integrationDao())
        val notificationRepository = NotificationRepository(database.notificationDao(), database.reminderDao(), database.settingsDao())
        val settingsRepository = SettingsRepository(database.settingsDao(), database.automationDao())

        // Foreground Sync: Instant Room cached state is displayed immediately, background refresh syncs incrementally
        CoroutineScope(Dispatchers.IO).launch {
            syncCoordinator.syncAll(SyncMode.FOREGROUND)
        }

        setContent {
            DarkModeStudioTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DmsColors.OledBlack),
                    color = DmsColors.OledBlack
                ) {
                    val navController = rememberNavController()

                    DmsNavHost(
                        navController = navController,
                        projectRepository = projectRepository,
                        taskRepository = taskRepository,
                        agentRepository = agentRepository,
                        healthRepository = healthRepository,
                        notificationRepository = notificationRepository,
                        settingsRepository = settingsRepository,
                        keystoreCredentialManager = keystoreManager,
                        syncCoordinator = syncCoordinator
                    )
                }
            }
        }
    }
}
