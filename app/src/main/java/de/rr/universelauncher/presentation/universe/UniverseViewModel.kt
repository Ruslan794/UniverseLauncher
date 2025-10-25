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
import androidx.compose.ui.geometry.Size
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
    private var dataCollectionJob: Job? = null
    private var canvasSizeUpdateJob: Job? = null


    init {
        loadData(null)
    }

    private fun loadData(folderId: String?) {
        dataCollectionJob?.cancel()
        dataCollectionJob = if (folderId != null) {
            viewModelScope.launch {
                combine(
                    appDataManager.allApps,
                    appDataManager.folderSelectedApps,
                    appDataManager.folderAppOrders,
                    appDataManager.folderOrbitSpeeds,
                    appDataManager.folderPlanetSizes
                ) { allApps, folderSelected, folderOrders, folderSpeeds, folderSizes ->
                    val selected = folderSelected[folderId] ?: emptySet()
                    val order = folderOrders[folderId] ?: emptyMap()
                    val speeds = folderSpeeds[folderId] ?: emptyMap()
                    val sizes = folderSizes[folderId] ?: emptyMap()
                    buildOrbitalSystem(allApps, selected, order, speeds, sizes, folderId)
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
                ) { allApps, selected, order, speeds, sizes ->
                    buildOrbitalSystem(allApps, selected, order, speeds, sizes, null)
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


    fun setFolderId(folderId: String?) {
        _uiState.update { it.copy(folderId = folderId) }
        loadData(folderId)
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

    fun updateCanvasSize(canvasSize: Size) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        currentCanvasSize = canvasSize

        canvasSizeUpdateJob?.cancel()
        canvasSizeUpdateJob = viewModelScope.launch {
            kotlinx.coroutines.delay(100)
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

    override fun onCleared() {
        super.onCleared()
        dataCollectionJob?.cancel()
        canvasSizeUpdateJob?.cancel()
    }
}