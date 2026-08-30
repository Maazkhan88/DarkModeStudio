package com.darkmodestudio.commandcenter.core.data.repository

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RepositoryFilesRepositoryTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)
        keystoreManager.saveSecret("token_github", "test_mock_token")
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun loadDirectory_unlinkedProject_returnsNotLinkedState() = runBlocking {
        val filesRepo = RepositoryFilesRepository(
            gitHubConnector = GitHubConnector(),
            keystoreCredentialManager = keystoreManager,
            repositoryFileDao = database.repositoryFileDao()
        )

        filesRepo.loadDirectory(repoFullName = null)
        val stateNull = filesRepo.filesState.first()
        assertTrue(stateNull is RepositoryFilesState.NotLinked)

        filesRepo.loadDirectory(repoFullName = "")
        val stateEmpty = filesRepo.filesState.first()
        assertTrue(stateEmpty is RepositoryFilesState.NotLinked)
    }

    @Test
    fun loadDirectory_nonMainDefaultBranch_passesRefAndReportsBranch() = runBlocking {
        var capturedUrl = ""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                capturedUrl = chain.request().url.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[{\"name\": \"feature.txt\", \"path\": \"feature.txt\", \"sha\": \"sha123\", \"size\": 100, \"type\": \"file\"}]".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val filesRepo = RepositoryFilesRepository(
            gitHubConnector = connector,
            keystoreCredentialManager = keystoreManager,
            repositoryFileDao = database.repositoryFileDao()
        )

        filesRepo.loadDirectory(
            repoFullName = "Maazkhan88/DarkModeStudio",
            branch = "develop",
            path = ""
        )

        val state = filesRepo.filesState.first()
        assertTrue(state is RepositoryFilesState.Loaded)
        val loaded = state as RepositoryFilesState.Loaded
        assertEquals("develop", loaded.branch)
        assertEquals("Maazkhan88/DarkModeStudio", loaded.repository)
        assertTrue(capturedUrl.contains("ref=develop"))
    }

    @Test
    fun loadDirectory_rootAndNavigation_persistsToRoomAndUpdatesState() = runBlocking {
        val rootContentsJson = """
            [
                {
                    "name": "app",
                    "path": "app",
                    "sha": "sha_app_dir",
                    "size": 0,
                    "type": "dir"
                },
                {
                    "name": "README.md",
                    "path": "README.md",
                    "sha": "sha_readme",
                    "size": 2048,
                    "type": "file",
                    "download_url": "https://raw.githubusercontent.com/Maazkhan88/DarkModeStudio/main/README.md"
                }
            ]
        """.trimIndent()

        val appSubDirJson = """
            [
                {
                    "name": "build.gradle.kts",
                    "path": "app/build.gradle.kts",
                    "sha": "sha_gradle",
                    "size": 1024,
                    "type": "file"
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                val bodyJson = if (url.contains("/contents/app")) {
                    appSubDirJson
                } else {
                    rootContentsJson
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(bodyJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val filesRepo = RepositoryFilesRepository(
            gitHubConnector = connector,
            keystoreCredentialManager = keystoreManager,
            repositoryFileDao = database.repositoryFileDao()
        )

        // 1. Load Root Directory
        filesRepo.loadDirectory("Maazkhan88/DarkModeStudio", "main", "")
        val rootState = filesRepo.filesState.first()
        assertTrue("State should be Loaded", rootState is RepositoryFilesState.Loaded)
        val loadedRoot = rootState as RepositoryFilesState.Loaded
        assertEquals("Maazkhan88/DarkModeStudio", loadedRoot.repository)
        assertEquals("", loadedRoot.path)
        assertEquals(2, loadedRoot.entries.size)
        assertTrue("First item is app directory", loadedRoot.entries[0].isDirectory)
        assertEquals("app", loadedRoot.entries[0].name)
        assertEquals("README.md", loadedRoot.entries[1].name)

        // Verify Room SQLite persistence
        val cachedRoot = database.repositoryFileDao().getFiles("Maazkhan88/DarkModeStudio", "")
        assertEquals(2, cachedRoot.size)

        // 2. Navigate Into Subfolder "app"
        filesRepo.navigateTo("app")
        val subState = filesRepo.filesState.first()
        assertTrue("Subfolder state should be Loaded", subState is RepositoryFilesState.Loaded)
        val loadedSub = subState as RepositoryFilesState.Loaded
        assertEquals("app", loadedSub.path)
        assertEquals(1, loadedSub.entries.size)
        assertEquals("build.gradle.kts", loadedSub.entries[0].name)

        // 3. Navigate Up Back to Root
        filesRepo.navigateUp()
        val backState = filesRepo.filesState.first()
        assertTrue("Back state should be Loaded", backState is RepositoryFilesState.Loaded)
        val loadedBack = backState as RepositoryFilesState.Loaded
        assertEquals("", loadedBack.path)
        assertEquals(2, loadedBack.entries.size)
    }

    @Test
    fun loadDirectory_authFailure_returnsErrorState() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val filesRepo = RepositoryFilesRepository(
            gitHubConnector = connector,
            keystoreCredentialManager = keystoreManager,
            repositoryFileDao = database.repositoryFileDao()
        )

        filesRepo.loadDirectory("Maazkhan88/PrivateRepo", "main", "")
        val state = filesRepo.filesState.first()
        assertTrue("State should be Error", state is RepositoryFilesState.Error)
        assertTrue((state as RepositoryFilesState.Error).message.contains("401"))
    }
}
