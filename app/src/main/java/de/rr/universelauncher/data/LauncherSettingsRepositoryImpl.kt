package de.rr.universelauncher.data

import de.rr.universelauncher.data.preferences.LauncherPreferences
import de.rr.universelauncher.domain.repository.LauncherSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LauncherSettingsRepositoryImpl @Inject constructor(
    private val launcherPreferences: LauncherPreferences
) : LauncherSettingsRepository {

    override fun getSelectedApps(): Flow<Set<String>> = launcherPreferences.getSelectedApps()

    override suspend fun setSelectedApps(apps: Set<String>) = launcherPreferences.setSelectedApps(apps)

    override fun getAppLaunchCounts(): Flow<Map<String, Int>> = launcherPreferences.getAppLaunchCounts()

    override suspend fun incrementAppLaunchCount(packageName: String) = 
        launcherPreferences.incrementAppLaunchCount(packageName)

    override suspend fun getAppLaunchCount(packageName: String): Int {
        return launcherPreferences.getAppLaunchCount(packageName)
    }
}
