package de.rr.universelauncher.presentation.universe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.Star
import de.rr.universelauncher.presentation.theme.SpaceBackground
import kotlin.math.*

@Composable
fun UniverseScreen(
    modifier: Modifier = Modifier,
    viewModel: UniverseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp)
    ) {
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

            uiState.orbitalSystem != null -> {
                val orbitalSystem = uiState.orbitalSystem!!

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f

                                    orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                                        val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                                            orbitalBody = orbitalBody,
                                            timeSeconds = uiState.animationTime
                                        )

                                        val screenX = centerX + position.first
                                        val screenY = centerY + position.second
                                        val planetCenter = Offset(screenX, screenY)

                                        val distance = (offset - planetCenter).getDistance()
                                        if (distance <= orbitalBody.orbitalConfig.size * 2) {
                                            viewModel.onPlanetTapped(orbitalBody)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val center = Offset(centerX, centerY)

                    drawOrbitalPaths(orbitalSystem, center)
                    drawStar(orbitalSystem.star, center)

                    orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                        val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                            orbitalBody = orbitalBody,
                            timeSeconds = uiState.animationTime
                        )

                        val screenX = centerX + position.first
                        val screenY = centerY + position.second

                        drawOrbitalBody(
                            orbitalBody = orbitalBody,
                            position = Offset(screenX, screenY)
                        )
                    }
                }
            }
        }

        if (uiState.showAppDialog && uiState.selectedOrbitalBody != null) {
            AlertDialog(
                onDismissRequest = viewModel::onDismissDialog,
                title = { Text(uiState.selectedOrbitalBody!!.appInfo.appName) },
                text = { Text("Package: ${uiState.selectedOrbitalBody!!.appInfo.packageName}") },
                confirmButton = {
                    Button(onClick = viewModel::onLaunchApp) {
                        Text("Launch")
                    }
                },
                dismissButton = {
                    Button(onClick = viewModel::onDismissDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun DrawScope.drawOrbitalPaths(
    system: de.rr.universelauncher.domain.model.OrbitalSystem,
    center: Offset
) {
    system.orbitalBodies.forEach { orbitalBody ->
        val pathPoints = OrbitalPhysics.calculateOrbitPathPoints(orbitalBody)

        if (pathPoints.isNotEmpty()) {
            val path = Path()
            val firstPoint = pathPoints[0]
            path.moveTo(center.x + firstPoint.first, center.y + firstPoint.second)

            for (i in 1 until pathPoints.size) {
                val point = pathPoints[i]
                path.lineTo(center.x + point.first, center.y + point.second)
            }

            path.close()

            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawStar(
    star: Star,
    center: Offset
) {
    drawCircle(
        color = star.color.copy(alpha = 0.3f),
        radius = star.radius * 2,
        center = center
    )

    drawCircle(
        color = star.color,
        radius = star.radius,
        center = center
    )
}

private fun DrawScope.drawOrbitalBody(
    orbitalBody: OrbitalBody,
    position: Offset
) {
    val config = orbitalBody.orbitalConfig

    drawCircle(
        color = config.color.copy(alpha = 0.2f),
        radius = config.size * 1.5f,
        center = position
    )

    drawCircle(
        color = config.color,
        radius = config.size,
        center = position
    )
}
