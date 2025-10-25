package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.Star
import de.rr.universelauncher.domain.model.PlanetSize
import de.rr.universelauncher.presentation.universe.components.cache.IconCache
import kotlin.math.*
import android.util.Log

object PlanetRenderingEngine {

    private val planetAngles = mutableMapOf<String, Double>()
    
    fun getPlanetAngle(planetKey: String): Double? = planetAngles[planetKey]
    
    fun setPlanetAngle(planetKey: String, angle: Double) {
        planetAngles[planetKey] = angle
    }
    
    fun calculateDepthScale(angle: Double): Float {
        val sinAngle = sin(angle)
        val minScale = RenderingConstants.DEPTH_MIN_SCALE
        val maxScale = RenderingConstants.DEPTH_MAX_SCALE
        return minScale + (sinAngle.toFloat() + 1f) * (maxScale - minScale) / 2f
    }
    
    fun calculateDepthSpeed(angle: Double): Float {
        val sinAngle = sin(angle)
        val minSpeed = RenderingConstants.DEPTH_MIN_SPEED
        val maxSpeed = RenderingConstants.DEPTH_MAX_SPEED
        return minSpeed + (sinAngle.toFloat() + 1f) * (maxSpeed - minSpeed) / 2f
    }

    data class CanvasAnalysis(
        val canvasRadius: Float,
        val center: Offset,
        val minOffset: Float,
        val maxOffset: Float,
        val availableRadius: Float
    )

    data class SizeCalculation(
        val planetCount: Int,
        val radialSlotSize: Float,
        val maxPlanetRadius: Float,
        val sizeLookup: Map<PlanetSize, Float>
    )

    fun analyzeCanvas(canvasSize: Size, star: Star): CanvasAnalysis {
        val canvasRadius = min(canvasSize.width, canvasSize.height) / 2f
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        
        val minOffset = star.radius + star.deadZone
        val maxOffset = canvasRadius - RenderingConstants.GLOBAL_PADDING
        val availableRadius = maxOffset - minOffset
        
        return CanvasAnalysis(canvasRadius, center, minOffset, maxOffset, availableRadius)
    }

    fun calculateSizes(orbitalBodies: List<OrbitalBody>, canvasAnalysis: CanvasAnalysis): SizeCalculation {
        val planetCount = orbitalBodies.size
        val radialSlotSize = canvasAnalysis.availableRadius / planetCount
        val maxPlanetRadius = (radialSlotSize - 2 * RenderingConstants.PLANET_PADDING) / 2f
        
        val sizeLookup = mapOf(
            PlanetSize.LARGE to maxPlanetRadius * RenderingConstants.PLANET_SIZE_BIG_MODIFIER,
            PlanetSize.MEDIUM to maxPlanetRadius * RenderingConstants.PLANET_SIZE_MEDIUM_MODIFIER,
            PlanetSize.SMALL to maxPlanetRadius * RenderingConstants.PLANET_SIZE_SMALL_MODIFIER
        )
        
        return SizeCalculation(planetCount, radialSlotSize, maxPlanetRadius, sizeLookup)
    }

    private var cachedCanvasAnalysis: CanvasAnalysis? = null
    private var cachedSizeCalculation: SizeCalculation? = null
    private var lastCanvasSize: Size = Size.Zero
    private var lastPlanetCount: Int = 0

    fun drawPlanets(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        canvasSize: Size,
        iconCache: IconCache? = null,
        orbitPathCache: de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache? = null,
        speedMultiplier: Float = 1f
    ) {
        if (orbitalSystem.orbitalBodies.isEmpty()) return

        val canvasAnalysis = if (lastCanvasSize != canvasSize) {
            lastCanvasSize = canvasSize
            analyzeCanvas(canvasSize, orbitalSystem.star).also { cachedCanvasAnalysis = it }
        } else {
            cachedCanvasAnalysis ?: analyzeCanvas(canvasSize, orbitalSystem.star).also { cachedCanvasAnalysis = it }
        }

        val currentPlanetCount = orbitalSystem.orbitalBodies.size

        val sizeCalculation = if (cachedSizeCalculation == null ||
                                  lastCanvasSize != canvasSize ||
                                  lastPlanetCount != currentPlanetCount) {
            lastPlanetCount = currentPlanetCount
            calculateSizes(orbitalSystem.orbitalBodies, canvasAnalysis).also { cachedSizeCalculation = it }
        } else {
            cachedSizeCalculation!!
        }

        orbitalSystem.orbitalBodies.forEachIndexed { index, orbitalBody ->
            drawSinglePlanet(
                drawScope = drawScope,
                orbitalBody = orbitalBody,
                index = index,
                animationTime = animationTime,
                canvasAnalysis = canvasAnalysis,
                sizeCalculation = sizeCalculation,
                iconCache = iconCache,
                orbitPathCache = orbitPathCache,
                speedMultiplier = speedMultiplier
            )
        }
    }

