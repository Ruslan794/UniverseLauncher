package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay

@Composable
fun UniverseCanvas(
    orbitalSystem: OrbitalSystem,
    onPlanetTapped: (OrbitalBody, Offset, Float) -> Unit,
    onStarTapped: () -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.geometry.Size) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconCache = rememberIconCache(orbitalSystem)

    LaunchedEffect(orbitalSystem) {
        iconCache.preloadIcons(orbitalSystem)
    }

    var currentAnimationTime by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var lastCanvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            currentAnimationTime += 0.016f
        }
    }

    val orbitPathCache = rememberOrbitPathCache(orbitalSystem, center)

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(orbitalSystem) {
                detectTapGestures(
                    onTap = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val center = Offset(centerX, centerY)

                        val distanceFromCenter = (offset - center).getDistance()
                        if (distanceFromCenter <= orbitalSystem.star.radius * 1.5f) {
                            onStarTapped()
                            return@detectTapGestures
                        }

                        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                            val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                                orbitalBody = orbitalBody,
                                timeSeconds = currentAnimationTime
                            )

                            val screenX = centerX + position.first
                            val screenY = centerY + position.second
                            val planetCenter = Offset(screenX, screenY)

                            val distance = (offset - planetCenter).getDistance()
                            val planetRadius = 20f // Default radius for tap detection

                            if (distance <= planetRadius * 1.2f) {
                                onPlanetTapped(orbitalBody, planetCenter, planetRadius)
                                return@detectTapGestures
                            }
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val currentCenter = Offset(centerX, centerY)

        if (center != currentCenter) {
            center = currentCenter
        }

        val currentCanvasSize = androidx.compose.ui.geometry.Size(size.width, size.height)
        if (lastCanvasSize != currentCanvasSize) {
            lastCanvasSize = currentCanvasSize
            onCanvasSizeChanged(currentCanvasSize)
        }

        UniverseRenderer.drawUniverse(
            drawScope = this,
            orbitalSystem = orbitalSystem,
            animationTime = currentAnimationTime,
            center = currentCenter,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache,
            canvasSize = currentCanvasSize
        )
    }
}