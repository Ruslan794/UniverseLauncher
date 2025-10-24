package de.rr.universelauncher.presentation.universe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import de.rr.universelauncher.presentation.universe.components.AppList
import de.rr.universelauncher.presentation.universe.components.UniverseCanvas

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

            uiState.orbitalSystem != null -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    UniverseCanvas(
                        orbitalSystem = uiState.orbitalSystem!!,
                        animationTime = uiState.animationTime,
                        onPlanetTapped = viewModel::onPlanetTapped,
                        modifier = Modifier.weight(0.7f)
                    )

                    AppList(
                        apps = uiState.allApps,
                        selectedApp = uiState.selectedAppInfo,
                        onClick = viewModel::incrementSelectedPlanetSize,
                        modifier = Modifier.weight(0.3f)
                    )
                }
            }
        }

        if (uiState.showAppDialog && uiState.selectedOrbitalBody != null) {
            AlertDialog(
                onDismissRequest = viewModel::onDismissDialog,
                title = { Text(uiState.selectedOrbitalBody!!.appInfo.appName) },
                text = { Text("Package: ${uiState.selectedOrbitalBody!!.appInfo.packageName}") },
                confirmButton = {
                    Button(onClick = viewModel::onLaunchApp) {
                        Text("Launch")
                    }
                },
                dismissButton = {
                    Button(onClick = viewModel::onDismissDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

