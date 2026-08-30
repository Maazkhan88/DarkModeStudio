package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.model.AutomationStats
import com.darkmodestudio.commandcenter.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository {

    private val _userProfile = MutableStateFlow(UserProfile())
    private val _automationStats = MutableStateFlow(AutomationStats())
    private val _biometricLock = MutableStateFlow(true)
    private val _syncFrequency = MutableStateFlow("15 minutes")
    private val _dailyBriefing = MutableStateFlow(true)

    val userProfile: Flow<UserProfile> = _userProfile.asStateFlow()
    val automationStats: Flow<AutomationStats> = _automationStats.asStateFlow()
    val biometricLock: Flow<Boolean> = _biometricLock.asStateFlow()
    val syncFrequency: Flow<String> = _syncFrequency.asStateFlow()
    val dailyBriefing: Flow<Boolean> = _dailyBriefing.asStateFlow()

    fun toggleBiometricLock() {
        _biometricLock.update { !it }
    }

    fun toggleDailyBriefing() {
        _dailyBriefing.update { !it }
    }
}
