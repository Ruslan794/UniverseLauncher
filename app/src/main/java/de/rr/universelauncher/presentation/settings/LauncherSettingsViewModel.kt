package de.rr.universelauncher.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.repository.LauncherSettingsRepository
import de.rr.universelauncher.domain.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherSettingsUiState())
    val uiState: StateFlow<LauncherSettingsUiState> = _uiState.asStateFlow()

    private var cachedAllApps: List<AppInfo>? = null
    private var loadDataJob: Job? = null

    fun setFolderId(folderId: String?) {
        _uiState.update { it.copy(folderId = folderId) }
        loadData()
    }

    init {
        loadData()
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val allApps = cachedAllApps ?: appRepository.getInstalledAppsWithLaunchCounts().also {
                    cachedAllApps = it
                }

                val currentFolderId = _uiState.value.folderId

                if (currentFolderId != null) {
                    val folderData = launcherSettingsRepository.getFolders().first()
                    val folder = folderData.find { it.id == currentFolderId }
                    val defaultFolderApps = folder?.appPackageNames ?: emptySet()

                    var selectedApps = launcherSettingsRepository.getFolderSelectedApps(currentFolderId).first()
                    var appOrder = launcherSettingsRepository.getFolderAppOrder(currentFolderId).first()

                    if (selectedApps.isEmpty() && defaultFolderApps.isNotEmpty()) {
                        selectedApps = defaultFolderApps
                        launcherSettingsRepository.setFolderSelectedApps(currentFolderId, selectedApps)

                        val initialOrder = selectedApps.mapIndexed { index, packageName ->
                            packageName to (index + 1)
                        }.toMap()
                        appOrder = initialOrder
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, appOrder)
                    }

                    _uiState.update {
                        it.copy(
                            allApps = allApps,
                            selectedApps = selectedApps,
                            appOrder = appOrder,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    var selectedApps = launcherSettingsRepository.getSelectedApps().first()
                    var appOrder = launcherSettingsRepository.getAppOrder().first()

                    if (selectedApps.isEmpty()) {
                        val topApps = allApps.sortedByDescending { it.launchCount }.take(6)
                        selectedApps = topApps.map { it.packageName }.toSet()
                        launcherSettingsRepository.setSelectedApps(selectedApps)

                        val initialOrder = topApps.mapIndexed { index, app ->
                            app.packageName to (index + 1)
                        }.toMap()
                        appOrder = initialOrder
                        launcherSettingsRepository.setAppOrder(appOrder)
                    }

                    _uiState.update {
                        it.copy(
                            allApps = allApps,
                            selectedApps = selectedApps,
                            appOrder = appOrder,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load data"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            try {
                val currentSelected = _uiState.value.selectedApps
                val currentOrder = _uiState.value.appOrder.toMutableMap()
                val currentFolderId = _uiState.value.folderId

                if (packageName in currentSelected) {
                    val newSelected = currentSelected - packageName
                    currentOrder.remove(packageName)

                    val sortedEntries = currentOrder.entries.sortedBy { it.value }
                    sortedEntries.forEachIndexed { index, entry ->
                        currentOrder[entry.key] = index + 1
                    }

                    if (currentFolderId != null) {
                        launcherSettingsRepository.setFolderSelectedApps(currentFolderId, newSelected)
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        launcherSettingsRepository.setSelectedApps(newSelected)
                        launcherSettingsRepository.setAppOrder(currentOrder)
                    }

                    _uiState.update {
                        it.copy(
                            selectedApps = newSelected,
                            appOrder = currentOrder,
                            error = null
                        )
                    }
                } else {
                    if (currentSelected.size >= 6) {
                        _uiState.update {
                            it.copy(error = "Maximum 6 apps can be selected")
                        }
                        return@launch
                    }

                    val nextPosition = (currentOrder.values.maxOrNull() ?: 0) + 1
                    currentOrder[packageName] = nextPosition

                    val newSelected = currentSelected + packageName
                    if (currentFolderId != null) {
                        launcherSettingsRepository.setFolderSelectedApps(currentFolderId, newSelected)
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        launcherSettingsRepository.setSelectedApps(newSelected)
                        launcherSettingsRepository.setAppOrder(currentOrder)
                    }

                    _uiState.update {
                        it.copy(
                            selectedApps = newSelected,
                            appOrder = currentOrder,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update app selection")
                }
            }
        }
    }

    fun moveAppUp(packageName: String) {
        viewModelScope.launch {
            try {
                val currentOrder = _uiState.value.appOrder.toMutableMap()
                val currentPosition = currentOrder[packageName] ?: return@launch
                val currentFolderId = _uiState.value.folderId

                if (currentPosition <= 1) return@launch

                val swapPackage = currentOrder.entries.find { it.value == currentPosition - 1 }?.key
                if (swapPackage != null) {
                    currentOrder[packageName] = currentPosition - 1
                    currentOrder[swapPackage] = currentPosition
                    if (currentFolderId != null) {
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        launcherSettingsRepository.setAppOrder(currentOrder)
                    }

                    _uiState.update {
                        it.copy(appOrder = currentOrder)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to move app up")
                }
            }
        }
    }

    fun moveAppDown(packageName: String) {
        viewModelScope.launch {
            try {
                val currentOrder = _uiState.value.appOrder.toMutableMap()
                val currentPosition = currentOrder[packageName] ?: return@launch
                val maxPosition = currentOrder.values.maxOrNull() ?: return@launch
                val currentFolderId = _uiState.value.folderId

                if (currentPosition >= maxPosition) return@launch

                val swapPackage = currentOrder.entries.find { it.value == currentPosition + 1 }?.key
                if (swapPackage != null) {
                    currentOrder[packageName] = currentPosition + 1
                    currentOrder[swapPackage] = currentPosition
                    if (currentFolderId != null) {
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        launcherSettingsRepository.setAppOrder(currentOrder)
                    }

                    _uiState.update {
                        it.copy(appOrder = currentOrder)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to move app down")
                }
            }
        }
    }

    fun setAppPosition(packageName: String, newPosition: Int) {
        viewModelScope.launch {
            try {
                val currentOrder = _uiState.value.appOrder.toMutableMap()
                val oldPosition = currentOrder[packageName] ?: return@launch
                val currentFolderId = _uiState.value.folderId

                if (oldPosition == newPosition) return@launch

                val maxPosition = currentOrder.size
                val clampedPosition = newPosition.coerceIn(1, maxPosition)

                val sortedEntries = currentOrder.entries
                    .sortedBy { it.value }
                    .toMutableList()

                val movingEntry = sortedEntries.find { it.key == packageName }
                if (movingEntry != null) {
                    sortedEntries.remove(movingEntry)
                    sortedEntries.add(clampedPosition - 1, movingEntry)

                    val newOrder = mutableMapOf<String, Int>()
                    sortedEntries.forEachIndexed { index, entry ->
                        newOrder[entry.key] = index + 1
                    }

                    if (currentFolderId != null) {
                        launcherSettingsRepository.setFolderAppOrder(currentFolderId, newOrder)
                    } else {
                        launcherSettingsRepository.setAppOrder(newOrder)
                    }

                    _uiState.update {
                        it.copy(appOrder = newOrder)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set app position")
                }
            }
        }
    }

    fun setAppOrbitSpeed(packageName: String, speed: Float) {
        viewModelScope.launch {
            try {
                val currentFolderId = _uiState.value.folderId
                if (currentFolderId != null) {
                    launcherSettingsRepository.setFolderAppOrbitSpeed(currentFolderId, packageName, speed)
                } else {
                    launcherSettingsRepository.setAppOrbitSpeed(packageName, speed)
                }

                val allApps = _uiState.value.allApps
                val updatedApps = allApps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(customOrbitSpeed = speed)
                    } else {
                        app
                    }
                }
                _uiState.update {
                    it.copy(allApps = updatedApps)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set orbit speed")
                }
            }
        }
    }

    fun setAppPlanetSize(packageName: String, size: String) {
        viewModelScope.launch {
            try {
                val currentFolderId = _uiState.value.folderId
                if (currentFolderId != null) {
                    launcherSettingsRepository.setFolderAppPlanetSize(currentFolderId, packageName, size)
                } else {
                    launcherSettingsRepository.setAppPlanetSize(packageName, size)
                }

                val planetSize = when (size) {
                    "SMALL" -> de.rr.universelauncher.domain.model.PlanetSize.SMALL
                    "MEDIUM" -> de.rr.universelauncher.domain.model.PlanetSize.MEDIUM
                    "LARGE" -> de.rr.universelauncher.domain.model.PlanetSize.LARGE
                    else -> null
                }

                val allApps = _uiState.value.allApps
                val updatedApps = allApps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(customPlanetSize = planetSize)
                    } else {
                        app
                    }
                }
                _uiState.update {
                    it.copy(allApps = updatedApps)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set planet size")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadDataJob?.cancel()
    }
}