package de.rr.universelauncher.presentation.universe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.theme.SpaceBackground
import de.rr.universelauncher.presentation.universe.components.UniverseCanvas
import de.rr.universelauncher.presentation.settings.LauncherSettingsScreen

@Composable
fun UniverseScreen(
    modifier: Modifier = Modifier,
    viewModel: UniverseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                UniverseCanvas(
                    orbitalSystem = uiState.orbitalSystem,
                    onPlanetTapped = viewModel::onPlanetTapped,
                    onStarTapped = viewModel::onStarTapped,
                    onCanvasSizeChanged = viewModel::updateCanvasSize,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }

        if (uiState.showSettings) {
            LauncherSettingsScreen(
                onClose = viewModel::onCloseSettings
            )
        }

    }
}

