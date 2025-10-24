package de.rr.universelauncher.presentation.universe

import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem

data class UniverseUiState(
    val orbitalSystem: OrbitalSystem,
    val allApps: List<AppInfo>,
    val isLoading: Boolean,
    val error: String?,
    val selectedOrbitalBody: OrbitalBody?,
    val showAppDialog: Boolean
)