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
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherSettingsRepository: LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UniverseUiState(
            orbitalSystem = emptyOrbitalSystemWithDefaultStar,
            isLoading = true,
            error = null,
            showSettings = false,
            folderId = null
        )
    )
    val uiState: StateFlow<UniverseUiState> = _uiState.asStateFlow()

    private var currentCanvasSize: androidx.compose.ui.geometry.Size =
        androidx.compose.ui.geometry.Size(1080f, 2400f)

    init {
        loadApps()
    }

    fun setFolderId(folderId: String?) {
        _uiState.update { it.copy(folderId = folderId) }
        loadApps()
    }

    private suspend fun getFolderAppPackages(folderId: String): Set<String> {
        val folders = launcherSettingsRepository.getFolders().first()
        return folders.find { it.id == folderId }?.appPackageNames ?: emptySet()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val allApps = appRepository.getInstalledAppsWithLaunchCounts()
                val currentFolderId = _uiState.value.folderId

                val finalSelectedApps = if (currentFolderId != null) {
                    val folderSelectedApps = launcherSettingsRepository.getFolderSelectedApps(currentFolderId).first()
                    if (folderSelectedApps.isNotEmpty()) {
                        folderSelectedApps
                    } else {
                        getFolderAppPackages(currentFolderId)
                    }
                } else {
                    val selectedApps = launcherSettingsRepository.getSelectedApps().first()
                    selectedApps.ifEmpty {
                        val topApps = allApps.sortedByDescending { it.launchCount }.take(10)
                        val topAppPackages = topApps.map { it.packageName }.toSet()
                        launcherSettingsRepository.setSelectedApps(topAppPackages)
                        topAppPackages
                    }
                }

                val orbitSpeeds = if (currentFolderId != null) {
                    launcherSettingsRepository.getFolderAppOrbitSpeeds(currentFolderId).first()
                } else {
                    launcherSettingsRepository.getAppOrbitSpeeds().first()
                }

                val planetSizes = if (currentFolderId != null) {
                    launcherSettingsRepository.getFolderAppPlanetSizes(currentFolderId).first()
                } else {
                    launcherSettingsRepository.getAppPlanetSizes().first()
                }

                val filteredApps = allApps.filter { it.packageName in finalSelectedApps }.map { app ->
                    val customPlanetSize = when (planetSizes[app.packageName]) {
                        "SMALL" -> de.rr.universelauncher.domain.model.PlanetSize.SMALL
                        "MEDIUM" -> de.rr.universelauncher.domain.model.PlanetSize.MEDIUM
                        "LARGE" -> de.rr.universelauncher.domain.model.PlanetSize.LARGE
                        else -> null
                    }

                    app.copy(
                        customOrbitSpeed = orbitSpeeds[app.packageName],
                        customPlanetSize = customPlanetSize
                    )
                }

                val appOrder = if (currentFolderId != null) {
                    launcherSettingsRepository.getFolderAppOrder(currentFolderId).first()
                } else {
                    launcherSettingsRepository.getAppOrder().first()
                }

                val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(filteredApps, appOrder)
                val distributedSystem = OrbitalDistanceCalculator.distributeOrbitsInCanvas(
                    orbitalSystem,
                    currentCanvasSize
                )

                _uiState.update {
                    it.copy(
                        orbitalSystem = distributedSystem,
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

    fun onPlanetTapped(orbitalBody: OrbitalBody, planetPosition: Offset, planetSize: Float) {
        viewModelScope.launch {
            try {
                appRepository.trackAppLaunch(orbitalBody.appInfo.packageName)
            } catch (e: Exception) {
                android.util.Log.e("UniverseViewModel", "Failed to track app launch", e)
            }

            try {
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
        viewModelScope.launch {
            delay(300)
            loadApps()
        }
    }


    private suspend fun loadAppsWithSelectedApps(selectedApps: Set<String>) {
        try {
            if (selectedApps.isEmpty()) {
                android.util.Log.w("UniverseViewModel", "Attempted to load with empty selected apps")
                return
            }

            val allApps = appRepository.getInstalledAppsWithLaunchCounts()
            val filteredApps = allApps.filter { it.packageName in selectedApps }

            if (filteredApps.isEmpty()) {
                android.util.Log.w("UniverseViewModel", "No apps found for selected packages")
                return
            }

            val currentFolderId = _uiState.value.folderId
            val appOrder = if (currentFolderId != null) {
                launcherSettingsRepository.getFolderAppOrder(currentFolderId).first()
            } else {
                launcherSettingsRepository.getAppOrder().first()
            }
            val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(filteredApps, appOrder)
            val distributedSystem = OrbitalDistanceCalculator.distributeOrbitsInCanvas(
                orbitalSystem,
                currentCanvasSize
            )

            _uiState.update {
                it.copy(
                    orbitalSystem = distributedSystem,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UniverseViewModel", "Failed to load apps with selected apps", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to reload apps: ${e.message}"
                )
            }
        }
    }

    fun updateCanvasSize(canvasSize: androidx.compose.ui.geometry.Size) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return

        currentCanvasSize = canvasSize

        viewModelScope.launch {
            try {
                val currentSystem = _uiState.value.orbitalSystem
                val updatedSystem = OrbitalDistanceCalculator.distributeOrbitsInCanvas(
                    currentSystem,
                    canvasSize
                )

                _uiState.update {
                    it.copy(orbitalSystem = updatedSystem)
                }
            } catch (e: Exception) {
                android.util.Log.e("UniverseViewModel", "Failed to update canvas size", e)
            }
        }
    }
}