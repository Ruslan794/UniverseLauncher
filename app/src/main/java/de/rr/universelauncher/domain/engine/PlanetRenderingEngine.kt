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

object PlanetRenderingEngine {

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
            PlanetSize.LARGE to maxPlanetRadius * 1.5f,
            PlanetSize.MEDIUM to maxPlanetRadius * 1.0f,
            PlanetSize.SMALL to maxPlanetRadius * 0.75f
        )
        
        return SizeCalculation(planetCount, radialSlotSize, maxPlanetRadius, sizeLookup)
    }

    fun drawPlanets(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        canvasSize: Size,
        iconCache: IconCache? = null,
        orbitPathCache: de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache? = null
    ) {
        val canvasAnalysis = analyzeCanvas(canvasSize, orbitalSystem.star)
        val sizeCalculation = calculateSizes(orbitalSystem.orbitalBodies, canvasAnalysis)
        
        orbitalSystem.orbitalBodies.forEachIndexed { index, orbitalBody ->
            drawSinglePlanet(
                drawScope = drawScope,
                orbitalBody = orbitalBody,
                index = index,
                animationTime = animationTime,
                canvasAnalysis = canvasAnalysis,
                sizeCalculation = sizeCalculation,
                iconCache = iconCache,
                orbitPathCache = orbitPathCache
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
        orbitPathCache: de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache? = null
    ) {
        // 10. Tatsächliche Größe auflösen
        val planetRadius = sizeCalculation.sizeLookup[orbitalBody.orbitalConfig.sizeCategory] ?: 
            sizeCalculation.sizeLookup[PlanetSize.MEDIUM]!!
        
        // 11. Orbit-Distance berechnen
        val orbitDistance = canvasAnalysis.minOffset + 
            (index * sizeCalculation.radialSlotSize) + 
            sizeCalculation.radialSlotSize / 2f
        
        // 12. Orbit-Pfad zeichnen (aus Cache oder neu erstellen)
        val orbitPath = orbitPathCache?.getPath(orbitalBody)
        if (orbitPath != null) {
            drawScope.drawPath(
                path = orbitPath,
                color = Color.White.copy(alpha = RenderingConstants.ORBIT_LINE_ALPHA),
                style = Stroke(width = RenderingConstants.ORBIT_LINE_WIDTH)
            )
        } else {
            // Fallback: Orbit-Pfad neu zeichnen
            drawOrbitPath(drawScope, canvasAnalysis.center, orbitDistance, orbitalBody.orbitalConfig.ellipseRatio)
        }
        
        // 14. Planet-Position berechnen
        val position = calculatePlanetPosition(
            orbitalBody = orbitalBody,
            animationTime = animationTime,
            orbitDistance = orbitDistance,
            center = canvasAnalysis.center,
            ellipseRatio = orbitalBody.orbitalConfig.ellipseRatio
        )
        
        // 15. Planet zeichnen
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
        
        // Ellipse-Pfad erstellen
        val rect = androidx.compose.ui.geometry.Rect(
            center.x - radiusX,
            center.y - radiusY,
            center.x + radiusX,
            center.y + radiusY
        )
        path.addOval(rect)
        
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
        
        // 14. Planet-Position berechnen
        val effectiveOrbitDuration = orbitalBody.appInfo.customOrbitSpeed ?: config.orbitDuration
        val angle = (animationTime / effectiveOrbitDuration * 360f + config.startAngle) * PI / 180f
        
        val x = center.x + orbitDistance * cos(angle).toFloat() * ellipseRatio
        val y = center.y + orbitDistance * sin(angle).toFloat()
        
        return Offset(x, y)
    }

    private fun drawPlanet(
        drawScope: DrawScope,
        orbitalBody: OrbitalBody,
        position: Offset,
        planetRadius: Float,
        iconCache: IconCache? = null
    ) {
        // Versuche Icon zu zeichnen, falls verfügbar
        val cachedBitmap = iconCache?.getIconBitmapSync(orbitalBody)
        
        if (cachedBitmap != null) {
            // Icon mit korrekter Größe zeichnen
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
            // Fallback: Farbige Kreise zeichnen
            // Glow-Effekt
            drawScope.drawCircle(
                color = orbitalBody.orbitalConfig.color.copy(alpha = 0.2f),
                radius = planetRadius * 1.3f,
                center = position
            )
            
            // Haupt-Planet
            drawScope.drawCircle(
                color = orbitalBody.orbitalConfig.color,
                radius = planetRadius,
                center = position
            )
        }
    }
}
