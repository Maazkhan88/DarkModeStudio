package com.darkmodestudio.commandcenter.core.sync

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.CloudflareConnector
import com.darkmodestudio.commandcenter.core.network.CloudflareTelemetryResult
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IntegrationCascadePreservationTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun cloudflareSyncer_repeatedSyncAndDisconnectedState_neverDeletesChildMetricsOrIncidents() = runBlocking {
        // 1. Initial seeded integration
        val initial = IntegrationEntity(
            id = "cloudflare",
            name = "Cloudflare",
            category = "Edge & Infrastructure",
            isConnected = false,
            health = IntegrationHealth.DISCONNECTED,
            lastSync = "Not configured",
            primaryMetric = "Disconnected"
        )
        database.integrationDao().upsertIntegrationNonDestructively(initial)

        // Seed child metrics and incidents
        database.integrationDao().insertMetrics(
            listOf(
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Daily Requests", value = "12.4M"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Cache Hit Ratio", value = "94.2%")
            )
        )
        database.integrationDao().insertIncidents(
            listOf(
                IntegrationIncidentEntity(
                    id = "cf-inc-1",
                    integrationId = "cloudflare",
                    title = "Edge SSL Degraded",
                    description = "Automatic failover completed",
                    timestamp = "10m ago",
                    isResolved = true
                )
            )
        )

        assertEquals(2, database.integrationDao().getMetricCount("cloudflare"))
        assertEquals(1, database.integrationDao().getIncidentCount("cloudflare"))

        // 2. Syncer runs with no token -> updates to DISCONNECTED state non-destructively
        val syncer = CloudflareSyncer(database, keystoreManager)
        val result1 = syncer.sync(SyncMode.MANUAL)
        assertTrue(!result1.isSuccess)

        // Child metrics and incidents MUST survive
        assertEquals(2, database.integrationDao().getMetricCount("cloudflare"))
        assertEquals(1, database.integrationDao().getIncidentCount("cloudflare"))

        // 3. Repeated updates non-destructively
        val updated = database.integrationDao().getIntegrationById("cloudflare")!!.copy(
            health = IntegrationHealth.OPERATIONAL,
            primaryMetric = "14.2M req • 0.01% err"
        )
        database.integrationDao().upsertIntegrationNonDestructively(updated)

        assertEquals(2, database.integrationDao().getMetricCount("cloudflare"))
        assertEquals(1, database.integrationDao().getIncidentCount("cloudflare"))
    }

    @Test
    fun supabaseAndVercel_repeatedSyncers_neverDeleteChildRows() = runBlocking {
        // Setup Supabase
        database.integrationDao().upsertIntegrationNonDestructively(
            IntegrationEntity(
                id = "supabase",
                name = "Supabase",
                category = "Database & Backend",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "Now",
                primaryMetric = "Connected"
            )
        )
        database.integrationDao().insertMetrics(
            listOf(IntegrationMetricEntity(integrationId = "supabase", label = "DB Health", value = "Healthy"))
        )
        database.integrationDao().insertIncidents(
            listOf(IntegrationIncidentEntity(id = "sb-1", integrationId = "supabase", title = "Connection Pool Warning", description = "Resolved", timestamp = "Yesterday", isResolved = true))
        )

        val supabaseSyncer = SupabaseSyncer(database, keystoreManager)
        supabaseSyncer.sync(SyncMode.MANUAL)

        // Metrics and incidents preserved
        assertEquals(1, database.integrationDao().getMetricCount("supabase"))
        assertEquals(1, database.integrationDao().getIncidentCount("supabase"))

        // Setup Vercel
        database.integrationDao().upsertIntegrationNonDestructively(
            IntegrationEntity(
                id = "vercel",
                name = "Vercel",
                category = "Cloud & Hosting",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "Now",
                primaryMetric = "Connected"
            )
        )
        database.integrationDao().insertMetrics(
            listOf(IntegrationMetricEntity(integrationId = "vercel", label = "Deployments", value = "12 active"))
        )

        val vercelSyncer = VercelSyncer(database, keystoreManager)
        vercelSyncer.sync(SyncMode.MANUAL)

        assertEquals(1, database.integrationDao().getMetricCount("vercel"))
    }
}
