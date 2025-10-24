package de.rr.universelauncher.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.theme.SpaceBackground
import de.rr.universelauncher.presentation.settings.components.AppSelectionList
import de.rr.universelauncher.presentation.settings.components.AppSettingsDialog
import de.rr.universelauncher.domain.model.PlanetSize
import de.rr.universelauncher.domain.model.AppInfo

@Composable
fun LauncherSettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAppSettings by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = Color.White
                        )
                    }
                }

                else -> {
                    AppSelectionList(
                        apps = uiState.allApps,
                        selectedApps = uiState.selectedApps,
                        appOrder = uiState.appOrder,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onToggleApp = viewModel::toggleAppSelection,
                        onMoveUp = viewModel::moveAppUp,
                        onMoveDown = viewModel::moveAppDown,
                        onSetPosition = viewModel::setAppPosition,
                        onAppSettings = { app -> showAppSettings = app }
                    )
                }
            }
        }
    }

    // App Settings Dialog
    showAppSettings?.let { app ->
        AppSettingsDialog(
            app = app,
            currentSize = app.customPlanetSize ?: PlanetSize.MEDIUM,
            currentSpeed = app.customOrbitSpeed ?: 30f,
            onDismiss = { showAppSettings = null },
            onSave = { size, speed ->
                viewModel.setAppOrbitSpeed(app.packageName, speed)
                viewModel.setAppPlanetSize(app.packageName, size.name)
                showAppSettings = null
            }
        )
    }
}
