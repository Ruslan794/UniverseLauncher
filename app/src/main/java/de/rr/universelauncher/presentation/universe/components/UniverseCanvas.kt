package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.Star
import kotlin.math.*
import androidx.core.graphics.createBitmap

@Composable
fun UniverseCanvas(
    orbitalSystem: OrbitalSystem,
    animationTime: Float,
    onPlanetTapped: (OrbitalBody) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                            val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                                orbitalBody = orbitalBody,
                                timeSeconds = animationTime
                            )

                            val screenX = centerX + position.first
                            val screenY = centerY + position.second
                            val planetCenter = Offset(screenX, screenY)

                            val distance = (offset - planetCenter).getDistance()
                            if (distance <= orbitalBody.orbitalConfig.size * 2) {
                                onPlanetTapped(orbitalBody)
                            }
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)

        drawOrbitalPaths(orbitalSystem, center)
        drawStar(orbitalSystem.star, center)

        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
            val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                orbitalBody = orbitalBody,
                timeSeconds = animationTime
            )

            val screenX = centerX + position.first
            val screenY = centerY + position.second

            drawOrbitalBody(
                orbitalBody = orbitalBody,
                position = Offset(screenX, screenY)
            )
        }
    }
}

private fun DrawScope.drawOrbitalPaths(
    system: OrbitalSystem,
    center: Offset
) {
    system.orbitalBodies.forEach { orbitalBody ->
        val pathPoints = OrbitalPhysics.calculateOrbitPathPoints(orbitalBody)

        if (pathPoints.isNotEmpty()) {
            val path = Path()
            val firstPoint = pathPoints[0]
            path.moveTo(center.x + firstPoint.first, center.y + firstPoint.second)

            for (i in 1 until pathPoints.size) {
                val point = pathPoints[i]
                path.lineTo(center.x + point.first, center.y + point.second)
            }

            path.close()

            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawStar(
    star: Star,
    center: Offset
) {
    drawCircle(
        color = star.color.copy(alpha = 0.3f),
        radius = star.radius * 2,
        center = center
    )

    drawCircle(
        color = star.color,
        radius = star.radius,
        center = center
    )
}

private fun DrawScope.drawOrbitalBody(
    orbitalBody: OrbitalBody,
    position: Offset
) {
    val config = orbitalBody.orbitalConfig
    val appIcon = orbitalBody.appInfo.icon
    
    val iconSize = (config.size * 2).toInt()
    
    try {
        val bitmap = createBitmap(iconSize, iconSize)
        val canvas = android.graphics.Canvas(bitmap)
        appIcon.setBounds(0, 0, iconSize, iconSize)
        appIcon.draw(canvas)
        
        val imageBitmap = bitmap.asImageBitmap()
        
        drawImage(
            image = imageBitmap,
            topLeft = Offset(
                position.x - config.size,
                position.y - config.size
            )
        )
    } catch (e: Exception) {
        drawCircle(
            color = config.color.copy(alpha = 0.2f),
            radius = config.size * 1.5f,
            center = position
        )

        drawCircle(
            color = config.color,
            radius = config.size,
            center = position
        )
    }
}