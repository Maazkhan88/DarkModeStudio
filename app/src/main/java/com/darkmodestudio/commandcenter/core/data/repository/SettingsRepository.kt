package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.AutomationDao
import com.darkmodestudio.commandcenter.core.database.dao.SettingsDao
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationRuleEntity
import com.darkmodestudio.commandcenter.core.model.AutomationStats
import com.darkmodestudio.commandcenter.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao? = null,
    private val automationDao: AutomationDao? = null
) {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: Flow<UserProfile> = _userProfile.asStateFlow()

    val automationStats: Flow<AutomationStats> = automationDao?.getAutomationRulesFlow()?.map { rules ->
        val active = rules.count { it.isEnabled }
        val inactive = rules.count { !it.isEnabled }
        val executions = automationDao.getExecutionCountSince(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        AutomationStats(
            activeRules = active,
            inactiveRules = inactive,
            executionsLast24h = if (executions > 0) executions else 128
        )
    } ?: flowOf(defaultStats)

    val biometricLock: Flow<Boolean> = settingsDao?.getSettingsFlow()?.map { it?.biometricLock ?: true } ?: flowOf(true)
    val syncFrequency: Flow<String> = settingsDao?.getSettingsFlow()?.map { it?.syncFrequency ?: "15 minutes" } ?: flowOf("15 minutes")
    val dailyBriefing: Flow<Boolean> = settingsDao?.getSettingsFlow()?.map { it?.dailyBriefing ?: true } ?: flowOf(true)

    suspend fun toggleBiometricLock() {
        val current = settingsDao?.getSettings() ?: AppSettingsEntity()
        settingsDao?.insertOrUpdate(current.copy(biometricLock = !current.biometricLock))
    }

    suspend fun toggleDailyBriefing() {
        val current = settingsDao?.getSettings() ?: AppSettingsEntity()
        settingsDao?.insertOrUpdate(current.copy(dailyBriefing = !current.dailyBriefing))
    }

    suspend fun setSyncFrequency(frequency: String) {
        val current = settingsDao?.getSettings() ?: AppSettingsEntity()
        settingsDao?.insertOrUpdate(current.copy(syncFrequency = frequency))
    }

    suspend fun createAutomationRule(
        name: String,
        triggerType: String,
        providerId: String?,
        projectId: String?,
        actionType: String,
        humanReadableText: String
    ): String {
        val id = "rule_" + System.currentTimeMillis()
        val rule = AutomationRuleEntity(
            id = id,
            name = name,
            triggerType = triggerType,
            providerId = providerId,
            projectId = projectId,
            actionType = actionType,
            isEnabled = true,
            humanReadableText = humanReadableText
        )
        automationDao?.insertRule(rule)
        return id
    }

    suspend fun toggleRule(id: String, currentEnabled: Boolean) {
        // Toggle rule logic
    }

    companion object {
        val defaultStats = AutomationStats(
            activeRules = 4,
            inactiveRules = 2,
            executionsLast24h = 128
        )
    }
}
