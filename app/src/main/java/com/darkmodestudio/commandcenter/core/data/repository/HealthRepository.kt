package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.IntegrationDao
import com.darkmodestudio.commandcenter.core.database.dao.IntegrationWithDetails
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.IntegrationItem
import com.darkmodestudio.commandcenter.core.model.IntegrationMetric
import com.darkmodestudio.commandcenter.core.model.PlatformHealthSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class HealthRepository(private val integrationDao: IntegrationDao? = null) {

    val integrations: Flow<List<IntegrationItem>> = integrationDao?.getIntegrationsWithDetailsFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultIntegrations)

    val summaryFlow: Flow<PlatformHealthSummary> = integrations.map { list ->
        val connected = list.count { it.isConnected }
        val degraded = list.count { it.health == IntegrationHealth.DEGRADED }
        val disconnected = list.count { it.health == IntegrationHealth.DISCONNECTED || !it.isConnected }
        val alerts = list.sumOf { it.activeAlerts.size }

        val score = if (list.isNotEmpty()) {
            val healthyCount = list.count { it.health == IntegrationHealth.OPERATIONAL && it.isConnected }
            healthyCount.toFloat() / list.size
        } else 1.0f

        PlatformHealthSummary(
            connectedCount = connected,
            degradedCount = degraded,
            disconnectedCount = disconnected,
            alertsCount = alerts,
            healthScore = score
        )
    }

    val summary = PlatformHealthSummary(
        connectedCount = 7,
        degradedCount = 1,
        disconnectedCount = 0,
        alertsCount = 2,
        healthScore = 0.96f
    )

    companion object {
        val defaultIntegrations = listOf(
            IntegrationItem(
                id = "github",
                name = "GitHub",
                category = "Code & CI/CD",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "Just now",
                primaryMetric = "All CI Actions Passing",
                metrics = listOf(
                    IntegrationMetric("Primary Repo", "darkmodestudio/core"),
                    IntegrationMetric("Last Push", "4m ago (main)"),
                    IntegrationMetric("Open PRs", "3 review required"),
                    IntegrationMetric("Workflows", "12 passing / 0 failing")
                )
            ),
            IntegrationItem(
                id = "cloudflare",
                name = "Cloudflare",
                category = "Edge & Infrastructure",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "1m ago",
                primaryMetric = "1.4M req • 0.02% err",
                metrics = listOf(
                    IntegrationMetric("Daily Requests", "1,420,890"),
                    IntegrationMetric("Error Rate", "0.02% (nominal)"),
                    IntegrationMetric("Cache Hit Ratio", "94.8%"),
                    IntegrationMetric("Workers Status", "14 workers active")
                )
            ),
            IntegrationItem(
                id = "firebase",
                name = "Firebase",
                category = "Mobile & Crashlytics",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "2m ago",
                primaryMetric = "99.94% Crash-Free",
                metrics = listOf(
                    IntegrationMetric("Latest Build", "Release #28"),
                    IntegrationMetric("Crash-Free Rate", "99.94% sessions"),
                    IntegrationMetric("Auth Service", "Operational"),
                    IntegrationMetric("FCM Messaging", "Healthy")
                )
            ),
            IntegrationItem(
                id = "play_console",
                name = "Google Play Console",
                category = "Distribution",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "12m ago",
                primaryMetric = "Internal Track v1.0.0-rc2",
                metrics = listOf(
                    IntegrationMetric("Active Track", "Internal Testing"),
                    IntegrationMetric("Version", "1.0.0-rc2 (104)"),
                    IntegrationMetric("Crash Rate", "0.01% ANR/Crash"),
                    IntegrationMetric("Rating", "4.9 ★ (Internal QA)")
                )
            ),
            IntegrationItem(
                id = "apple_dev",
                name = "Apple Developer",
                category = "Distribution",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "1h ago",
                primaryMetric = "TestFlight v1.0 (42)",
                metrics = listOf(
                    IntegrationMetric("TestFlight Build", "v1.0 (Build 42)"),
                    IntegrationMetric("External Testers", "14 Active"),
                    IntegrationMetric("App Store Status", "Pending Binary Upload")
                )
            ),
            IntegrationItem(
                id = "supabase",
                name = "Supabase",
                category = "Database & Auth",
                isConnected = true,
                health = IntegrationHealth.DEGRADED,
                lastSync = "45s ago",
                primaryMetric = "Connection Pool High",
                metrics = listOf(
                    IntegrationMetric("Database Latency", "42ms"),
                    IntegrationMetric("Pool Usage", "88% threshold alert"),
                    IntegrationMetric("Storage Used", "18.4 GB / 50 GB"),
                    IntegrationMetric("Auth Endpoints", "100% Available")
                ),
                activeAlerts = listOf("Connection pool above 85% on replica 02")
            ),
            IntegrationItem(
                id = "vercel",
                name = "Vercel",
                category = "Frontend & Edge",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "5m ago",
                primaryMetric = "Deploy: secondme-web",
                metrics = listOf(
                    IntegrationMetric("Production URL", "secondme-web.app"),
                    IntegrationMetric("Build Status", "Ready in 18s"),
                    IntegrationMetric("Daily Deployments", "14"),
                    IntegrationMetric("Edge Latency", "28ms avg")
                )
            )
        )
    }
}

private fun IntegrationWithDetails.toDomain(): IntegrationItem {
    return IntegrationItem(
        id = integration.id,
        name = integration.name,
        category = integration.category,
        isConnected = integration.isConnected,
        health = integration.health,
        lastSync = integration.lastSync,
        primaryMetric = integration.primaryMetric,
        metrics = metrics.map { IntegrationMetric(it.label, it.value) },
        activeAlerts = incidents.filter { !it.isResolved }.map { it.title }
    )
}
