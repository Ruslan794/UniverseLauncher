package de.rr.universelauncher.presentation.folderoverview.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rr.universelauncher.domain.engine.FolderRenderer
import de.rr.universelauncher.domain.model.Folder
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size

@Composable
fun FolderCanvas(
    folders: List<Folder>,
    onFolderTapped: (String) -> Unit,
    onFolderNameTapped: (String) -> Unit = {},
    onScreenSizeChanged: (androidx.compose.ui.geometry.Size) -> Unit = {},
    editingFolderId: String? = null,
    onUpdateFolderName: (String, String) -> Unit = { _, _ -> },
    onCancelEditing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentAnimationTime by remember { mutableFloatStateOf(0f) }
    var lastScreenSize by remember { mutableStateOf(Size.Zero) }
    var currentScreenSize by remember { mutableStateOf(Size.Zero) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    val density = LocalDensity.current
    var editingTexts by remember { mutableStateOf(mapOf<String, String>()) }
    val focusRequesters = remember(editingFolderId) {
        if (editingFolderId != null) {
            mutableMapOf<String, FocusRequester>().apply {
                put(editingFolderId, FocusRequester())
            }
        } else {
            mutableMapOf()
        }
    }

    LaunchedEffect(editingFolderId) {
        if (editingFolderId != null) {
            val folder = folders.find { it.id == editingFolderId }
            if (folder != null) {
                editingTexts = editingTexts + (editingFolderId to folder.name)
                delay(100)
                focusRequesters.getOrPut(editingFolderId) { FocusRequester() }.requestFocus()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime > 0) {
                    val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f
                    currentAnimationTime += deltaTime
                }
                lastFrameTime = frameTime
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(folders) {
                    detectTapGestures(
                        onTap = { offset ->
                            val FOLDER_TAP_TOLERANCE = 100f

                            folders.forEach { folder ->
                                val planetCount = folder.appPackageNames.size
                                val lastOrbitRadius = 100f + ((planetCount - 1) * 50f)
                                val nameY = folder.position.y + lastOrbitRadius + 40f

                                if (offset.y >= nameY && offset.y <= nameY + 30f &&
                                    offset.x >= folder.position.x - 100f &&
                                    offset.x <= folder.position.x + 100f) {
                                    onFolderNameTapped(folder.id)
                                    return@detectTapGestures
                                }

                                val distance = (offset - folder.position).getDistance()
                                if (distance <= FOLDER_TAP_TOLERANCE) {
                                    onFolderTapped(folder.id)
                                    return@detectTapGestures
                                }
                            }

                            if (editingFolderId != null) {
                                onCancelEditing()
                            }
                        }
                    )
                }
        ) {
            currentScreenSize = androidx.compose.ui.geometry.Size(size.width, size.height)

            val sizeChanged = abs(lastScreenSize.width - currentScreenSize.width) > 5f ||
                    abs(lastScreenSize.height - currentScreenSize.height) > 5f

            if (sizeChanged) {
                lastScreenSize = currentScreenSize
                onScreenSizeChanged(currentScreenSize)
            }

            val clipRect = androidx.compose.ui.geometry.Rect(
                offset = androidx.compose.ui.geometry.Offset.Zero,
                size = currentScreenSize
            )
            
            drawContext.canvas.clipRect(clipRect)

            folders.forEach { folder ->
                FolderRenderer.drawFolder(
                    drawScope = this,
                    folder = folder,
                    animationTime = currentAnimationTime,
                    planetCount = folder.appPackageNames.size
                )
            }
        }

        folders.forEach { folder ->
            val planetCount = folder.appPackageNames.size
            val lastOrbitRadius = 100f + ((planetCount - 1) * 50f)
            val nameY = folder.position.y + lastOrbitRadius + 40f
            
            val canvasWidth = currentScreenSize.width
            val canvasHeight = currentScreenSize.height
            
            val textX = if (canvasWidth >= 200f) {
                (folder.position.x - 100f).coerceIn(0f, canvasWidth - 200f)
            } else {
                (folder.position.x - 100f).coerceIn(0f, canvasWidth)
            }
            val textY = if (canvasHeight >= 30f) {
                nameY.coerceIn(0f, canvasHeight - 30f)
            } else {
                nameY.coerceIn(0f, canvasHeight)
            }

            if (editingFolderId == folder.id) {
                val focusRequester = focusRequesters.getOrPut(folder.id) { FocusRequester() }

                BasicTextField(
                    value = editingTexts[folder.id] ?: folder.name,
                    onValueChange = { newValue ->
                        editingTexts = editingTexts + (folder.id to newValue)
                    },
                    modifier = Modifier
                        .offset(
                            x = with(density) { textX.toDp() },
                            y = with(density) { textY.toDp() }
                        )
                        .width(200.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && editingFolderId == folder.id) {
                                val newName = editingTexts[folder.id] ?: folder.name
                                if (newName.isNotBlank() && newName != folder.name) {
                                    onUpdateFolderName(folder.id, newName)
                                } else {
                                    onCancelEditing()
                                }
                            }
                        },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true
                )
            } else {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(
                            x = with(density) { textX.toDp() },
                            y = with(density) { textY.toDp() }
                        )
                        .width(200.dp)
                )
            }
        }
    }
}