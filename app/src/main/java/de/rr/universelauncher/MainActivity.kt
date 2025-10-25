package de.rr.universelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import de.rr.universelauncher.presentation.universe.UniverseScreen
import de.rr.universelauncher.presentation.folderoverview.FolderOverviewScreen
import de.rr.universelauncher.presentation.theme.UniverseLauncherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            UniverseLauncherTheme {
                var showFolderOverview by remember { mutableStateOf(false) }
                var selectedFolderId by remember { mutableStateOf<String?>(null) }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        UniverseScreen(
                            modifier = Modifier
                                .padding(innerPadding)
                                .alpha(if (showFolderOverview) 0f else 1f)
                                .animateContentSize(
                                    animationSpec = tween(300)
                                ),
                            folderId = selectedFolderId,
                            onBackPressed = {
                                selectedFolderId = null
                            },
                            onShowFolderOverview = {
                                showFolderOverview = true
                            }
                        )
                        
                        AnimatedVisibility(
                            visible = showFolderOverview,
                            enter = slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(300)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            )
                        ) {
                            FolderOverviewScreen(
                                modifier = Modifier.padding(innerPadding),
                                onFolderSelected = { folderId ->
                                    selectedFolderId = folderId
                                    showFolderOverview = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrbitScreenPreview() {
    UniverseLauncherTheme {
        UniverseScreen()
    }
}