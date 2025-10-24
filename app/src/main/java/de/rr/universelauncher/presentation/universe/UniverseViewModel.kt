package de.rr.universelauncher.presentation.universe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
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
                _uiState.update { it.copy(isLoading = true, error = null) }
                val apps = appRepository.getInstalledApps()
                val orbitalSystem = OrbitalPhysics.createOrbitalSystemFromApps(apps)
                _uiState.update {
                    it.copy(orbitalSystem = orbitalSystem, isLoading = false)
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

    private fun startAnimation() {
        animationJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60fps
                _uiState.update {
                    it.copy(animationTime = it.animationTime + 0.016f)
                }
            }
        }
    }

    fun onPlanetTapped(orbitalBody: OrbitalBody) {
        _uiState.update {
            it.copy(
                selectedOrbitalBody = orbitalBody, showAppDialog = true
            )
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
        animationJob?.cancel()
    }

}
