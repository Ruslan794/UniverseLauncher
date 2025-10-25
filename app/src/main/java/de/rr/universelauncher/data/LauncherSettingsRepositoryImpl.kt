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

    override fun getAppOrder(): Flow<Map<String, Int>> = launcherPreferences.getAppOrder()

    override suspend fun setAppOrder(appOrder: Map<String, Int>) = launcherPreferences.setAppOrder(appOrder)

    override fun getAppOrbitSpeeds(): Flow<Map<String, Float>> = launcherPreferences.getAppOrbitSpeeds()

    override suspend fun setAppOrbitSpeed(packageName: String, speed: Float) = 
        launcherPreferences.setAppOrbitSpeed(packageName, speed)

    override suspend fun getAppOrbitSpeed(packageName: String): Float? = 
        launcherPreferences.getAppOrbitSpeed(packageName)

    override fun getAppPlanetSizes(): Flow<Map<String, String>> = launcherPreferences.getAppPlanetSizes()

    override suspend fun setAppPlanetSize(packageName: String, size: String) = 
        launcherPreferences.setAppPlanetSize(packageName, size)

    override suspend fun getAppPlanetSize(packageName: String): String? = 
        launcherPreferences.getAppPlanetSize(packageName)

    override fun getFolders() = launcherPreferences.getFolders()

    override suspend fun saveFolders(folders: List<de.rr.universelauncher.domain.model.FolderData>) = 
        launcherPreferences.saveFolders(folders)

    override suspend fun updateFolderName(folderId: String, newName: String) = 
        launcherPreferences.updateFolderName(folderId, newName)

    override fun getFolderSelectedApps(folderId: String) = launcherPreferences.getFolderSelectedApps(folderId)

    override suspend fun setFolderSelectedApps(folderId: String, apps: Set<String>) = 
        launcherPreferences.setFolderSelectedApps(folderId, apps)

    override fun getFolderAppOrder(folderId: String) = launcherPreferences.getFolderAppOrder(folderId)

    override suspend fun setFolderAppOrder(folderId: String, appOrder: Map<String, Int>) = 
        launcherPreferences.setFolderAppOrder(folderId, appOrder)

    override fun getFolderAppOrbitSpeeds(folderId: String) = launcherPreferences.getFolderAppOrbitSpeeds(folderId)

    override suspend fun setFolderAppOrbitSpeed(folderId: String, packageName: String, speed: Float) = 
        launcherPreferences.setFolderAppOrbitSpeed(folderId, packageName, speed)

    override suspend fun getFolderAppOrbitSpeed(folderId: String, packageName: String): Float? = 
        launcherPreferences.getFolderAppOrbitSpeed(folderId, packageName)

    override fun getFolderAppPlanetSizes(folderId: String) = launcherPreferences.getFolderAppPlanetSizes(folderId)

    override suspend fun setFolderAppPlanetSize(folderId: String, packageName: String, size: String) = 
        launcherPreferences.setFolderAppPlanetSize(folderId, packageName, size)

    override suspend fun getFolderAppPlanetSize(folderId: String, packageName: String): String? = 
        launcherPreferences.getFolderAppPlanetSize(folderId, packageName)
}
