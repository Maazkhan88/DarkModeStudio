package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.SupabaseConnector
import com.darkmodestudio.commandcenter.core.network.VercelConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun upsertIntegrationNonDestructively(database: DmsDatabase, integration: IntegrationEntity) {
    val existing = database.integrationDao().getIntegrationById(integration.id)
    if (existing == null) {
        database.integrationDao().insertIntegration(integration)
    } else {
        database.integrationDao().updateIntegration(integration)
    }
}

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
            upsertIntegrationNonDestructively(database, disconnected)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.SUPABASE,
                isSuccess = false,
                message = "Supabase credentials not configured"
            )
        }

        val result = supabaseConnector.pingHealth(storedUrl, storedKey)
        if (!result.isSuccess) {
            val existingMetrics = database.integrationDao().getMetricsByIntegration("supabase")
            val hasPriorMetrics = existingMetrics.isNotEmpty()

            val failedIntegration = IntegrationEntity(
                id = "supabase",
                name = "Supabase",
                category = "Database & Auth",
                isConnected = true,
                health = if (hasPriorMetrics) IntegrationHealth.DEGRADED else IntegrationHealth.ALERT,
                lastSync = nowFormatted,
                lastError = result.errorMessage ?: "Supabase connection failed",
                primaryMetric = if (hasPriorMetrics) "Degraded — ${result.errorMessage}" else "Unavailable — Check Credentials"
            )
            upsertIntegrationNonDestructively(database, failedIntegration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.SUPABASE,
                isSuccess = false,
                message = result.errorMessage ?: "Supabase ping failed"
            )
        }

        val latencyText = result.latencyMs?.let { "${it}ms" } ?: "Normal"
        val integration = IntegrationEntity(
            id = "supabase",
            name = "Supabase",
            category = "Database & Auth",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "$latencyText Latency • REST API Operational"
        )
        upsertIntegrationNonDestructively(database, integration)

        val metrics = mutableListOf<IntegrationMetricEntity>()
        result.latencyMs?.let {
            metrics.add(IntegrationMetricEntity(integrationId = "supabase", label = "DB Latency", value = "${it}ms"))
        }
        metrics.add(IntegrationMetricEntity(integrationId = "supabase", label = "REST Endpoint", value = "Operational"))
        metrics.add(IntegrationMetricEntity(integrationId = "supabase", label = "Database Service", value = "Active"))

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
            upsertIntegrationNonDestructively(database, disconnected)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CUSTOM,
                isSuccess = false,
                message = "Vercel token not configured"
            )
        }

        val result = vercelConnector.fetchDeployments(storedToken)
        if (!result.isSuccess) {
            val existingMetrics = database.integrationDao().getMetricsByIntegration("vercel")
            val hasPriorMetrics = existingMetrics.isNotEmpty()

            val failedIntegration = IntegrationEntity(
                id = "vercel",
                name = "Vercel",
                category = "Hosting & Previews",
                isConnected = true,
                health = if (hasPriorMetrics) IntegrationHealth.DEGRADED else IntegrationHealth.ALERT,
                lastSync = nowFormatted,
                lastError = result.errorMessage ?: "Vercel API error",
                primaryMetric = if (hasPriorMetrics) "Degraded — ${result.errorMessage}" else "Unavailable — Check Token"
            )
            upsertIntegrationNonDestructively(database, failedIntegration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.CUSTOM,
                isSuccess = false,
                message = result.errorMessage ?: "Vercel sync failed"
            )
        }

        val prodUrl = result.productionUrl ?: "Configured Project"
        val buildStatus = result.buildStatus ?: "Ready"
        val integration = IntegrationEntity(
            id = "vercel",
            name = "Vercel",
            category = "Hosting & Previews",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "$prodUrl • $buildStatus"
        )
        upsertIntegrationNonDestructively(database, integration)

        val metrics = mutableListOf<IntegrationMetricEntity>()
        result.productionUrl?.let {
            metrics.add(IntegrationMetricEntity(integrationId = "vercel", label = "Production URL", value = it))
        }
        result.buildStatus?.let {
            metrics.add(IntegrationMetricEntity(integrationId = "vercel", label = "Latest Build", value = it))
        }
        result.dailyDeployments?.let {
            metrics.add(IntegrationMetricEntity(integrationId = "vercel", label = "Deployments", value = "$it total"))
        }

        if (metrics.isNotEmpty()) {
            database.integrationDao().insertMetrics(metrics)
        }

        ProviderSyncResult(
            provider = SecureProvider.CUSTOM,
            isSuccess = true,
            message = "Vercel deployments synchronized"
        )
    }
}
