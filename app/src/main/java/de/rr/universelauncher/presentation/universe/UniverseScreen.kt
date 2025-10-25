package de.rr.universelauncher.presentation.universe

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.theme.SpaceBackground
import de.rr.universelauncher.presentation.universe.components.UniverseCanvas
import de.rr.universelauncher.presentation.settings.LauncherSettingsScreen
import de.rr.universelauncher.R

@Composable
fun UniverseScreen(
    modifier: Modifier = Modifier,
    viewModel: UniverseViewModel = hiltViewModel(),
    folderId: String? = null,
    onBackPressed: () -> Unit = {},
    onShowFolderOverview: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(folderId) {
        viewModel.setFolderId(folderId)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.stars_background),
            contentDescription = "Stars background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
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
                    onPlanetTapped = { orbitalBody, position, size -> 
                        viewModel.onPlanetTapped(orbitalBody, position, size)
                    },
                    onStarTapped = viewModel::onStarTapped,
                    onCanvasSizeChanged = viewModel::updateCanvasSize,
                    isPaused = uiState.showSettings,
                    onSwipeFromLeft = onShowFolderOverview,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }

        if (uiState.showSettings) {
            LauncherSettingsScreen(
                onClose = viewModel::onCloseSettings,
                folderId = uiState.folderId
            )
        }

    }
}

