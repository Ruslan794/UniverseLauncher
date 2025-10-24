package de.rr.universelauncher.domain.engine

import androidx.compose.ui.graphics.Color
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalConfig
import de.rr.universelauncher.domain.model.Star
import de.rr.universelauncher.domain.model.AppInfo
import kotlin.math.*

object OrbitalPhysics {

    private val colors = listOf(
        Color(0xFF8C7853),
        Color(0xFFFFC649),
        Color(0xFF6B93D6),
        Color(0xFFCD5C5C),
        Color(0xFFD8CA9D),
        Color(0xFFFAD5A5),
        Color(0xFF4FD0E7),
        Color(0xFF4B70DD),
        Color(0xFF9C27B0),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFE91E63)
    )

    fun calculateOrbitalBodyPosition(
        orbitalBody: OrbitalBody,
        timeSeconds: Float
    ): Pair<Float, Float> {
        val config = orbitalBody.orbitalConfig

        val angle = (timeSeconds / config.orbitDuration) * 2 * PI + Math.toRadians(config.startAngle.toDouble())

        val radiusX = config.distance * config.ellipseRatio
        val radiusY = config.distance

        val x = radiusX * cos(angle).toFloat()
        val y = radiusY * sin(angle).toFloat()

        return Pair(x, y)
    }

    fun calculateOrbitPathPoints(
        orbitalBody: OrbitalBody,
        numPoints: Int = 100
    ): List<Pair<Float, Float>> {
        val config = orbitalBody.orbitalConfig
        val points = mutableListOf<Pair<Float, Float>>()

        val radiusX = config.distance * config.ellipseRatio
        val radiusY = config.distance

        for (i in 0 until numPoints) {
            val timeRatio = i.toFloat() / numPoints
            val angle = timeRatio * 2 * PI + Math.toRadians(config.startAngle.toDouble())

            val x = radiusX * cos(angle).toFloat()
            val y = radiusY * sin(angle).toFloat()

            points.add(Pair(x, y))
        }

        return points
    }

    fun createOrbitalBodiesFromApps(apps: List<AppInfo>): List<OrbitalBody> {
        return apps.take(12).mapIndexed { index, app ->
            val distance = 80f + (index * 30f) // Initial distances (will be recalculated)
            val orbitDuration = 8f + (index * 2f) // Different speeds
            val size = 20f + (index % 3) * 10f
            val startAngle = (index * 30f) % 360f // Staggered starting positions
            val color = colors[index % colors.size]

            val orbitalConfig = OrbitalConfig(
                distance = distance,
                orbitDuration = orbitDuration,
                size = size,
                startAngle = startAngle,
                color = color,
                ellipseRatio = 1.5f
            )

            OrbitalBody(app, orbitalConfig)
        }
    }

    
    fun createOrbitalSystemFromApps(apps: List<AppInfo>): OrbitalSystem {
        val sun = Star(
            radius = 20f,
            color = Color(0xFFFFD700)
        )
        
        val colors = listOf(
            Color(0xFF8C7853), // Mercury brown
            Color(0xFFFFC649), // Venus yellow
            Color(0xFF6B93D6), // Earth blue
            Color(0xFFCD5C5C), // Mars red
            Color(0xFFD8CA9D), // Jupiter tan
            Color(0xFFFAD5A5), // Saturn gold
            Color(0xFF4FD0E7), // Uranus cyan
            Color(0xFF4B70DD), // Neptune blue
            Color(0xFF9C27B0), // Purple
            Color(0xFF4CAF50), // Green
            Color(0xFFFF9800), // Orange
            Color(0xFFE91E63)  // Pink
        )
        
        val orbitalBodies = apps.take(12).mapIndexed { index, app ->
            val distance = 80f + (index * 30f) // Initial distances (will be recalculated)
            val orbitDuration = 8f + (index * 2f) // Different speeds
            val size = 20f + (index % 3) * 10f
            val startAngle = (index * 30f) % 360f // Staggered starting positions
            val color = colors[index % colors.size]

            val orbitalConfig = OrbitalConfig(
                distance = distance,
                orbitDuration = orbitDuration,
                size = size,
                startAngle = startAngle,
                color = color,
                ellipseRatio = 1.5f
            )
            
            OrbitalBody(app, orbitalConfig)
        }
        
        val initialSystem = OrbitalSystem(sun, orbitalBodies)
        // Recalculate distances to prevent overlaps
        return OrbitalDistanceCalculator.recalculateOrbitalDistances(initialSystem)
    }
}
