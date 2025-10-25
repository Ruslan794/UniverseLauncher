package de.rr.universelauncher.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.theme.SpaceBackground
import de.rr.universelauncher.presentation.settings.components.AppSelectionList
import de.rr.universelauncher.presentation.settings.components.AppSettingsDialog
import de.rr.universelauncher.domain.model.PlanetSize

@Composable
fun LauncherSettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    folderId: String? = null,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAppSettings by remember { mutableStateOf<de.rr.universelauncher.domain.model.AppInfo?>(null) }

    LaunchedEffect(folderId) {
        viewModel.setFolderId(folderId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Einstellungen",
                    fontSize = 24.sp,
                    color = Color.White
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                onAppSettings = { app -> showAppSettings = app },
                onLaunchApp = viewModel::launchApp
            )
        }
    }

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