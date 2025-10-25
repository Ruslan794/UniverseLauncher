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
import de.rr.universelauncher.domain.engine.RenderingConstants
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import de.rr.universelauncher.presentation.universe.components.cache.IconCache
import de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache
import kotlin.math.abs
import kotlin.math.min

@Composable
fun UniverseCanvas(
    orbitalSystem: OrbitalSystem,
    onPlanetTapped: (OrbitalBody, Offset, Float) -> Unit,
    onStarTapped: () -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.geometry.Size) -> Unit = {},
    isPaused: Boolean = false,
    onSwipeFromLeft: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val orbitalSystemId = remember(orbitalSystem.orbitalBodies.map { it.appInfo.packageName }) {
        orbitalSystem.orbitalBodies.map { it.appInfo.packageName }.hashCode()
    }

    val iconCache = remember(orbitalSystemId) {
        IconCache()
    }

    LaunchedEffect(orbitalSystemId) {
        iconCache.clearCache()
        iconCache.preloadIcons(orbitalSystem)
        PlanetRenderingEngine.clearAnglesForSystem(
            orbitalSystem.orbitalBodies.map { it.appInfo.packageName }
        )
    }

    var currentAnimationTime by remember { mutableStateOf(0f) }
    var lastCanvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    val PHASE1_DURATION = 100L
    val PHASE2_DURATION = 400L
    val PHASE3_DURATION = 800L
    val MAX_SPEED_MULTIPLIER = 10f
    val SPEED_STACK_FACTOR = 2f

    var speedMultiplier by remember { mutableStateOf(1f) }
    var isSpeedBoostActive by remember { mutableStateOf(false) }
    var currentMaxSpeed by remember { mutableStateOf(MAX_SPEED_MULTIPLIER) }

    LaunchedEffect(isSpeedBoostActive) {
        if (!isSpeedBoostActive) return@LaunchedEffect

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

    val orbitPathCache = remember(orbitalSystemId) {
        OrbitPathCache.createRelative(orbitalSystem)
    }

    fun startSpeedBoost() {
        if (isSpeedBoostActive) {
            val additionalSpeed = (currentMaxSpeed - 1f) * SPEED_STACK_FACTOR
            currentMaxSpeed += additionalSpeed
        } else {
            isSpeedBoostActive = true
        }
    }

    val planetHitboxData = remember(orbitalSystemId) {
        orbitalSystem.orbitalBodies.map { it.appInfo.packageName }
    }

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(orbitalSystemId) {
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
                                val planetKey = orbitalBody.appInfo.packageName

                                val currentAngle = PlanetRenderingEngine.getPlanetAngle(planetKey) ?: (config.startAngle * kotlin.math.PI / 180f)

                                val cosAngle = kotlin.math.cos(currentAngle).toFloat()
                                val sinAngle = kotlin.math.sin(currentAngle).toFloat()

                                val radiusX = orbitDistance * config.ellipseRatio
                                val radiusY = orbitDistance

                                val offsetX = radiusX * cosAngle
                                val offsetY = radiusY * sinAngle

                                val tiltAngle = RenderingConstants.ORBIT_TILT_ANGLE * kotlin.math.PI / 180.0
                                val cosTilt = kotlin.math.cos(tiltAngle).toFloat()
                                val sinTilt = kotlin.math.sin(tiltAngle).toFloat()

                                val rotatedX = offsetX * cosTilt - offsetY * sinTilt
                                val rotatedY = offsetX * sinTilt + offsetY * cosTilt

                                val x = centerX + rotatedX
                                val y = centerY + rotatedY
                                val planetCenter = Offset(x, y)

                                val basePlanetRadius = sizeCalculation.sizeLookup[orbitalBody.orbitalConfig.sizeCategory]
                                    ?: sizeCalculation.sizeLookup[de.rr.universelauncher.domain.model.PlanetSize.MEDIUM]!!

                                val depthScale = PlanetRenderingEngine.calculateDepthScale(currentAngle)
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
            .pointerInput(Unit) {
                var dragStartPosition: Offset? = null
                var isSwipeFromRight = false
                var isSwipeFromLeft = false
                val DRAG_THRESHOLD = 20f

                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width / 2f

                        isSwipeFromRight = offset.x > centerX
                        isSwipeFromLeft = offset.x < centerX

                        if (isSwipeFromRight || isSwipeFromLeft) {
                            dragStartPosition = offset
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragStartPosition != null) {
                            val deltaX = change.position.x - dragStartPosition!!.x
                            val deltaY = change.position.y - dragStartPosition!!.y

                            if (isSwipeFromLeft && deltaX > 100f && abs(deltaY) < 100f) {
                                onSwipeFromLeft()
                                isSwipeFromLeft = false
                            } else if (isSwipeFromRight && deltaY > 50f) {
                                startSpeedBoost()
                                isSwipeFromRight = false
                            }
                        }
                    },
                    onDragEnd = {
                        if (dragStartPosition != null) {
                            val centerX = size.width / 2f
                            val offset = dragStartPosition!!
                            val totalDragDistance = (offset - dragStartPosition!!).getDistance()

                            val isRightOfCenter = offset.x > centerX

                            if (totalDragDistance < DRAG_THRESHOLD && isRightOfCenter) {
                                startSpeedBoost()
                            }
                        }

                        dragStartPosition = null
                        isSwipeFromRight = false
                        isSwipeFromLeft = false
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
            orbitalSystem = orbitalSystem,
            animationTime = currentAnimationTime,
            center = currentCenter,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache,
            canvasSize = currentCanvasSize,
            speedMultiplier = speedMultiplier
        )
    }
}