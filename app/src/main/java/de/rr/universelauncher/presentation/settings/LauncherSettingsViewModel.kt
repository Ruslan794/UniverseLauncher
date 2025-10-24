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
                // Load apps with launch counts
                val allApps = appRepository.getInstalledAppsWithLaunchCounts()
                val appOrder = launcherSettingsRepository.getAppOrder().first()
                
                // Start observing selected apps changes
                launcherSettingsRepository.getSelectedApps()
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load selected apps"
                            )
                        }
                    }
                    .collect { selectedApps ->
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

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            try {
                val currentSelected = _uiState.value.selectedApps
                val currentOrder = _uiState.value.appOrder
                
                if (packageName in currentSelected) {
                    // Removing app - keep its position for when re-selected
                    val newSelected = currentSelected - packageName
                    launcherSettingsRepository.setSelectedApps(newSelected)
                } else {
                    // Adding app - check if we're at max limit (6)
                    if (currentSelected.size >= 6) {
                        _uiState.update {
                            it.copy(error = "Maximum 6 apps can be selected")
                        }
                        return@launch
                    }
                    
                    // Assign next available position
                    val nextPosition = (currentOrder.values.maxOrNull() ?: 0) + 1
                    val newOrder = currentOrder.toMutableMap()
                    newOrder[packageName] = nextPosition
                    
                    val newSelected = currentSelected + packageName
                    launcherSettingsRepository.setSelectedApps(newSelected)
                    launcherSettingsRepository.setAppOrder(newOrder)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update app selection")
                }
            }
        }
    }

    fun setCurrentTab(tab: SettingsTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun getTopUsedApps(): List<AppInfo> {
        return _uiState.value.allApps
            .sortedByDescending { it.launchCount }
            .take(10)
    }

    fun moveAppUp(packageName: String) {
        viewModelScope.launch {
            try {
                val currentOrder = launcherSettingsRepository.getAppOrder().first()
                val currentPosition = currentOrder[packageName] ?: return@launch
                
                if (currentPosition > 1) {
                    val newOrder = currentOrder.toMutableMap()
                    newOrder[packageName] = currentPosition - 1
                    
                    // Find app at position - 1 and swap
                    val otherApp = newOrder.entries.find { it.value == currentPosition - 1 }
                    otherApp?.let { 
                        newOrder[it.key] = currentPosition
                    }
                    
                    launcherSettingsRepository.setAppOrder(newOrder)
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
                val currentOrder = launcherSettingsRepository.getAppOrder().first()
                val currentPosition = currentOrder[packageName] ?: return@launch
                val maxPosition = currentOrder.values.maxOrNull() ?: 0
                
                if (currentPosition < maxPosition) {
                    val newOrder = currentOrder.toMutableMap()
                    newOrder[packageName] = currentPosition + 1
                    
                    // Find app at position + 1 and swap
                    val otherApp = newOrder.entries.find { it.value == currentPosition + 1 }
                    otherApp?.let { 
                        newOrder[it.key] = currentPosition
                    }
                    
                    launcherSettingsRepository.setAppOrder(newOrder)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to move app down")
                }
            }
        }
    }

    fun setAppPosition(packageName: String, position: Int) {
        viewModelScope.launch {
            try {
                val currentOrder = launcherSettingsRepository.getAppOrder().first().toMutableMap()
                val oldPosition = currentOrder[packageName]
                
                if (oldPosition != null) {
                    // Find app at target position and swap
                    val otherApp = currentOrder.entries.find { it.value == position }
                    otherApp?.let { 
                        currentOrder[it.key] = oldPosition
                    }
                }
                
                currentOrder[packageName] = position
                launcherSettingsRepository.setAppOrder(currentOrder)
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
                launcherSettingsRepository.setAppOrbitSpeed(packageName, speed)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to set orbit speed")
                }
            }
        }
    }

    fun getAppOrder(): Map<String, Int> {
        return _uiState.value.appOrder
    }
}
