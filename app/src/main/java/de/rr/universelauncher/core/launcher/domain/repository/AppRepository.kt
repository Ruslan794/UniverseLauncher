package de.rr.universelauncher.core.launcher.domain.repository

import de.rr.universelauncher.core.launcher.domain.model.AppInfo

interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    fun launchApp(packageName: String)
}
