package com.darkmodestudio.commandcenter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.darkmodestudio.commandcenter.core.agent.DesktopHostBridge
import com.darkmodestudio.commandcenter.core.auth.ConnectAuthCoordinator
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.RepositoryFilesRepository
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.database.AppDataInitializer
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

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager
    private lateinit var syncCoordinator: SyncCoordinator
    private lateinit var connectAuthCoordinator: ConnectAuthCoordinator
    private lateinit var desktopHostBridge: DesktopHostBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = DmsDatabase.getInstance(this)
        keystoreManager = KeystoreCredentialManager(this)
        val appDataInitializer = AppDataInitializer(database)
        syncCoordinator = SyncCoordinator(database, keystoreManager)
        connectAuthCoordinator = ConnectAuthCoordinator(database, keystoreManager, syncCoordinator = syncCoordinator)
        desktopHostBridge = DesktopHostBridge(database, keystoreManager)

        val projectRepository = ProjectRepository(database.projectDao())
        val taskRepository = TaskRepository(database.taskDao())
        val agentRepository = AgentRepository(database.agentDao())
        val healthRepository = HealthRepository(database.integrationDao())
        val notificationRepository = NotificationRepository(database.notificationDao(), database.reminderDao(), database.settingsDao())
        val settingsRepository = SettingsRepository(database.settingsDao(), database.automationDao())
        val repositoryFilesRepository = RepositoryFilesRepository(
            keystoreCredentialManager = keystoreManager,
            repositoryFileDao = database.repositoryFileDao()
        )

        // Handle cold-start OAuth deep link callback if present
        intent?.data?.let { callbackUri ->
            CoroutineScope(Dispatchers.IO).launch {
                connectAuthCoordinator.handleDeepLink(callbackUri)
            }
        }

        // Deterministic startup sequence: initialize structural database defaults, then perform foreground sync
        CoroutineScope(Dispatchers.IO).launch {
            appDataInitializer.initialize()
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
                        repositoryFilesRepository = repositoryFilesRepository,
                        keystoreCredentialManager = keystoreManager,
                        syncCoordinator = syncCoordinator,
                        connectAuthCoordinator = connectAuthCoordinator,
                        desktopHostBridge = desktopHostBridge,
                        database = database
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle warm/hot OAuth deep link callback
        intent.data?.let { callbackUri ->
            CoroutineScope(Dispatchers.IO).launch {
                connectAuthCoordinator.handleDeepLink(callbackUri)
            }
        }
    }
}
