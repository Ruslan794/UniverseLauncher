package de.rr.universelauncher.presentation.settings.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.graphics.createBitmap
import de.rr.universelauncher.domain.model.AppInfo

@Composable
fun StatisticsView(
    apps: List<AppInfo>,
    topUsedApps: List<AppInfo>,
    onSetOrbitSpeed: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatisticsSummary(
                totalApps = apps.size,
                totalLaunches = apps.sumOf { it.launchCount }
            )
        }

        item {
            Text(
                text = "Top Used Apps",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        items(topUsedApps) { app ->
            AppStatisticsItem(
                app = app,
                onSetOrbitSpeed = { speed -> onSetOrbitSpeed(app.packageName, speed) }
            )
        }
    }
}

@Composable
private fun StatisticsSummary(
    totalApps: Int,
    totalLaunches: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Usage Statistics",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Apps",
                    value = totalApps.toString()
                )
                
                StatisticItem(
                    label = "Total Launches",
                    value = totalLaunches.toString()
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppStatisticsItem(
    app: AppInfo,
    onSetOrbitSpeed: (Float) -> Unit
) {
    var orbitSpeed by remember { mutableStateOf(app.customOrbitSpeed ?: 30f) }
    
    // Calculate planet size using the same logic as PlanetSizeCalculator
    val planetSize = remember(app.launchCount) {
        val baseMin = 28f
        val baseMax = 52f
        val scaleFactor = when {
            app.launchCount <= 3 -> 1.0f
            app.launchCount <= 5 -> 0.85f
            app.launchCount <= 7 -> 0.7f
            app.launchCount <= 9 -> 0.6f
            else -> 0.5f
        }
        val minRadius = baseMin * scaleFactor
        val maxRadius = baseMax * scaleFactor
        
        if (app.launchCount == 0) minRadius else {
            val normalizedValue = kotlin.math.ln((app.launchCount + 1).toFloat()) / kotlin.math.ln(100f)
            minRadius + (normalizedValue * (maxRadius - minRadius))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${app.launchCount}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "launches",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Planet size and orbit speed controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Planet size (read-only)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Planet Size",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = "${planetSize.toInt()}px",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Orbit speed slider
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Orbit Speed: ${orbitSpeed.toInt()}s",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = orbitSpeed,
                    onValueChange = { newSpeed ->
                        orbitSpeed = newSpeed
                        onSetOrbitSpeed(newSpeed)
                    },
                    valueRange = 10f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
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
