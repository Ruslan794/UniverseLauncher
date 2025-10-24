package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import de.rr.universelauncher.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.core.content.ContextCompat

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
    val context = LocalContext.current

    LaunchedEffect(orbitalSystem) {
        iconCache.preloadIcons(orbitalSystem)
    }
    
    // Load sun image
    LaunchedEffect(Unit) {
        val sunDrawable = ContextCompat.getDrawable(context, R.drawable.sun)
        sunDrawable?.let {
            UniverseRenderer.sunBitmap = it.toBitmap().asImageBitmap()
        }
    }

    var currentAnimationTime by remember { mutableStateOf(0f) }
    var lastCanvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var frameCount by remember { mutableStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastFrameTime by remember { mutableStateOf(0L) }
    val density = LocalDensity.current

    // Optimized animation loop with frame synchronization
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (true) {
                val frameTime = withFrameNanos { it }
                if (lastFrameTime > 0) {
                    val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f // Convert to seconds
                    currentAnimationTime += deltaTime
                }
                lastFrameTime = frameTime
            }
        }
    }

    // Calculate center without triggering recomposition
    val center = remember { Offset.Zero }
    
    // Create orbit path cache that's independent of absolute center
    val orbitPathCache = rememberOrbitPathCache(orbitalSystem, Offset.Zero)
    
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

        val currentCanvasSize = androidx.compose.ui.geometry.Size(size.width, size.height)
        
        // Only update canvas size if change is significant (>5px difference)
        val sizeChanged = kotlin.math.abs(lastCanvasSize.width - currentCanvasSize.width) > 5f ||
                         kotlin.math.abs(lastCanvasSize.height - currentCanvasSize.height) > 5f
        
        if (sizeChanged) {
            lastCanvasSize = currentCanvasSize
            onCanvasSizeChanged(currentCanvasSize)
        }

        UniverseRenderer.drawUniverse(
            drawScope = this,
            orbitalSystem = memoizedOrbitalSystem,
            animationTime = currentAnimationTime,
            center = currentCenter,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache,
            canvasSize = currentCanvasSize
        )

        // Optimized performance logging - only every 5 seconds
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 5000) { // Log every 5 seconds
            val fps = frameCount * 1000.0 / (currentTime - lastFpsTime)
            Log.i("UniversePerformance", "FPS: ${"%.1f".format(fps)}, Planets: ${orbitalSystem.orbitalBodies.size}")
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}