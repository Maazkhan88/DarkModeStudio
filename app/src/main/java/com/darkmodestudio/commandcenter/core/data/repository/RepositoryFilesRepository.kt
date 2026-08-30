package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.RepositoryFileDao
import com.darkmodestudio.commandcenter.core.database.entity.RepositoryFileEntryEntity
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.network.GitHubContentsResult
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class RepositoryFileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val sha: String,
    val downloadUrl: String? = null
)

sealed interface RepositoryFilesState {
    data object NotLinked : RepositoryFilesState
    data object Disconnected : RepositoryFilesState
    data object Loading : RepositoryFilesState
    data class Loaded(
        val repository: String,
        val branch: String = "main",
        val path: String = "",
        val entries: List<RepositoryFileEntry> = emptyList()
    ) : RepositoryFilesState
    data class Error(val message: String) : RepositoryFilesState
}

class RepositoryFilesRepository(
    private val gitHubConnector: GitHubConnector = GitHubConnector(),
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val repositoryFileDao: RepositoryFileDao? = null
) {
    private val _filesState = MutableStateFlow<RepositoryFilesState>(RepositoryFilesState.Loading)
    val filesState: StateFlow<RepositoryFilesState> = _filesState.asStateFlow()

    private var currentRepo: String? = null
    private var currentBranch: String = "main"
    private var currentPath: String = ""

    fun setNotLinked() {
        _filesState.value = RepositoryFilesState.NotLinked
    }

    suspend fun loadDirectory(
        repoFullName: String?,
        branch: String? = "main",
        path: String = "",
        forceRefresh: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (repoFullName.isNullOrBlank()) {
            _filesState.value = RepositoryFilesState.NotLinked
            return@withContext
        }

        currentRepo = repoFullName
        currentBranch = branch ?: "main"
        currentPath = path

        val token = keystoreCredentialManager.getSecret("token_github")
        if (token.isNullOrBlank()) {
            _filesState.value = RepositoryFilesState.Disconnected
            return@withContext
        }

        // 1. Check local Room cache first unless force refresh
        if (!forceRefresh && repositoryFileDao != null) {
            val cached = repositoryFileDao.getFiles(repoFullName, path)
            if (cached.isNotEmpty()) {
                _filesState.value = RepositoryFilesState.Loaded(
                    repository = repoFullName,
                    branch = currentBranch,
                    path = path,
                    entries = cached.map { it.toDomain() }
                )
                return@withContext
            }
        }

        _filesState.value = RepositoryFilesState.Loading

        // 2. Fetch live from GitHub Contents API with explicit branch ref
        when (val result = gitHubConnector.fetchRepoContents(token, repoFullName, path, currentBranch)) {
            is GitHubContentsResult.Success -> {
                val entries = result.entries.map { dto ->
                    RepositoryFileEntry(
                        name = dto.name,
                        path = dto.path,
                        isDirectory = dto.type == "dir",
                        size = dto.size,
                        sha = dto.sha,
                        downloadUrl = dto.downloadUrl
                    )
                }

                // Persist to Room
                if (repositoryFileDao != null) {
                    val entities = result.entries.map { dto ->
                        RepositoryFileEntryEntity(
                            id = "$repoFullName:${path}:${dto.name}",
                            repositoryFullName = repoFullName,
                            path = path,
                            name = dto.name,
                            fullPath = dto.path,
                            type = dto.type,
                            size = dto.size,
                            sha = dto.sha,
                            downloadUrl = dto.downloadUrl,
                            lastCached = System.currentTimeMillis()
                        )
                    }
                    repositoryFileDao.deleteFilesForPath(repoFullName, path)
                    repositoryFileDao.insertFiles(entities)
                }

                _filesState.value = RepositoryFilesState.Loaded(
                    repository = repoFullName,
                    branch = currentBranch,
                    path = path,
                    entries = entries
                )
            }
            is GitHubContentsResult.NoCredentials -> {
                _filesState.value = RepositoryFilesState.Disconnected
            }
            is GitHubContentsResult.AuthFailure -> {
                _filesState.value = RepositoryFilesState.Error("GitHub token expired or invalid (401)")
            }
            is GitHubContentsResult.RateLimited -> {
                _filesState.value = RepositoryFilesState.Error("GitHub API rate limit exceeded")
            }
            is GitHubContentsResult.ServerFailure -> {
                _filesState.value = RepositoryFilesState.Error("GitHub server error (${result.code})")
            }
            is GitHubContentsResult.NetworkFailure -> {
                _filesState.value = RepositoryFilesState.Error(result.message)
            }
        }
    }

    suspend fun navigateTo(subFolderName: String) {
        val repo = currentRepo ?: return
        val newPath = if (currentPath.isBlank()) subFolderName else "$currentPath/$subFolderName"
        loadDirectory(repo, currentBranch, newPath)
    }

    suspend fun navigateUp() {
        val repo = currentRepo ?: return
        if (currentPath.isBlank()) return
        val parentPath = if (currentPath.contains("/")) {
            currentPath.substringBeforeLast("/")
        } else {
            ""
        }
        loadDirectory(repo, currentBranch, parentPath)
    }

    suspend fun refresh() {
        val repo = currentRepo ?: return
        loadDirectory(repo, currentBranch, currentPath, forceRefresh = true)
    }
}

private fun RepositoryFileEntryEntity.toDomain(): RepositoryFileEntry {
    return RepositoryFileEntry(
        name = name,
        path = fullPath,
        isDirectory = type == "dir",
        size = size,
        sha = sha,
        downloadUrl = downloadUrl
    )
}
