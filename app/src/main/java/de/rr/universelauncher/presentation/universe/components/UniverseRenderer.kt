package de.rr.universelauncher.presentation.universe.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.IconCache
import de.rr.universelauncher.presentation.universe.components.cache.OrbitPathCache

object UniverseRenderer {

    fun drawUniverse(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache,
        orbitPathCache: OrbitPathCache
    ) {
        drawOrbitalPaths(drawScope, orbitPathCache)
        drawStar(drawScope, orbitalSystem.star, center)
        drawPlanets(drawScope, orbitalSystem, animationTime, center, iconCache)
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
        center: Offset
    ) {
        drawScope.drawCircle(
            color = star.color.copy(alpha = 0.3f),
            radius = star.radius * 1.5f,
            center = center
        )

        drawScope.drawCircle(
            color = star.color,
            radius = star.radius,
            center = center
        )
    }

    private fun drawPlanets(
        drawScope: DrawScope,
        orbitalSystem: OrbitalSystem,
        animationTime: Float,
        center: Offset,
        iconCache: IconCache
    ) {
        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
            val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                orbitalBody = orbitalBody,
                timeSeconds = animationTime
            )

            val screenX = center.x + position.first
            val screenY = center.y + position.second

            drawOrbitalBody(
                drawScope = drawScope,
                orbitalBody = orbitalBody,
                position = Offset(screenX, screenY),
                iconCache = iconCache
            )
        }
    }

    private fun drawOrbitalBody(
        drawScope: DrawScope,
        orbitalBody: de.rr.universelauncher.domain.model.OrbitalBody,
        position: Offset,
        iconCache: IconCache
    ) {
        val planetRadius = orbitalBody.orbitalConfig.size

        val cachedBitmap = iconCache.getIconBitmapSync(orbitalBody)

        if (cachedBitmap != null) {
            drawScope.drawImage(
                image = cachedBitmap,
                topLeft = Offset(
                    position.x - planetRadius,
                    position.y - planetRadius
                )
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