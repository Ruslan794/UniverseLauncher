package de.rr.universelauncher.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.graphics.createBitmap
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.presentation.settings.components.AppSettingsDialog
import de.rr.universelauncher.domain.model.PlanetSize

@Composable
fun AppSelectionList(
    apps: List<AppInfo>,
    selectedApps: Set<String>,
    appOrder: Map<String, Int>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onSetPosition: (String, Int) -> Unit,
    onAppSettings: (AppInfo) -> Unit,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaxSelected = selectedApps.size >= 6

    val sortedApps = remember(apps, appOrder, selectedApps) {
        val selectedAppsList = apps.filter { it.packageName in selectedApps }
            .sortedBy { appOrder[it.packageName] ?: Int.MAX_VALUE }
        val notSelectedApps = apps.filter { it.packageName !in selectedApps }
            .sortedBy { it.appName.lowercase() }
        selectedAppsList + notSelectedApps
    }

    val filteredApps = remember(sortedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedApps
        } else {
            sortedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Selected: ${selectedApps.size}/6",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
                val isAppSelected = app.packageName in selectedApps
                val position = if (isAppSelected) appOrder[app.packageName] ?: 0 else 0
                AppSelectionItem(
                    app = app,
                    isSelected = isAppSelected,
                    position = position,
                    maxPosition = selectedApps.size,
                    isDisabled = !isAppSelected && isMaxSelected,
                    onToggle = { onToggleApp(app.packageName) },
                    onMoveUp = { onMoveUp(app.packageName) },
                    onMoveDown = { onMoveDown(app.packageName) },
                    onSetPosition = { newPos -> onSetPosition(app.packageName, newPos) },
                    onSettings = { onAppSettings(app) },
                    onLaunchApp = { onLaunchApp(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    position: Int,
    maxPosition: Int,
    isDisabled: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetPosition: (Int) -> Unit,
    onSettings: () -> Unit,
    onLaunchApp: () -> Unit
) {
    var positionInput by remember { mutableStateOf("") }

    LaunchedEffect(position) {
        positionInput = if (position > 0) position.toString() else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .clickable { onLaunchApp() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Row {

                OutlinedTextField(
                    value = positionInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty()) {
                            positionInput = ""
                        } else if (newValue.length <= 3 && newValue.all { it.isDigit() }) {
                            positionInput = newValue
                            newValue.toIntOrNull()?.let { newPos ->
                                val clampedPos = when {
                                    newPos < 1 -> 1
                                    newPos > maxPosition -> maxPosition
                                    else -> newPos
                                }
                                onSetPosition(clampedPos)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(56.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp),
                        enabled = position > 1
                    ) {
                        Text(
                            "▲",
                            color = if (position > 1) Color.White else Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }


                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp),
                        enabled = position < maxPosition
                    ) {
                        Text(
                            "▼",
                            color = if (position < maxPosition) Color.White else Color.White.copy(
                                alpha = 0.3f
                            ),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(68.dp))
        }

        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = app.appName,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Launches: ${app.launchCount}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Speed: ${(app.customOrbitSpeed ?: 30f).toInt()}s",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isSelected) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = "⚙",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = if (!isDisabled || isSelected) { _ -> onToggle() } else null,
            enabled = !isDisabled || isSelected,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color.White.copy(alpha = 0.5f),
                disabledCheckedColor = Color.White.copy(alpha = 0.3f),
                disabledUncheckedColor = Color.White.copy(alpha = 0.2f),
                checkmarkColor = Color.Black
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