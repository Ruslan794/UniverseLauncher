package de.rr.universelauncher.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.graphics.createBitmap
import de.rr.universelauncher.domain.model.AppInfo

@Composable
fun AppSelectionList(
    apps: List<AppInfo>,
    selectedApps: Set<String>,
    appOrder: Map<String, Int>,
    onToggleApp: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onSetPosition: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaxSelected = selectedApps.size >= 6
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps) { app ->
            val isAppSelected = app.packageName in selectedApps
            AppSelectionItem(
                app = app,
                isSelected = isAppSelected,
                position = appOrder[app.packageName] ?: 0,
                isDisabled = !isAppSelected && isMaxSelected,
                onToggle = { onToggleApp(app.packageName) },
                onMoveUp = { onMoveUp(app.packageName) },
                onMoveDown = { onMoveDown(app.packageName) },
                onSetPosition = { position -> onSetPosition(app.packageName, position) }
            )
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    position: Int,
    isDisabled: Boolean = false,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetPosition: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reordering controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            // Up arrow
            Button(
                onClick = onMoveUp,
                modifier = Modifier.size(24.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text("↑", color = Color.White, fontSize = 12.sp)
            }
            
            // Position number
            OutlinedTextField(
                value = position.toString(),
                onValueChange = { 
                    it.toIntOrNull()?.let { newPos -> 
                        if (newPos > 0) onSetPosition(newPos)
                    }
                },
                modifier = Modifier
                    .width(40.dp)
                    .height(32.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                singleLine = true
            )
            
            // Down arrow
            Button(
                onClick = onMoveDown,
                modifier = Modifier.size(24.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text("↓", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // App icon and info
        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = app.appName,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Selection checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = if (isDisabled) null else { _ -> onToggle() },
            enabled = !isDisabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color.White.copy(alpha = 0.5f),
                disabledCheckedColor = Color.White.copy(alpha = 0.3f),
                disabledUncheckedColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun rememberDrawablePainter(drawable: android.graphics.drawable.Drawable): Painter {
    return remember(drawable) {
        val bitmap = createBitmap(64, 64)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, 64, 64)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }
}
