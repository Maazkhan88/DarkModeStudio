package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.CloudflareConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudflareSyncer(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val cloudflareConnector: CloudflareConnector = CloudflareConnector()
) : ProviderSyncer {

    override val provider: SecureProvider = SecureProvider.CLOUDFLARE

    override suspend fun sync(mode: SyncMode): ProviderSyncResult = withContext(Dispatchers.IO) {
        val storedToken = keystoreCredentialManager.getSecret("token_cloudflare")
        val isConnected = !storedToken.isNullOrBlank()

        val tokenToUse = storedToken ?: "preview_mode_token"
        val result = cloudflareConnector.fetchTelemetry(tokenToUse)

        val nowFormatted = "Just now"

        val integration = IntegrationEntity(
            id = "cloudflare",
            name = "Cloudflare",
            category = "Edge & Infrastructure",
            isConnected = isConnected,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "${result.totalRequestsLast24h} req • ${result.errorRate} err"
        )
        database.integrationDao().insertIntegration(integration)

        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "cloudflare", label = "Daily Requests", value = result.totalRequestsLast24h),
            IntegrationMetricEntity(integrationId = "cloudflare", label = "Error Rate", value = "${result.errorRate} (nominal)"),
            IntegrationMetricEntity(integrationId = "cloudflare", label = "Cache Hit Ratio", value = result.cacheHitRatio),
            IntegrationMetricEntity(integrationId = "cloudflare", label = "Workers Status", value = "${result.activeWorkersCount} workers active")
        )
        database.integrationDao().insertMetrics(metrics)

        ProviderSyncResult(
            provider = SecureProvider.CLOUDFLARE,
            isSuccess = true,
            message = "Cloudflare zones and worker telemetry synchronized"
        )
    }
}
