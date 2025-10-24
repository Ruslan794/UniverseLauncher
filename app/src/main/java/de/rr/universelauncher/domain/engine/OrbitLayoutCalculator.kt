package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Size
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.Star
import kotlin.math.min

data class OrbitLayout(
    val orbitDistance: Float,
    val planetSize: Float
)

object OrbitLayoutCalculator {

    private const val EDGE_PADDING = 60f
    private const val MIN_ORBIT_SPACING = 40f

    fun calculateOrbitLayouts(
        star: Star,
        orbitalBodies: List<OrbitalBody>,
        canvasSize: Size
    ): List<OrbitLayout> {
        if (orbitalBodies.isEmpty()) return emptyList()

        val canvasRadius = min(canvasSize.width, canvasSize.height) / 2f
        val maxUsableRadius = canvasRadius - EDGE_PADDING

        // Calculate minimum first orbit considering the first planet's radius
        val firstPlanetRadius = orbitalBodies.firstOrNull()?.orbitalConfig?.size ?: 0f
        val minFirstOrbit = star.radius + star.deadZone + firstPlanetRadius + MIN_ORBIT_SPACING

        if (maxUsableRadius <= minFirstOrbit) {
            return orbitalBodies.map { OrbitLayout(minFirstOrbit, it.orbitalConfig.size) }
        }

        val ellipseRatio = orbitalBodies.firstOrNull()?.orbitalConfig?.ellipseRatio ?: 1.3f

        val totalPlanetDiameters = orbitalBodies.sumOf { (it.orbitalConfig.size * 2).toDouble() }.toFloat()
        val minRequiredSpacing = (orbitalBodies.size - 1) * MIN_ORBIT_SPACING
        val totalRequiredSpace = totalPlanetDiameters + minRequiredSpacing

        val availableSpace = (maxUsableRadius / ellipseRatio) - minFirstOrbit

        return if (totalRequiredSpace > availableSpace) {
            compressOrbits(orbitalBodies, minFirstOrbit, maxUsableRadius, ellipseRatio)
        } else {
            distributeOrbitsEvenly(orbitalBodies, minFirstOrbit, availableSpace, ellipseRatio)
        }
    }

    private fun distributeOrbitsEvenly(
        orbitalBodies: List<OrbitalBody>,
        startDistance: Float,
        availableSpace: Float,
        ellipseRatio: Float
    ): List<OrbitLayout> {
        val layouts = mutableListOf<OrbitLayout>()
        
        if (orbitalBodies.isEmpty()) return layouts
        
        // Simple even distribution
        val spacing = if (orbitalBodies.size > 1) {
            availableSpace / orbitalBodies.size
        } else {
            availableSpace / 2
        }
        
        var currentDistance = startDistance

        orbitalBodies.forEach { body ->
            val planetRadius = body.orbitalConfig.size
            
            layouts.add(OrbitLayout(
                orbitDistance = currentDistance,
                planetSize = planetRadius
            ))

            currentDistance += spacing
        }

        return layouts
    }

    private fun compressOrbits(
        orbitalBodies: List<OrbitalBody>,
        startDistance: Float,
        maxUsableRadius: Float,
        ellipseRatio: Float
    ): List<OrbitLayout> {
        val layouts = mutableListOf<OrbitLayout>()
        var currentDistance = startDistance
        val reducedSpacing = MIN_ORBIT_SPACING * 0.5f

        orbitalBodies.forEach { body ->
            val planetRadius = body.orbitalConfig.size

            val maxPossibleDistance = (maxUsableRadius - planetRadius) / ellipseRatio

            if (currentDistance > maxPossibleDistance) {
                currentDistance = maxPossibleDistance.coerceAtLeast(startDistance)
            }

            layouts.add(OrbitLayout(
                orbitDistance = currentDistance,
                planetSize = planetRadius
            ))

            currentDistance += reducedSpacing
        }

        return layouts
    }
}