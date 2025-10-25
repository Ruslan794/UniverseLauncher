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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherSettingsUiState())
    val uiState: StateFlow<LauncherSettingsUiState> = _uiState.asStateFlow()

    fun setFolderId(folderId: String?) {
        _uiState.update { it.copy(folderId = folderId) }
        loadData()
    }

    init {
        loadData()
    }


    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val allApps = appRepository.getInstalledAppsWithLaunchCounts()
                val currentFolderId = _uiState.value.folderId

                if (currentFolderId != null) {
                    combine(
                        launcherSettingsRepository.getFolderSelectedApps(currentFolderId),
                        launcherSettingsRepository.getFolderAppOrder(currentFolderId)
                    ) { selectedApps, appOrder ->
                        Pair(selectedApps, appOrder)
                    }
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Failed to load folder settings"
                                )
                            }
                        }
                        .take(1)
                        .collect { (selectedApps, appOrder) ->
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
                } else {
                    combine(
                        launcherSettingsRepository.getSelectedApps(),
                        launcherSettingsRepository.getAppOrder()
                    ) { selectedApps, appOrder ->
                        Pair(selectedApps, appOrder)
                    }
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Failed to load selected apps"
                                )
                            }
                        }
                        .take(1)
                        .collect { (selectedApps, appOrder) ->
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
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set app position")
                }
            }
        }
    }

    fun getSelectedAppsWithStats(): List<AppInfo> {
        val selectedPackages = _uiState.value.selectedApps
        return _uiState.value.allApps.filter { it.packageName in selectedPackages }
    }

    fun getTopUsedApps(): List<AppInfo> {
        return _uiState.value.allApps
            .sortedByDescending { it.launchCount }
            .take(10)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set planet size")
                }
            }
        }
    }

    fun getAppOrder(): Map<String, Int> {
        return _uiState.value.appOrder
    }

    override fun onCleared() {
        super.onCleared()
        loadDataJob?.cancel()
    }
}
