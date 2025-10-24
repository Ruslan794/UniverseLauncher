package de.rr.universelauncher.presentation.universe.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.engine.PlanetRenderingEngine
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.IconCache
import de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap

object UniverseRenderer {

    var sunBitmap: ImageBitmap? = null

    fun drawUniverse(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache,
        orbitPathCache: OrbitPathCache,
        canvasSize: Size
    ) {
        // Orbit paths are now drawn in PlanetRenderingEngine for better performance
        drawStar(drawScope, orbitalSystem.star, center, sunBitmap)
        drawPlanets(drawScope, orbitalSystem, animationTime, center, iconCache, canvasSize, orbitPathCache)
    }

    private fun drawOrbitalPaths(
        drawScope: DrawScope,
        orbitPathCache: OrbitPathCache
    ) {
        orbitPathCache.getAllPaths().forEach { path ->
            drawScope.drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.15f),
                style = Stroke(width = 2f)
            )
        }
    }

    private fun drawStar(
        drawScope: DrawScope,
        star: de.rr.universelauncher.domain.model.Star,
        center: Offset,
        sunImage: ImageBitmap?
    ) {
        // Always draw the enhanced sun design
        drawEnhancedSun(drawScope, star, center)
    }
    
    private fun drawEnhancedSun(
        drawScope: DrawScope,
        star: de.rr.universelauncher.domain.model.Star,
        center: Offset
    ) {
        val baseRadius = star.radius
        val sunColor = Color(0xFFFFD700) // Golden yellow
        val brightColor = Color(0xFFFFF8DC) // Cream white
        val shadowColor = Color(0xFFB8860B) // Dark goldenrod
        
        // Soft outer glow
        drawScope.drawCircle(
            color = sunColor.copy(alpha = 0.15f),
            radius = baseRadius * 1.6f,
            center = center
        )
        
        // Main sun body
        drawScope.drawCircle(
            color = sunColor,
            radius = baseRadius,
            center = center
        )
        
        // Subtle inner highlight (reflection)
        drawScope.drawCircle(
            color = brightColor.copy(alpha = 0.4f),
            radius = baseRadius * 0.6f,
            center = center + Offset(-baseRadius * 0.2f, -baseRadius * 0.2f)
        )
        
        // Soft shadow on the bottom
        drawScope.drawCircle(
            color = shadowColor.copy(alpha = 0.3f),
            radius = baseRadius * 0.8f,
            center = center + Offset(baseRadius * 0.1f, baseRadius * 0.1f)
        )
    }

    private fun drawPlanets(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache,
        canvasSize: Size,
        orbitPathCache: OrbitPathCache
    ) {
        // Use the new PlanetRenderingEngine for consistent rendering
        PlanetRenderingEngine.drawPlanets(
            drawScope = drawScope,
            orbitalSystem = orbitalSystem,
            animationTime = animationTime,
            canvasSize = canvasSize,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache
        )
    }

    private fun drawOrbitalBody(
        drawScope: DrawScope,
        orbitalBody: de.rr.universelauncher.domain.model.OrbitalBody,
        position: Offset,
        iconCache: IconCache
    ) {
        // This method is now handled by PlanetRenderingEngine
        // Keeping for potential icon rendering in the future
        val cachedBitmap = iconCache.getIconBitmapSync(orbitalBody)

        if (cachedBitmap != null) {
            // For now, use a default radius for icon sizing
            // In the future, this could be calculated dynamically
            val planetRadius = 20f
            
            drawScope.drawImage(
                image = cachedBitmap,
                topLeft = Offset(
                    position.x - planetRadius,
                    position.y - planetRadius
                )
            )
        }
    }
}