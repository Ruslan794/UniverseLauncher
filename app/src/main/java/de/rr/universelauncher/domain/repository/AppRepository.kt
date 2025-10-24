package de.rr.universelauncher.domain.repository

import de.rr.universelauncher.domain.model.AppInfo

interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    fun launchApp(packageName: String)
}
