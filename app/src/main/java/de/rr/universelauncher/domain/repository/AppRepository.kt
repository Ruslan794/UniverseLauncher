package de.rr.universelauncher.domain.repository

import de.rr.universelauncher.domain.model.AppInfo

interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getInstalledAppsWithLaunchCounts(): List<AppInfo>
    fun launchApp(packageName: String)
    suspend fun trackAppLaunch(packageName: String)
}
