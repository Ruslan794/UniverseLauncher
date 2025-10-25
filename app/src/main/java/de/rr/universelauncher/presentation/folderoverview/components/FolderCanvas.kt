package de.rr.universelauncher.presentation.folderoverview.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import de.rr.universelauncher.domain.engine.FolderRenderer
import de.rr.universelauncher.domain.model.Folder
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import android.graphics.Paint
import android.graphics.Typeface

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
    var editingText by remember { mutableStateOf("") }
    
    LaunchedEffect(editingFolderId) {
        if (editingFolderId != null) {
            val folder = folders.find { it.id == editingFolderId }
            editingText = folder?.name ?: ""
        }
    }
    

    LaunchedEffect(Unit) {
        while (true) {
            val frameTime = withFrameNanos { it }
            currentAnimationTime += 0.016f
            delay(16)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(folders) {
                detectTapGestures(
                    onTap = { offset ->
                        val FOLDER_TAP_TOLERANCE = 80f
                        val NAME_TAP_TOLERANCE = 100f
                        
                        folders.forEach { folder ->
                            val distance = (offset - folder.position).getDistance()
                            val nameY = folder.position.y + 120f + 40f
                            val nameDistance = abs(offset.y - nameY)
                            
                            if (distance <= FOLDER_TAP_TOLERANCE) {
                                onFolderTapped(folder.id)
                                return@detectTapGestures
                            } else if (nameDistance <= 20f && abs(offset.x - folder.position.x) <= 100f) {
                                onFolderNameTapped(folder.id)
                                return@detectTapGestures
                            }
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
            
            if (editingFolderId != folder.id) {
                drawFolderName(
                    drawScope = this,
                    folder = folder,
                    density = density
                )
            } else {
                drawEditingFolderName(
                    drawScope = this,
                    folder = folder,
                    editingText = editingText,
                    density = density
                )
            }
        }
    }
}

private fun drawFolderName(
    drawScope: DrawScope,
    folder: Folder,
    density: androidx.compose.ui.unit.Density
) {
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val nameY = folder.position.y + 120f + 40f
    val namePosition = Offset(folder.position.x, nameY)
    
    drawScope.drawContext.canvas.nativeCanvas.drawText(
        folder.name,
        namePosition.x,
        namePosition.y,
        textPaint
    )
}

private fun drawEditingFolderName(
    drawScope: DrawScope,
    folder: Folder,
    editingText: String,
    density: androidx.compose.ui.unit.Density
) {
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val nameY = folder.position.y + 120f + 40f
    val namePosition = Offset(folder.position.x, nameY)
    
    drawScope.drawContext.canvas.nativeCanvas.drawText(
        editingText,
        namePosition.x,
        namePosition.y,
        textPaint
    )
}
