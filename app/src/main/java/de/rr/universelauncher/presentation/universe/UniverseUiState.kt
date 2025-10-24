package de.rr.universelauncher.presentation.universe

import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem

data class UniverseUiState(
    val orbitalSystem: OrbitalSystem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedOrbitalBody: OrbitalBody? = null,
    val showAppDialog: Boolean = false,
    val animationTime: Float = 0f
)