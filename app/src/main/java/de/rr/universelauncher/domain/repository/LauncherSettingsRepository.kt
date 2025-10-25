package de.rr.universelauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import de.rr.universelauncher.domain.model.FolderData

interface LauncherSettingsRepository {
    fun getSelectedApps(): Flow<Set<String>>
    suspend fun setSelectedApps(apps: Set<String>)
    fun getAppLaunchCounts(): Flow<Map<String, Int>>
    suspend fun incrementAppLaunchCount(packageName: String)
    suspend fun getAppLaunchCount(packageName: String): Int
    fun getAppOrder(): Flow<Map<String, Int>>
    suspend fun setAppOrder(appOrder: Map<String, Int>)
    fun getAppOrbitSpeeds(): Flow<Map<String, Float>>
    suspend fun setAppOrbitSpeed(packageName: String, speed: Float)
    suspend fun getAppOrbitSpeed(packageName: String): Float?
    fun getAppPlanetSizes(): Flow<Map<String, String>>
    suspend fun setAppPlanetSize(packageName: String, size: String)
    suspend fun getAppPlanetSize(packageName: String): String?
    fun getFolders(): Flow<List<FolderData>>
    suspend fun saveFolders(folders: List<FolderData>)
    suspend fun updateFolderName(folderId: String, newName: String)
}
