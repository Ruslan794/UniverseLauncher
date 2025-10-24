package de.rr.universelauncher.presentation.universe.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.model.OrbitalBody
import androidx.core.graphics.createBitmap

@Composable
fun AppList(
    apps: List<AppInfo>,
    selectedApp: AppInfo?,
    selectedOrbitalBody: OrbitalBody?,
    onIncreaseSize: () -> Unit,
    onDecreaseSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Color.Black.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            item {
                Column(
                    modifier = modifier
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {

                    Text(
                        text = selectedApp?.appName ?: "No app selected",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedOrbitalBody != null) {
                        Text(
                            text = "Size: ${selectedOrbitalBody.orbitalConfig.size.toInt()}",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Speed: ${selectedOrbitalBody.orbitalConfig.orbitDuration.toInt()}s",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = onIncreaseSize,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Increase Size")
                            }

                            Button(
                                onClick = onDecreaseSpeed,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Decrease Speed")
                            }
                        }
                    } else {
                        Text(
                            text = "No planet selected",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            items(apps) { app ->
                AppListItem(
                    app = app,
                )
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = app.appName,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        val bitmap = createBitmap(64, 64)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, 64, 64)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }
}