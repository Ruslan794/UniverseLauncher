package de.rr.universelauncher.presentation.settings

import de.rr.universelauncher.domain.model.AppInfo

data class LauncherSettingsUiState(
    val allApps: List<AppInfo> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val appOrder: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)