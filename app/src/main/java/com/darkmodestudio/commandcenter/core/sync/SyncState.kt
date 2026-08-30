package com.darkmodestudio.commandcenter.core.sync

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val providerResults: List<ProviderSyncResult> = emptyList(),
    val errorMessage: String? = null
)
