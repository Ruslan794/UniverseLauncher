package de.rr.universelauncher.presentation.folderoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.folderoverview.components.FolderCanvas
import de.rr.universelauncher.R

@Composable
fun FolderOverviewScreen(
    modifier: Modifier = Modifier,
    viewModel: FolderOverviewViewModel = hiltViewModel(),
    onFolderSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                FolderCanvas(
                    folders = uiState.folders,
                    onFolderTapped = { folderId ->
                        onFolderSelected(folderId)
                    },
                    onFolderNameTapped = { folderId ->
                        viewModel.onFolderNameTapped(folderId)
                    },
                    onScreenSizeChanged = viewModel::updateScreenSize,
                    editingFolderId = uiState.editingFolderId,
                    onUpdateFolderName = { folderId, newName ->
                        viewModel.updateFolderName(folderId, newName)
                    },
                    onCancelEditing = {
                        viewModel.cancelEditing()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
    }
}
