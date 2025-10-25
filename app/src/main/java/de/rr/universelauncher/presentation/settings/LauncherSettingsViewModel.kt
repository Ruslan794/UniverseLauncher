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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherSettingsUiState())
    val uiState: StateFlow<LauncherSettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }


    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val allApps = appRepository.getInstalledAppsWithLaunchCounts()

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

                if (packageName in currentSelected) {
                    val newSelected = currentSelected - packageName
                    currentOrder.remove(packageName)

                    val sortedEntries = currentOrder.entries.sortedBy { it.value }
                    sortedEntries.forEachIndexed { index, entry ->
                        currentOrder[entry.key] = index + 1
                    }

                    launcherSettingsRepository.setSelectedApps(newSelected)
                    launcherSettingsRepository.setAppOrder(currentOrder)
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
                    launcherSettingsRepository.setSelectedApps(newSelected)
                    launcherSettingsRepository.setAppOrder(currentOrder)
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

                if (currentPosition <= 1) return@launch

                val swapPackage = currentOrder.entries.find { it.value == currentPosition - 1 }?.key
                if (swapPackage != null) {
                    currentOrder[packageName] = currentPosition - 1
                    currentOrder[swapPackage] = currentPosition
                    launcherSettingsRepository.setAppOrder(currentOrder)
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

                if (currentPosition >= maxPosition) return@launch

                val swapPackage = currentOrder.entries.find { it.value == currentPosition + 1 }?.key
                if (swapPackage != null) {
                    currentOrder[packageName] = currentPosition + 1
                    currentOrder[swapPackage] = currentPosition
                    launcherSettingsRepository.setAppOrder(currentOrder)
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

                    launcherSettingsRepository.setAppOrder(newOrder)
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
                launcherSettingsRepository.setAppOrbitSpeed(packageName, speed)
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
                launcherSettingsRepository.setAppPlanetSize(packageName, size)
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
    }
}
