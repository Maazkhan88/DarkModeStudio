package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.SupabaseConnector
import com.darkmodestudio.commandcenter.core.network.VercelConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseSyncer(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val supabaseConnector: SupabaseConnector = SupabaseConnector()
) : ProviderSyncer {

    override val provider: SecureProvider = SecureProvider.SUPABASE

    override suspend fun sync(mode: SyncMode): ProviderSyncResult = withContext(Dispatchers.IO) {
        val storedKey = keystoreCredentialManager.getSecret("token_supabase")
        val storedUrl = keystoreCredentialManager.getSecret("url_supabase")
        val nowFormatted = DmsTimeFormatter.formatNow()

        if (storedKey.isNullOrBlank() || storedUrl.isNullOrBlank()) {
            val disconnected = IntegrationEntity(
                id = "supabase",
                name = "Supabase",
                category = "Database & Auth",
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Not configured",
                lastSuccessfulSync = null,
                lastError = null,
                primaryMetric = "Disconnected — Tap to configure"
            )
            database.integrationDao().insertIntegration(disconnected)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.SUPABASE,
                isSuccess = false,
                message = "Supabase credentials not configured"
            )
        }

        val result = supabaseConnector.pingHealth(storedUrl, storedKey)

        val integration = IntegrationEntity(
            id = "supabase",
            name = "Supabase",
            category = "Database & Auth",
            isConnected = true,
            health = if (result.isDegraded) IntegrationHealth.DEGRADED else IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "${result.latencyMs}ms Latency • Pool ${result.poolUsagePercent}%"
        )
        database.integrationDao().insertIntegration(integration)

        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "supabase", label = "DB Latency", value = "${result.latencyMs}ms"),
            IntegrationMetricEntity(integrationId = "supabase", label = "Connection Pool", value = "${result.poolUsagePercent}% active"),
            IntegrationMetricEntity(integrationId = "supabase", label = "Storage Used", value = "${result.storageUsedGb} GB / ${result.storageTotalGb} GB"),
            IntegrationMetricEntity(integrationId = "supabase", label = "Auth Service", value = "Operational")
        )
        database.integrationDao().insertMetrics(metrics)

        ProviderSyncResult(
            provider = SecureProvider.SUPABASE,
            isSuccess = true,
            message = "Supabase database telemetry synchronized"
        )
    }
}

class VercelSyncer(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val vercelConnector: VercelConnector = VercelConnector()
) : ProviderSyncer {

    override val provider: SecureProvider = SecureProvider.CUSTOM

    override suspend fun sync(mode: SyncMode): ProviderSyncResult = withContext(Dispatchers.IO) {
        val storedToken = keystoreCredentialManager.getSecret("token_vercel")
        val nowFormatted = DmsTimeFormatter.formatNow()

        if (storedToken.isNullOrBlank()) {
            val disconnected = IntegrationEntity(
                id = "vercel",
                name = "Vercel",
                category = "Hosting & Previews",
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Not configured",
                lastSuccessfulSync = null,
                lastError = null,
                primaryMetric = "Disconnected — Tap to configure"
            )
            database.integrationDao().insertIntegration(disconnected)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CUSTOM,
                isSuccess = false,
                message = "Vercel token not configured"
            )
        }

        val result = vercelConnector.fetchDeployments(storedToken)
        if (!result.isSuccess) {
            val failedIntegration = IntegrationEntity(
                id = "vercel",
                name = "Vercel",
                category = "Hosting & Previews",
                isConnected = true,
                health = IntegrationHealth.ALERT,
                lastSync = nowFormatted,
                lastError = "Vercel API error",
                primaryMetric = "Sync Failure — Check Token"
            )
            database.integrationDao().insertIntegration(failedIntegration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CUSTOM,
                isSuccess = false,
                message = "Vercel sync failed"
            )
        }

        val integration = IntegrationEntity(
            id = "vercel",
            name = "Vercel",
            category = "Hosting & Previews",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "${result.productionUrl} • ${result.buildStatus}"
        )
        database.integrationDao().insertIntegration(integration)

        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "vercel", label = "Production URL", value = result.productionUrl),
            IntegrationMetricEntity(integrationId = "vercel", label = "Latest Build", value = result.buildStatus),
            IntegrationMetricEntity(integrationId = "vercel", label = "Daily Deploys", value = "${result.dailyDeployments} deployed today"),
            IntegrationMetricEntity(integrationId = "vercel", label = "Edge Network", value = "${result.edgeLatencyMs}ms worldwide")
        )
        database.integrationDao().insertMetrics(metrics)

        ProviderSyncResult(
            provider = SecureProvider.CUSTOM,
            isSuccess = true,
            message = "Vercel deployments synchronized"
        )
    }
}