    private fun drawSinglePlanet(
        drawScope: DrawScope,
        orbitalBody: OrbitalBody,
        index: Int,
        animationTime: Float,
        canvasAnalysis: CanvasAnalysis,
        sizeCalculation: SizeCalculation,
        iconCache: IconCache? = null,
        orbitPathCache: de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache? = null,
        speedMultiplier: Float = 1f
    ) {
        val basePlanetRadius = sizeCalculation.sizeLookup[orbitalBody.orbitalConfig.sizeCategory] ?:
        sizeCalculation.sizeLookup[PlanetSize.MEDIUM]!!

        val orbitDistance = orbitalBody.orbitalConfig.distance

        val orbitPath = orbitPathCache?.getPath(orbitalBody)
        if (orbitPath != null) {
            val translatedPath = Path()
            translatedPath.addPath(orbitPath, canvasAnalysis.center)
            drawScope.drawPath(
                path = translatedPath,
                color = Color.White.copy(alpha = RenderingConstants.ORBIT_LINE_ALPHA),
                style = Stroke(width = RenderingConstants.ORBIT_LINE_WIDTH)
            )
        } else {
            drawOrbitPath(drawScope, canvasAnalysis.center, orbitDistance, orbitalBody.orbitalConfig.ellipseRatio)
        }

        val config = orbitalBody.orbitalConfig
        val baseOrbitDuration = orbitalBody.appInfo.customOrbitSpeed ?: config.orbitDuration
        val planetKey = orbitalBody.appInfo.packageName

        val baseAngularVelocity = 2 * PI / baseOrbitDuration
        val deltaTime = 0.016f * speedMultiplier

        val currentAngle = planetAngles[planetKey] ?: (config.startAngle * PI / 180f)
        val depthSpeed = calculateDepthSpeed(currentAngle)
        val newAngle = currentAngle + baseAngularVelocity * depthSpeed * deltaTime
        planetAngles[planetKey] = newAngle

        val angle = newAngle
        val depthScale = calculateDepthScale(angle)
        val planetRadius = basePlanetRadius * depthScale

        val position = calculatePlanetPositionWithAngle(
            angle = angle,
            orbitDistance = orbitDistance,
            center = canvasAnalysis.center,
            ellipseRatio = orbitalBody.orbitalConfig.ellipseRatio
        )

        drawPlanet(drawScope, orbitalBody, position, planetRadius, iconCache)
    }

    private fun drawOrbitPath(
        drawScope: DrawScope,
        center: Offset,
        orbitDistance: Float,
        ellipseRatio: Float
    ) {
        val path = Path()
        val radiusX = orbitDistance * ellipseRatio
        val radiusY = orbitDistance
        
        val tiltAngle = RenderingConstants.ORBIT_TILT_ANGLE * PI / 180.0
        val cosTilt = cos(tiltAngle).toFloat()
        val sinTilt = sin(tiltAngle).toFloat()
        
        val numPoints = 100
        for (i in 0 until numPoints) {
            val angle = (i.toFloat() / numPoints) * 2 * PI
            val offsetX = radiusX * cos(angle).toFloat()
            val offsetY = radiusY * sin(angle).toFloat()
            
            val rotatedX = offsetX * cosTilt - offsetY * sinTilt
            val rotatedY = offsetX * sinTilt + offsetY * cosTilt
            
            val x = center.x + rotatedX
            val y = center.y + rotatedY
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        drawScope.drawPath(
            path = path,
            color = Color.White.copy(alpha = RenderingConstants.ORBIT_LINE_ALPHA),
            style = Stroke(width = RenderingConstants.ORBIT_LINE_WIDTH)
        )
    }

    private fun calculatePlanetPosition(
        orbitalBody: OrbitalBody,
        animationTime: Float,
        orbitDistance: Float,
        center: Offset,
        ellipseRatio: Float
    ): Offset {
        val config = orbitalBody.orbitalConfig
        
        val baseOrbitDuration = orbitalBody.appInfo.customOrbitSpeed ?: config.orbitDuration
        
        val normalizedTime = (animationTime / baseOrbitDuration) % 1f
        val angle = (normalizedTime * 360f + config.startAngle) * PI / 180f
        
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        
        val x = center.x + orbitDistance * cosAngle * ellipseRatio
        val y = center.y + orbitDistance * sinAngle
        
        return Offset(x, y)
    }

    private fun calculatePlanetPositionWithAngle(
        angle: Double,
        orbitDistance: Float,
        center: Offset,
        ellipseRatio: Float
    ): Offset {
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        
        val radiusX = orbitDistance * ellipseRatio
        val radiusY = orbitDistance
        
        val offsetX = radiusX * cosAngle
        val offsetY = radiusY * sinAngle
        
        val tiltAngle = RenderingConstants.ORBIT_TILT_ANGLE * PI / 180.0
        val cosTilt = cos(tiltAngle).toFloat()
        val sinTilt = sin(tiltAngle).toFloat()
        
        val rotatedX = offsetX * cosTilt - offsetY * sinTilt
        val rotatedY = offsetX * sinTilt + offsetY * cosTilt
        
        val x = center.x + rotatedX
        val y = center.y + rotatedY
        
        return Offset(x, y)
    }


    private fun drawPlanet(
        drawScope: DrawScope,
        orbitalBody: OrbitalBody,
        position: Offset,
        planetRadius: Float,
        iconCache: IconCache? = null
    ) {
        val cachedBitmap = iconCache?.getIconBitmapSync(orbitalBody)
        
        if (cachedBitmap != null) {
            val iconSize = (planetRadius * 2).toInt()
            drawScope.drawImage(
                image = cachedBitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    (position.x - planetRadius).toInt(),
                    (position.y - planetRadius).toInt()
                ),
                dstSize = androidx.compose.ui.unit.IntSize(iconSize, iconSize)
            )
        } else {
            drawScope.drawCircle(
                color = orbitalBody.orbitalConfig.color.copy(alpha = 0.2f),
                radius = planetRadius * 1.3f,
                center = position
            )
            
            drawScope.drawCircle(
                color = orbitalBody.orbitalConfig.color,
                radius = planetRadius,
                center = position
            )
        }
    }
}
