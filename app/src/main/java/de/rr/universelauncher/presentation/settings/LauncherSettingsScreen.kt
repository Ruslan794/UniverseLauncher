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
import de.rr.universelauncher.presentation.settings.components.StatisticsView

@Composable
fun LauncherSettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launcher Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Text("Close", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = uiState.currentTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                SettingsTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.setCurrentTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    SettingsTab.APP_SELECTION -> "App Selection"
                                    SettingsTab.STATISTICS -> "Statistics"
                                },
                                color = Color.White
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    when (uiState.currentTab) {
                        SettingsTab.APP_SELECTION -> {
                            AppSelectionList(
                                apps = uiState.allApps,
                                selectedApps = uiState.selectedApps,
                                appOrder = uiState.appOrder,
                                onToggleApp = viewModel::toggleAppSelection,
                                onMoveUp = viewModel::moveAppUp,
                                onMoveDown = viewModel::moveAppDown,
                                onSetPosition = viewModel::setAppPosition
                            )
                        }
                        SettingsTab.STATISTICS -> {
                            StatisticsView(
                                apps = uiState.allApps,
                                topUsedApps = viewModel.getTopUsedApps(),
                                onSetOrbitSpeed = viewModel::setAppOrbitSpeed
                            )
                        }
                    }
                }
            }
        }
    }
}
