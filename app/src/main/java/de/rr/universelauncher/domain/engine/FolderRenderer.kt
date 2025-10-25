package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.rr.universelauncher.domain.model.Folder
import kotlin.math.*

object FolderRenderer {

    private const val FOLDER_STAR_RADIUS = 60f
    private const val FOLDER_STAR_DEAD_ZONE = 40f
    private const val FOLDER_PLANET_RADIUS = 12f
    private const val MIN_ORBIT_RADIUS = 100f
    private const val ORBIT_SPACING = 50f

    private val PLANET_COLORS = listOf(
        Color(0xFF4A90E2),
        Color(0xFFE24A4A),
        Color(0xFF4AE24A),
        Color(0xFFE2E24A),
        Color(0xFFE24AE2),
        Color(0xFF4AE2E2),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFE66D),
        Color(0xFFA8E6CF)
    )

    fun drawFolder(
        drawScope: DrawScope,
        folder: Folder,
        animationTime: Float,
        planetCount: Int
    ) {
        val canvasSize = drawScope.size
        val margin = 32f * 3.5f
        val availableWidth = canvasSize.width - (margin * 2)
        val availableHeight = canvasSize.height - (margin * 2)
        val maxRadius = minOf(availableWidth, availableHeight) / 2f - 50f
        
        val clampedPosition = androidx.compose.ui.geometry.Offset(
            folder.position.x.coerceIn(margin, canvasSize.width - margin),
            folder.position.y.coerceIn(margin, canvasSize.height - margin)
        )
        
        drawFolderStar(drawScope, clampedPosition, animationTime)
        drawFolderPlanets(drawScope, clampedPosition, animationTime, planetCount, maxRadius)
    }

    private fun drawFolderStar(
        drawScope: DrawScope,
        position: Offset,
        animationTime: Float
    ) {
        val starColor = Color(0xFFFFD700)
        val pulse = (sin(animationTime * 1.2f) * 0.15f + 0.9f)

        val rings = 3
        for (i in rings downTo 1) {
            val alpha = 0.25f * (i.toFloat() / rings)
            val radius = FOLDER_STAR_RADIUS * (1f + (rings - i) * 0.2f) * pulse

            drawScope.drawCircle(
                color = starColor.copy(alpha = alpha),
                radius = radius,
                center = position
            )
        }

        drawScope.drawCircle(
            color = starColor,
            radius = FOLDER_STAR_RADIUS * 0.85f,
            center = position
        )

        drawScope.drawCircle(
            color = Color(0xFFFFF8DC).copy(alpha = 0.7f),
            radius = FOLDER_STAR_RADIUS * 0.55f,
            center = position
        )
    }

    private fun drawFolderPlanets(
        drawScope: DrawScope,
        center: Offset,
        animationTime: Float,
        planetCount: Int,
        maxRadius: Float
    ) {
        if (planetCount <= 0) return

        for (planetIndex in 0 until planetCount) {
            val orbitRadius = (MIN_ORBIT_RADIUS + (planetIndex * ORBIT_SPACING)).coerceAtMost(maxRadius)

            drawOrbitPath(drawScope, center, orbitRadius)

            val speed = 1.5f + (planetIndex * 0.4f)
            val angle = animationTime * speed

            val x = center.x + cos(angle).toFloat() * orbitRadius
            val y = center.y + sin(angle).toFloat() * orbitRadius

            val planetPosition = Offset(x, y)
            val color = PLANET_COLORS[planetIndex % PLANET_COLORS.size]

            drawFolderPlanet(drawScope, planetPosition, FOLDER_PLANET_RADIUS, color)
        }
    }

    private fun drawOrbitPath(
        drawScope: DrawScope,
        center: Offset,
        radius: Float
    ) {
        val path = androidx.compose.ui.graphics.Path()
        val steps = 64
        for (i in 0..steps) {
            val angle = (i * 2 * PI / steps).toFloat()
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            val point = Offset(x, y)

            if (i == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        drawScope.drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.12f),
            style = Stroke(width = 1.5f)
        )
    }

    private fun drawFolderPlanet(
        drawScope: DrawScope,
        position: Offset,
        radius: Float,
        color: Color
    ) {
        val shadowColor = color.copy(alpha = 0.6f)

        drawScope.drawCircle(
            color = shadowColor,
            radius = radius,
            center = position + Offset(radius * 0.1f, radius * 0.1f)
        )

        drawScope.drawCircle(
            color = color,
            radius = radius,
            center = position
        )

        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = radius * 0.35f,
            center = position - Offset(radius * 0.3f, radius * 0.3f)
        )
    }
}