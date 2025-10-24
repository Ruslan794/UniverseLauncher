package de.rr.universelauncher.presentation.universe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.engine.OrbitalDistanceCalculator
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.model.emptyOrbitalSystemWithDefaultStar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniverseUiState(
        orbitalSystem = emptyOrbitalSystemWithDefaultStar,
        allApps = emptyList(),
        isLoading = true,
        error = null,
        selectedOrbitalBody = null,
        showAppDialog = false,
        selectedAppInfo = null,
        selectedPlanetIndex = null
    ))
    val uiState: StateFlow<UniverseUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val apps = appRepository.getInstalledApps()

                val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(apps.take(5))
                _uiState.update {
                    it.copy(
                        orbitalSystem = orbitalSystem,
                        allApps = apps,
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
        val currentSystem = _uiState.value.orbitalSystem
        val planetIndex = currentSystem?.orbitalBodies?.indexOf(orbitalBody) ?: -1

        _uiState.update {
            it.copy(
                selectedOrbitalBody = orbitalBody,
                showAppDialog = true,
                selectedAppInfo = orbitalBody.appInfo,
                selectedPlanetIndex = planetIndex
            )
        }
    }

    fun incrementSelectedPlanetSize() {
        val currentSystem = _uiState.value.orbitalSystem
        val planetIndex = _uiState.value.selectedPlanetIndex

        if (currentSystem != null && planetIndex != null && planetIndex >= 0) {
            val currentPlanet = currentSystem.orbitalBodies[planetIndex]
            val newSize = currentPlanet.orbitalConfig.size + 5f

            val updatedSystem = OrbitalDistanceCalculator.updatePlanetSizeAndRecalculate(
                currentSystem, planetIndex, newSize
            )
            _uiState.update {
                it.copy(orbitalSystem = updatedSystem)
            }
        }
    }

    fun onLaunchApp() {
        val selectedBody = _uiState.value.selectedOrbitalBody
        if (selectedBody != null) {
            appRepository.launchApp(selectedBody.appInfo.packageName)
            onDismissDialog()
        }
    }

    fun onDismissDialog() {
        _uiState.update {
            it.copy(
                selectedOrbitalBody = null, showAppDialog = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // No animation job to cancel anymore
    }

}
