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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.presentation.folderoverview.components.FolderCanvas
import de.rr.universelauncher.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun FolderOverviewScreen(
    modifier: Modifier = Modifier,
    viewModel: FolderOverviewViewModel = hiltViewModel(),
    onFolderSelected: (String) -> Unit,
    reloadTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            currentTime = timeFormat.format(now.time)
            currentDate = dateFormat.format(now.time)
            delay(1000)
        }
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            viewModel.reloadFolders()
        }
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


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = currentDate,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Error loading folders",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            uiState.folders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No folders available",
                        color = Color.White,
                        fontSize = 18.sp
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            }
        }
    }
}