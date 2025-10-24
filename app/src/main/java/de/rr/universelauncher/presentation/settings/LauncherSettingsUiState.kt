package de.rr.universelauncher.presentation.settings

import de.rr.universelauncher.domain.model.AppInfo

data class LauncherSettingsUiState(
    val allApps: List<AppInfo> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTab: SettingsTab = SettingsTab.APP_SELECTION
)

enum class SettingsTab {
    APP_SELECTION,
    STATISTICS
}
