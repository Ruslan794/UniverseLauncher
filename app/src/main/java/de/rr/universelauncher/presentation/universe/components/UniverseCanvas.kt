package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay
import android.util.Log

@Composable
fun UniverseCanvas(
    orbitalSystem: OrbitalSystem,
    onPlanetTapped: (OrbitalBody, Offset, Float) -> Unit,
    onStarTapped: () -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.geometry.Size) -> Unit = {},
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconCache = rememberIconCache(orbitalSystem)

    LaunchedEffect(orbitalSystem) {
        iconCache.preloadIcons(orbitalSystem)
    }

    var currentAnimationTime by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var lastCanvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var frameCount by remember { mutableStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val density = LocalDensity.current

    // Use a more efficient animation approach
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (true) {
                delay(16) // 60 FPS
                currentAnimationTime += 0.016f
            }
        }
    }

    val orbitPathCache = rememberOrbitPathCache(orbitalSystem, center)
    
    // Memoize expensive calculations
    val memoizedOrbitalSystem = remember(orbitalSystem) { orbitalSystem }

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

        val renderStartTime = System.nanoTime()
        
        UniverseRenderer.drawUniverse(
            drawScope = this,
            orbitalSystem = memoizedOrbitalSystem,
            animationTime = currentAnimationTime,
            center = currentCenter,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache,
            canvasSize = currentCanvasSize
        )
        
        val renderEndTime = System.nanoTime()
        val renderTimeMs = (renderEndTime - renderStartTime) / 1_000_000.0
        
        // Performance logging - only log every 3 seconds to reduce overhead
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 3000) { // Log every 3 seconds
            val fps = frameCount * 1000.0 / (currentTime - lastFpsTime)
            Log.i("UniversePerformance", "FPS: ${"%.1f".format(fps)}, Render: ${"%.2f".format(renderTimeMs)}ms, Planets: ${orbitalSystem.orbitalBodies.size}")
            frameCount = 0
            lastFpsTime = currentTime
        }
        
        // Log slow frames only if significantly slow
        if (renderTimeMs > 8.0) {
            Log.w("UniversePerformance", "Slow frame: ${"%.2f".format(renderTimeMs)}ms")
        }
    }
}