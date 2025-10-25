package de.rr.universelauncher.presentation.universe

import de.rr.universelauncher.domain.model.OrbitalSystem

data class UniverseUiState(
    val orbitalSystem: OrbitalSystem,
    val isLoading: Boolean,
    val error: String?,
    val showSettings: Boolean,
    val folderId: String?
)