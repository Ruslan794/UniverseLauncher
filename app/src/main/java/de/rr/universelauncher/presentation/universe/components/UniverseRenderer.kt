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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*

object UniverseRenderer {

    var sunBitmap: ImageBitmap? = null

    fun drawUniverse(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache,
        orbitPathCache: OrbitPathCache,
        canvasSize: Size,
        speedMultiplier: Float = 1f
    ) {
        drawStar(drawScope, orbitalSystem.star, center, sunBitmap, animationTime)
        drawPlanets(drawScope, orbitalSystem, animationTime, center, iconCache, canvasSize, orbitPathCache, speedMultiplier)
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
        sunImage: ImageBitmap?,
        animationTime: Float
    ) {
        drawGradientBloomSun(drawScope, star, center, animationTime)
    }

    private fun drawGradientBloomSun(
        drawScope: DrawScope,
        star: de.rr.universelauncher.domain.model.Star,
        center: Offset,
        animationTime: Float
    ) {
        val baseRadius = star.radius
        val sunColor = Color(0xFFFFD700)
        val pulse = (sin(animationTime * 0.5f) * 0.1f + 0.9f)

        val rings = 5
        for (i in rings downTo 1) {
            val alpha = 0.15f * (i.toFloat() / rings)
            val radius = baseRadius * (1f + (rings - i) * 0.25f) * pulse

            drawScope.drawCircle(
                color = sunColor.copy(alpha = alpha),
                radius = radius,
                center = center
            )
        }

        drawScope.drawCircle(
            color = sunColor,
            radius = baseRadius * 0.9f,
            center = center
        )

        drawScope.drawCircle(
            color = Color(0xFFFFF8DC).copy(alpha = 0.5f),
            radius = baseRadius * 0.6f,
            center = center
        )
    }

    private fun drawMinimalistFlatSun(
        drawScope: DrawScope,
        star: de.rr.universelauncher.domain.model.Star,
        center: Offset
    ) {
        val baseRadius = star.radius
        val sunColor = Color(0xFFFFD700)
        val shadowColor = Color(0xFFB8860B)

        drawScope.drawCircle(
            color = sunColor.copy(alpha = 0.2f),
            radius = baseRadius * 1.3f,
            center = center
        )

        drawScope.drawCircle(
            color = shadowColor,
            radius = baseRadius,
            center = center + Offset(baseRadius * 0.05f, baseRadius * 0.05f)
        )

        drawScope.drawCircle(
            color = sunColor,
            radius = baseRadius,
            center = center
        )

        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = baseRadius * 0.5f,
            center = center - Offset(baseRadius * 0.2f, baseRadius * 0.2f)
        )
    }

    private fun drawPlanets(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache,
        canvasSize: Size,
        orbitPathCache: OrbitPathCache,
        speedMultiplier: Float = 1f
    ) {
        PlanetRenderingEngine.drawPlanets(
            drawScope = drawScope,
            orbitalSystem = orbitalSystem,
            animationTime = animationTime,
            canvasSize = canvasSize,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache,
            speedMultiplier = speedMultiplier
        )
    }

    private fun drawOrbitalBody(
        drawScope: DrawScope,
        orbitalBody: de.rr.universelauncher.domain.model.OrbitalBody,
        position: Offset,
        iconCache: IconCache
    ) {
        val cachedBitmap = iconCache.getIconBitmapSync(orbitalBody)

        if (cachedBitmap != null) {
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