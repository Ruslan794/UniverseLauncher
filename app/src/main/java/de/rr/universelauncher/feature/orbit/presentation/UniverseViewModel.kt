package de.rr.universelauncher.feature.orbit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.core.launcher.domain.usecase.GetInstalledAppsUseCase
import de.rr.universelauncher.core.launcher.domain.usecase.LaunchAppUseCase
import de.rr.universelauncher.core.physics.domain.engine.OrbitalPhysics
import de.rr.universelauncher.core.physics.domain.model.OrbitalBody
import de.rr.universelauncher.core.physics.domain.model.OrbitalSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UniverseUiState(
    val orbitalSystem: OrbitalSystem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedOrbitalBody: OrbitalBody? = null,
    val showAppDialog: Boolean = false,
    val animationTime: Float = 0f
)

@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniverseUiState())
    val uiState: StateFlow<UniverseUiState> = _uiState.asStateFlow()

    private var animationJob: Job? = null

    init {
        loadApps()
        startAnimation()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val apps = getInstalledAppsUseCase()
                val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(apps)
                _uiState.value = _uiState.value.copy(
                    orbitalSystem = orbitalSystem,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load apps"
                )
            }
        }
    }

    private fun startAnimation() {
        animationJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60fps
                _uiState.value = _uiState.value.copy(
                    animationTime = _uiState.value.animationTime + 0.016f
                )
            }
        }
    }

    fun onPlanetTapped(orbitalBody: OrbitalBody) {
        _uiState.value = _uiState.value.copy(
            selectedOrbitalBody = orbitalBody,
            showAppDialog = true
        )
    }

    fun onLaunchApp() {
        val selectedBody = _uiState.value.selectedOrbitalBody
        if (selectedBody != null) {
            launchAppUseCase(selectedBody.appInfo.packageName)
            onDismissDialog()
        }
    }

    fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(
            selectedOrbitalBody = null,
            showAppDialog = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        animationJob?.cancel()
    }
}
