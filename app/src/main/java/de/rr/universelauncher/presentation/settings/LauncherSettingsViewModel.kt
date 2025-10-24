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
                val newSelected = if (packageName in currentSelected) {
                    currentSelected - packageName
                } else {
                    currentSelected + packageName
                }
                
                // Update repository first, then UI will be updated via Flow
                launcherSettingsRepository.setSelectedApps(newSelected)
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
}
