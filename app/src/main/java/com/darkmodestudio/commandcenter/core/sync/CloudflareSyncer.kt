package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.CloudflareConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
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
        val nowFormatted = DmsTimeFormatter.formatNow()

        if (storedToken.isNullOrBlank()) {
            val disconnected = IntegrationEntity(
                id = "cloudflare",
                name = "Cloudflare",
                category = "Edge & Infrastructure",
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Not configured",
                lastSuccessfulSync = null,
                lastError = null,
                primaryMetric = "Disconnected — Tap to configure"
            )
            database.integrationDao().upsertIntegrationNonDestructively(disconnected)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CLOUDFLARE,
                isSuccess = false,
                message = "Cloudflare API Token not configured"
            )
        }

        val result = cloudflareConnector.fetchTelemetry(storedToken)
        if (!result.isSuccess) {
            val failedIntegration = IntegrationEntity(
                id = "cloudflare",
                name = "Cloudflare",
                category = "Edge & Infrastructure",
                isConnected = true,
                health = IntegrationHealth.ALERT,
                lastSync = nowFormatted,
                lastError = "Failed to fetch Cloudflare telemetry",
                primaryMetric = "Sync Failure — Verify Token"
            )
            database.integrationDao().upsertIntegrationNonDestructively(failedIntegration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CLOUDFLARE,
                isSuccess = false,
                message = "Cloudflare sync failed"
            )
        }

        val integration = IntegrationEntity(
            id = "cloudflare",
            name = "Cloudflare",
            category = "Edge & Infrastructure",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "${result.totalRequestsLast24h} req • ${result.errorRate} err"
        )
        database.integrationDao().upsertIntegrationNonDestructively(integration)

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
            message = "Cloudflare live edge telemetry synchronized"
        )
    }
}
