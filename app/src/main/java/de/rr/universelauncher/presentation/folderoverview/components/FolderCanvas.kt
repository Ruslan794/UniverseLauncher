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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

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
    var currentAnimationTime by remember { mutableStateOf(0f) }
    var lastScreenSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val density = LocalDensity.current
    var editingTexts by remember { mutableStateOf(mapOf<String, String>()) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

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
            val frameTime = withFrameNanos { it }
            currentAnimationTime += 0.016f
            delay(16)
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
                                val distance = (offset - folder.position).getDistance()
                                val nameY = folder.position.y + 160f
                                val nameDistance = abs(offset.y - nameY)

                                if (distance <= FOLDER_TAP_TOLERANCE) {
                                    onFolderTapped(folder.id)
                                    return@detectTapGestures
                                } else if (nameDistance <= 30f && abs(offset.x - folder.position.x) <= 120f) {
                                    onFolderNameTapped(folder.id)
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
            val currentScreenSize = androidx.compose.ui.geometry.Size(size.width, size.height)

            val sizeChanged = abs(lastScreenSize.width - currentScreenSize.width) > 5f ||
                    abs(lastScreenSize.height - currentScreenSize.height) > 5f

            if (sizeChanged) {
                lastScreenSize = currentScreenSize
                onScreenSizeChanged(currentScreenSize)
            }

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
            val nameY = folder.position.y + 160f

            if (editingFolderId == folder.id) {
                val focusRequester = focusRequesters.getOrPut(folder.id) { FocusRequester() }

                BasicTextField(
                    value = editingTexts[folder.id] ?: folder.name,
                    onValueChange = { newValue ->
                        editingTexts = editingTexts + (folder.id to newValue)
                    },
                    modifier = Modifier
                        .offset(
                            x = with(density) { (folder.position.x - 100f).toDp() },
                            y = with(density) { (nameY - 20f).toDp() }
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            innerTextField()
                        }
                    }
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (folder.position.x - 100f).toDp() },
                            y = with(density) { (nameY - 20f).toDp() }
                        )
                        .width(200.dp)
                ) {
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 32f
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        folder.name,
                        size.width / 2,
                        20f,
                        textPaint
                    )
                }
            }
        }
    }
}