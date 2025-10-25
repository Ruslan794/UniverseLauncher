package de.rr.universelauncher.domain.manager

import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.repository.LauncherSettingsRepository
import de.rr.universelauncher.domain.model.AppInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataManager @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob())

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _appOrder = MutableStateFlow<Map<String, Int>>(emptyMap())
    val appOrder: StateFlow<Map<String, Int>> = _appOrder.asStateFlow()

    private val _orbitSpeeds = MutableStateFlow<Map<String, Float>>(emptyMap())
    val orbitSpeeds: StateFlow<Map<String, Float>> = _orbitSpeeds.asStateFlow()

    private val _planetSizes = MutableStateFlow<Map<String, String>>(emptyMap())
    val planetSizes: StateFlow<Map<String, String>> = _planetSizes.asStateFlow()

    private val _folderSelectedApps = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val folderSelectedApps: StateFlow<Map<String, Set<String>>> = _folderSelectedApps.asStateFlow()

    private val _folderAppOrders = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val folderAppOrders: StateFlow<Map<String, Map<String, Int>>> = _folderAppOrders.asStateFlow()

    private val _folderOrbitSpeeds = MutableStateFlow<Map<String, Map<String, Float>>>(emptyMap())
    val folderOrbitSpeeds: StateFlow<Map<String, Map<String, Float>>> = _folderOrbitSpeeds.asStateFlow()

    private val _folderPlanetSizes = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val folderPlanetSizes: StateFlow<Map<String, Map<String, String>>> = _folderPlanetSizes.asStateFlow()

    private val _folders = MutableStateFlow<List<de.rr.universelauncher.domain.model.FolderData>>(emptyList())
    val folders: StateFlow<List<de.rr.universelauncher.domain.model.FolderData>> = _folders.asStateFlow()

    suspend fun preloadData() {
        val apps = appRepository.getInstalledAppsWithLaunchCounts()
        _allApps.value = apps

        _selectedApps.value = launcherSettingsRepository.getSelectedApps().first()
        _appOrder.value = launcherSettingsRepository.getAppOrder().first()
        _orbitSpeeds.value = launcherSettingsRepository.getAppOrbitSpeeds().first()
        _planetSizes.value = launcherSettingsRepository.getAppPlanetSizes().first()

        val folderData = launcherSettingsRepository.getFolders().first()
        _folders.value = folderData

        val folderSelectedMap = mutableMapOf<String, Set<String>>()
        val folderOrdersMap = mutableMapOf<String, Map<String, Int>>()
        val folderSpeedsMap = mutableMapOf<String, Map<String, Float>>()
        val folderSizesMap = mutableMapOf<String, Map<String, String>>()

        folderData.forEach { folder ->
            folderSelectedMap[folder.id] = launcherSettingsRepository.getFolderSelectedApps(folder.id).first()
            folderOrdersMap[folder.id] = launcherSettingsRepository.getFolderAppOrder(folder.id).first()
            folderSpeedsMap[folder.id] = launcherSettingsRepository.getFolderAppOrbitSpeeds(folder.id).first()
            folderSizesMap[folder.id] = launcherSettingsRepository.getFolderAppPlanetSizes(folder.id).first()
        }

        _folderSelectedApps.value = folderSelectedMap
        _folderAppOrders.value = folderOrdersMap
        _folderOrbitSpeeds.value = folderSpeedsMap
        _folderPlanetSizes.value = folderSizesMap
    }

    suspend fun setSelectedApps(apps: Set<String>) {
        launcherSettingsRepository.setSelectedApps(apps)
        _selectedApps.value = apps
    }

    suspend fun setAppOrder(appOrder: Map<String, Int>) {
        launcherSettingsRepository.setAppOrder(appOrder)
        _appOrder.value = appOrder
    }

    suspend fun setAppOrbitSpeed(packageName: String, speed: Float) {
        launcherSettingsRepository.setAppOrbitSpeed(packageName, speed)
        _orbitSpeeds.value = _orbitSpeeds.value.toMutableMap().apply { put(packageName, speed) }
    }

    suspend fun setAppPlanetSize(packageName: String, size: String) {
        launcherSettingsRepository.setAppPlanetSize(packageName, size)
        _planetSizes.value = _planetSizes.value.toMutableMap().apply { put(packageName, size) }
    }

    suspend fun setFolderSelectedApps(folderId: String, apps: Set<String>) {
        launcherSettingsRepository.setFolderSelectedApps(folderId, apps)
        _folderSelectedApps.value = _folderSelectedApps.value.toMutableMap().apply { put(folderId, apps) }
    }

    suspend fun setFolderAppOrder(folderId: String, appOrder: Map<String, Int>) {
        launcherSettingsRepository.setFolderAppOrder(folderId, appOrder)
        _folderAppOrders.value = _folderAppOrders.value.toMutableMap().apply { put(folderId, appOrder) }
    }

    suspend fun setFolderAppOrbitSpeed(folderId: String, packageName: String, speed: Float) {
        launcherSettingsRepository.setFolderAppOrbitSpeed(folderId, packageName, speed)
        val folderSpeeds = _folderOrbitSpeeds.value.toMutableMap()
        val speeds = folderSpeeds[folderId]?.toMutableMap() ?: mutableMapOf()
        speeds[packageName] = speed
        folderSpeeds[folderId] = speeds
        _folderOrbitSpeeds.value = folderSpeeds
    }

    suspend fun setFolderAppPlanetSize(folderId: String, packageName: String, size: String) {
        launcherSettingsRepository.setFolderAppPlanetSize(folderId, packageName, size)
        val folderSizes = _folderPlanetSizes.value.toMutableMap()
        val sizes = folderSizes[folderId]?.toMutableMap() ?: mutableMapOf()
        sizes[packageName] = size
        folderSizes[folderId] = sizes
        _folderPlanetSizes.value = folderSizes
    }

    suspend fun saveFolders(folders: List<de.rr.universelauncher.domain.model.FolderData>) {
        launcherSettingsRepository.saveFolders(folders)
        _folders.value = folders
    }

    suspend fun updateFolderName(folderId: String, newName: String) {
        launcherSettingsRepository.updateFolderName(folderId, newName)
        _folders.value = _folders.value.map { if (it.id == folderId) it.copy(name = newName) else it }
    }

    fun clearSettingsSearch() {
    }
}
