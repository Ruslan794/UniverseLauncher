package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.engine.PlanetRenderingEngine
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.min

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
    var lastCanvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var frameCount by remember { mutableStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastFrameTime by remember { mutableStateOf(0L) }
    val density = LocalDensity.current

    val PHASE1_DURATION = 100L
    val PHASE2_DURATION = 400L
    val PHASE3_DURATION = 1000L
    val MAX_SPEED_MULTIPLIER = 25f
    val SPEED_STACK_FACTOR = 2f

    var speedMultiplier by remember { mutableStateOf(1f) }
    var isSpeedBoostActive by remember { mutableStateOf(false) }
    var currentMaxSpeed by remember { mutableStateOf(MAX_SPEED_MULTIPLIER) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isSpeedBoostActive) {
                delay(100)
                continue
            }

            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < PHASE1_DURATION) {
                val progress = (System.currentTimeMillis() - startTime).toFloat() / PHASE1_DURATION
                speedMultiplier = 1f + ((currentMaxSpeed - 1f) * progress)
                delay(16)
            }

            speedMultiplier = currentMaxSpeed
            delay(PHASE2_DURATION)

            val decelStartTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - decelStartTime < PHASE3_DURATION) {
                val progress = (System.currentTimeMillis() - decelStartTime).toFloat() / PHASE3_DURATION
                speedMultiplier = currentMaxSpeed - ((currentMaxSpeed - 1f) * progress)
                delay(16)
            }

            speedMultiplier = 1f
            isSpeedBoostActive = false
            currentMaxSpeed = MAX_SPEED_MULTIPLIER
        }
    }

    fun startSpeedBoost() {
        if (isSpeedBoostActive) {
            val additionalSpeed = (currentMaxSpeed - 1f) * SPEED_STACK_FACTOR
            currentMaxSpeed += additionalSpeed
        } else {
            isSpeedBoostActive = true
        }
    }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (true) {
                val frameTime = withFrameNanos { it }
                if (lastFrameTime > 0) {
                    val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f
                    currentAnimationTime += deltaTime * speedMultiplier
                }
                lastFrameTime = frameTime
            }
        }
    }

    val center = remember { Offset.Zero }

    val orbitPathCache = rememberOrbitPathCache(orbitalSystem, Offset.Zero)

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
                        val PLANET_TAP_TOLERANCE = 1.8f

                        val distanceFromCenter = (offset - center).getDistance()
                        if (distanceFromCenter <= orbitalSystem.star.radius * 1.5f) {
                            onStarTapped()
                        } else {
                            val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                            val canvasAnalysis = PlanetRenderingEngine.analyzeCanvas(canvasSize, orbitalSystem.star)
                            val sizeCalculation = PlanetRenderingEngine.calculateSizes(orbitalSystem.orbitalBodies, canvasAnalysis)

                            orbitalSystem.orbitalBodies.forEachIndexed { index, orbitalBody ->
                                val orbitDistance = canvasAnalysis.minOffset + 
                                    (index * sizeCalculation.radialSlotSize) + 
                                    sizeCalculation.radialSlotSize / 2f
                                
                                val config = orbitalBody.orbitalConfig
                                val baseOrbitDuration = orbitalBody.appInfo.customOrbitSpeed ?: config.orbitDuration
                                val planetKey = orbitalBody.appInfo.packageName
                                
                                val baseAngularVelocity = 2 * kotlin.math.PI / baseOrbitDuration
                                val deltaTime = 0.016f * speedMultiplier
                                
                                val currentAngle = PlanetRenderingEngine.getPlanetAngle(planetKey) ?: (config.startAngle * kotlin.math.PI / 180f)
                                val depthSpeed = PlanetRenderingEngine.calculateDepthSpeed(currentAngle)
                                val newAngle = currentAngle + baseAngularVelocity * depthSpeed * deltaTime
                                PlanetRenderingEngine.setPlanetAngle(planetKey, newAngle)
                                
                                val cosAngle = kotlin.math.cos(newAngle).toFloat()
                                val sinAngle = kotlin.math.sin(newAngle).toFloat()
                                
                                val x = centerX + orbitDistance * cosAngle * config.ellipseRatio
                                val y = centerY + orbitDistance * sinAngle
                                val planetCenter = Offset(x, y)

                                val basePlanetRadius = sizeCalculation.sizeLookup[orbitalBody.orbitalConfig.sizeCategory]
                                    ?: sizeCalculation.sizeLookup[de.rr.universelauncher.domain.model.PlanetSize.MEDIUM]!!
                                
                                val depthScale = PlanetRenderingEngine.calculateDepthScale(newAngle)
                                val planetRadius = basePlanetRadius * depthScale

                                val distance = (offset - planetCenter).getDistance()

                                if (distance <= planetRadius * PLANET_TAP_TOLERANCE) {
                                    onPlanetTapped(orbitalBody, planetCenter, planetRadius)
                                    return@forEachIndexed
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(orbitalSystem) {
                var dragStartPosition: Offset? = null
                var isSwipeFromRight = false
                var lastTapTime = 0L
                var lastTapPosition: Offset? = null
                val DRAG_THRESHOLD = 20f

                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        isSwipeFromRight = offset.x > centerX

                        if (isSwipeFromRight) {
                            dragStartPosition = offset
                        }
                    },
                    onDrag = { change, _ ->
                        if (isSwipeFromRight && dragStartPosition != null) {
                            val totalDeltaY = change.position.y - dragStartPosition!!.y

                            if (totalDeltaY > 50f) {
                                startSpeedBoost()
                                isSwipeFromRight = false
                            }
                        }
                    },
                    onDragEnd = {
                        if (dragStartPosition != null) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val offset = dragStartPosition!!
                            val totalDragDistance = (offset - dragStartPosition!!).getDistance()

                            val currentTime = System.currentTimeMillis()
                            val isRightOfCenter = offset.x > centerX

                            if (totalDragDistance < DRAG_THRESHOLD) {
                                if (isRightOfCenter && lastTapPosition != null &&
                                    lastTapTime > 0 && (currentTime - lastTapTime) < 500 &&
                                    (offset - lastTapPosition!!).getDistance() < 50f) {
                                    startSpeedBoost()
                                } else if (isRightOfCenter) {
                                    lastTapTime = currentTime
                                    lastTapPosition = offset
                                }
                            }
                        }

                        dragStartPosition = null
                        isSwipeFromRight = false
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val currentCenter = Offset(centerX, centerY)

        val currentCanvasSize = androidx.compose.ui.geometry.Size(size.width, size.height)

        val sizeChanged = abs(lastCanvasSize.width - currentCanvasSize.width) > 5f ||
                abs(lastCanvasSize.height - currentCanvasSize.height) > 5f

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
            canvasSize = currentCanvasSize,
            speedMultiplier = speedMultiplier
        )

        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 5000) {
            val fps = frameCount * 1000.0 / (currentTime - lastFpsTime)
            Log.i("UniversePerformance", "FPS: ${"%.1f".format(fps)}, Planets: ${orbitalSystem.orbitalBodies.size}")
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}