package de.rr.universelauncher.presentation.universe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.manager.AppDataManager
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.engine.OrbitalDistanceCalculator
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.emptyOrbitalSystemWithDefaultStar
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appDataManager: AppDataManager
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

    private var folderIdJob: Job? = null

    init {
        folderIdJob = viewModelScope.launch {
            combine(
                appDataManager.allApps,
                appDataManager.selectedApps,
                appDataManager.appOrder,
                appDataManager.orbitSpeeds,
                appDataManager.planetSizes
            ) { arr ->
                val apps = arr[0] as List<de.rr.universelauncher.domain.model.AppInfo>
                val selected = arr[1] as Set<String>
                val order = arr[2] as Map<String, Int>
                val speeds = arr[3] as Map<String, Float>
                val sizes = arr[4] as Map<String, String>
                buildOrbitalSystem(apps, selected, order, speeds, sizes, null)
            }.collect { orbitalSystem ->
                _uiState.update {
                    it.copy(
                        orbitalSystem = orbitalSystem,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun setFolderId(folderId: String?) {
        _uiState.update { it.copy(folderId = folderId) }
        
        folderIdJob?.cancel()
        folderIdJob = if (folderId != null) {
            viewModelScope.launch {
                combine(
                    appDataManager.allApps,
                    appDataManager.folderSelectedApps,
                    appDataManager.folderAppOrders,
                    appDataManager.folderOrbitSpeeds,
                    appDataManager.folderPlanetSizes,
                    _uiState
                ) { arr ->
                    val apps = arr[0] as List<de.rr.universelauncher.domain.model.AppInfo>
                    val folderSelected = arr[1] as Map<String, Set<String>>
                    val folderOrders = arr[2] as Map<String, Map<String, Int>>
                    val folderSpeeds = arr[3] as Map<String, Map<String, Float>>
                    val folderSizes = arr[4] as Map<String, Map<String, String>>
                    val state = arr[5] as UniverseUiState
                    val selected = folderSelected[state.folderId] ?: emptySet()
                    val order = folderOrders[state.folderId] ?: emptyMap()
                    val speeds = folderSpeeds[state.folderId] ?: emptyMap()
                    val sizes = folderSizes[state.folderId] ?: emptyMap()
                    buildOrbitalSystem(apps, selected, order, speeds, sizes, state.folderId)
                }.collect { orbitalSystem ->
                    _uiState.update {
                        it.copy(
                            orbitalSystem = orbitalSystem,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        } else {
            viewModelScope.launch {
                combine(
                    appDataManager.allApps,
                    appDataManager.selectedApps,
                    appDataManager.appOrder,
                    appDataManager.orbitSpeeds,
                    appDataManager.planetSizes
                ) { arr ->
                    val apps = arr[0] as List<de.rr.universelauncher.domain.model.AppInfo>
                    val selected = arr[1] as Set<String>
                    val order = arr[2] as Map<String, Int>
                    val speeds = arr[3] as Map<String, Float>
                    val sizes = arr[4] as Map<String, String>
                    buildOrbitalSystem(apps, selected, order, speeds, sizes, null)
                }.collect { orbitalSystem ->
                    _uiState.update {
                        it.copy(
                            orbitalSystem = orbitalSystem,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun buildOrbitalSystem(
        allApps: List<de.rr.universelauncher.domain.model.AppInfo>,
        selectedApps: Set<String>,
        appOrder: Map<String, Int>,
        orbitSpeeds: Map<String, Float>,
        planetSizes: Map<String, String>,
        folderId: String?
    ): OrbitalSystem {
        val finalSelectedApps = selectedApps.ifEmpty {
            val topApps = allApps.sortedByDescending { it.launchCount }.take(10)
            val topAppPackages = topApps.map { it.packageName }.toSet()
            viewModelScope.launch {
                appDataManager.setSelectedApps(topAppPackages)
            }
            topAppPackages
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

        val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(filteredApps, appOrder)
        return OrbitalDistanceCalculator.distributeOrbitsInCanvas(
            orbitalSystem,
            currentCanvasSize
        )
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
    }

    private var canvasSizeUpdateJob: Job? = null

    fun updateCanvasSize(canvasSize: androidx.compose.ui.geometry.Size) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return

        currentCanvasSize = canvasSize

        canvasSizeUpdateJob?.cancel()
        canvasSizeUpdateJob = viewModelScope.launch {
            try {
                val currentSystem = _uiState.value.orbitalSystem
                if (currentSystem.orbitalBodies.isEmpty()) return@launch

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