package de.rr.universelauncher.presentation.universe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.repository.LauncherSettingsRepository
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.engine.OrbitalDistanceCalculator
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.emptyOrbitalSystemWithDefaultStar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniverseUiState(
        orbitalSystem = emptyOrbitalSystemWithDefaultStar,
        isLoading = true,
        error = null,
        showSettings = false
    ))
    val uiState: StateFlow<UniverseUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        observeSettingsChanges()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Get all apps with launch counts
                val allApps = appRepository.getInstalledAppsWithLaunchCounts()
                
                // Get selected apps from settings
                val selectedApps = launcherSettingsRepository.getSelectedApps().first()
                
                // If no apps are selected, initialize with top 10 most used apps
                val finalSelectedApps = if (selectedApps.isEmpty()) {
                    val topApps = allApps.sortedByDescending { it.launchCount }.take(10)
                    val topAppPackages = topApps.map { it.packageName }.toSet()
                    launcherSettingsRepository.setSelectedApps(topAppPackages)
                    topAppPackages
                } else {
                    selectedApps
                }
                
                // Filter apps to show only selected ones
                val filteredApps = allApps.filter { it.packageName in finalSelectedApps }

                val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(filteredApps)
                _uiState.update {
                    it.copy(
                        orbitalSystem = orbitalSystem,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, error = e.message ?: "Failed to load apps"
                    )
                }
            }
        }
    }


    fun onPlanetTapped(orbitalBody: OrbitalBody) {
        viewModelScope.launch {
            try {
                appRepository.trackAppLaunch(orbitalBody.appInfo.packageName)
                appRepository.launchApp(orbitalBody.appInfo.packageName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to launch app: ${e.message}")
                }
            }
        }
    }

    fun onStarTapped() {
        _uiState.update {
            it.copy(showSettings = true)
        }
    }

    fun onCloseSettings() {
        _uiState.update {
            it.copy(showSettings = false)
        }
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            launcherSettingsRepository.getSelectedApps()
                .catch { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to observe settings changes")
                    }
                }
                .collect { selectedApps ->
                    // Reload apps when selected apps change
                    loadAppsWithSelectedApps(selectedApps)
                }
        }
    }

    private suspend fun loadAppsWithSelectedApps(selectedApps: Set<String>) {
        try {
            val allApps = appRepository.getInstalledAppsWithLaunchCounts()
            val filteredApps = allApps.filter { it.packageName in selectedApps }
            val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(filteredApps)
            
            _uiState.update {
                it.copy(
                    orbitalSystem = orbitalSystem,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load apps"
                )
            }
        }
    }

    fun updateCanvasSize(canvasSize: androidx.compose.ui.geometry.Size) {
        viewModelScope.launch {
            try {
                val currentSystem = _uiState.value.orbitalSystem
                val updatedSystem = OrbitalDistanceCalculator.distributeOrbitsInCanvas(currentSystem, canvasSize)
                
                _uiState.update {
                    it.copy(orbitalSystem = updatedSystem)
                }
            } catch (e: Exception) {
                // Log error but don't break the app
                android.util.Log.e("UniverseViewModel", "Failed to update canvas size", e)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
    }

}
