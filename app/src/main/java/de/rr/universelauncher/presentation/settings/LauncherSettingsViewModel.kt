package de.rr.universelauncher.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.manager.AppDataManager
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val appDataManager: AppDataManager,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherSettingsUiState())
    val uiState: StateFlow<LauncherSettingsUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null

    fun setFolderId(folderId: String?) {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            if (folderId != null) {
                combine(
                    appDataManager.allApps,
                    appDataManager.folderSelectedApps,
                    appDataManager.folderAppOrders
                ) { allApps, folderSelected, folderOrders ->
                    val selected = folderSelected[folderId] ?: emptySet()
                    val order = folderOrders[folderId] ?: emptyMap()
                    Triple(allApps, selected, order)
                }.collect { (allApps, selected, order) ->
                    _uiState.update {
                        it.copy(
                            allApps = allApps,
                            selectedApps = selected,
                            appOrder = order,
                            folderId = folderId,
                            isLoading = false
                        )
                    }
                }
            } else {
                combine(
                    appDataManager.allApps,
                    appDataManager.selectedApps,
                    appDataManager.appOrder
                ) { allApps, selected, order ->
                    Triple(allApps, selected, order)
                }.collect { (allApps, selected, order) ->
                    _uiState.update {
                        it.copy(
                            allApps = allApps,
                            selectedApps = selected,
                            appOrder = order,
                            folderId = null,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.trackAppLaunch(packageName)
                appRepository.launchApp(packageName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to launch app: ${e.message}")
                }
            }
        }
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
                        appDataManager.setFolderSelectedApps(currentFolderId, newSelected)
                        appDataManager.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        appDataManager.setSelectedApps(newSelected)
                        appDataManager.setAppOrder(currentOrder)
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
                        appDataManager.setFolderSelectedApps(currentFolderId, newSelected)
                        appDataManager.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        appDataManager.setSelectedApps(newSelected)
                        appDataManager.setAppOrder(currentOrder)
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
                        appDataManager.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        appDataManager.setAppOrder(currentOrder)
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
                        appDataManager.setFolderAppOrder(currentFolderId, currentOrder)
                    } else {
                        appDataManager.setAppOrder(currentOrder)
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
                        appDataManager.setFolderAppOrder(currentFolderId, newOrder)
                    } else {
                        appDataManager.setAppOrder(newOrder)
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
                    appDataManager.setFolderAppOrbitSpeed(currentFolderId, packageName, speed)
                } else {
                    appDataManager.setAppOrbitSpeed(packageName, speed)
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
                    appDataManager.setFolderAppPlanetSize(currentFolderId, packageName, size)
                } else {
                    appDataManager.setAppPlanetSize(packageName, size)
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