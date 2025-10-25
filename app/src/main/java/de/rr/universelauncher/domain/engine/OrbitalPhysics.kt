package de.rr.universelauncher.domain.engine

import androidx.compose.ui.graphics.Color
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalConfig
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.model.defaultStar
import de.rr.universelauncher.domain.model.PlanetSize
import androidx.compose.ui.geometry.Size
import kotlin.math.*

object OrbitalPhysics {

    private val planetColors = listOf(
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

        val tiltAngle = RenderingConstants.ORBIT_TILT_ANGLE * PI / 180.0
        val cosTilt = cos(tiltAngle).toFloat()
        val sinTilt = sin(tiltAngle).toFloat()

        for (i in 0 until numPoints) {
            val timeRatio = i.toFloat() / numPoints
            val angle = timeRatio * 2 * PI + Math.toRadians(config.startAngle.toDouble())

            val offsetX = radiusX * cos(angle).toFloat()
            val offsetY = radiusY * sin(angle).toFloat()

            val rotatedX = offsetX * cosTilt - offsetY * sinTilt
            val rotatedY = offsetX * sinTilt + offsetY * cosTilt

            points.add(Pair(rotatedX, rotatedY))
        }

        return points
    }

    fun createOrbitalSystemFromApps(apps: List<AppInfo>, appOrder: Map<String, Int> = emptyMap()): OrbitalSystem {
        val sun = defaultStar

        val selectedApps = apps

        val sortedApps = if (appOrder.isNotEmpty()) {
            selectedApps.sortedBy { app ->
                appOrder[app.packageName] ?: Int.MAX_VALUE
            }
        } else {
            selectedApps.sortedByDescending { it.launchCount }
        }

        val orbitalBodies = sortedApps.mapIndexed { index, app ->
            val orbitDuration = app.customOrbitSpeed ?: (15f + (index * 3f))
            val sizeCategory = app.customPlanetSize ?: PlanetSize.MEDIUM
            val randomAngle = (0..359).random().toFloat()
            val color = planetColors[index % planetColors.size]

            val orbitalConfig = OrbitalConfig(
                distance = 0f,
                orbitDuration = orbitDuration,
                sizeCategory = sizeCategory,
                startAngle = randomAngle,
                color = color,
                ellipseRatio = 1.3f
            )

            OrbitalBody(app, orbitalConfig)
        }

        val initialSystem = OrbitalSystem(sun, orbitalBodies)
        return OrbitalDistanceCalculator.recalculateOrbitalDistances(initialSystem)
    }

}