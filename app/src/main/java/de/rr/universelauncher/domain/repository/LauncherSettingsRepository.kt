package de.rr.universelauncher.domain.repository

import kotlinx.coroutines.flow.Flow

interface LauncherSettingsRepository {
    fun getSelectedApps(): Flow<Set<String>>
    suspend fun setSelectedApps(apps: Set<String>)
    fun getAppLaunchCounts(): Flow<Map<String, Int>>
    suspend fun incrementAppLaunchCount(packageName: String)
    suspend fun getAppLaunchCount(packageName: String): Int
}
