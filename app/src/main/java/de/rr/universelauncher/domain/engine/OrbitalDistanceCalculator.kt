package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalConfig
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.Star
import androidx.compose.ui.geometry.Size

object OrbitalDistanceCalculator {

    private const val ORBIT_PADDING = 20f
    private const val PLANET_PADDING = 15f
    private const val EDGE_PADDING = 30f
    private const val PLANET_SPACING_MULTIPLIER = 2.5f

    fun distributeOrbitsInCanvas(orbitalSystem: OrbitalSystem, canvasSize: Size): OrbitalSystem {
        val star = orbitalSystem.star
        val orbitalBodies = orbitalSystem.orbitalBodies

        if (orbitalBodies.isEmpty()) {
            return orbitalSystem
        }

        val maxCanvasRadius = minOf(canvasSize.width, canvasSize.height) / 2f - EDGE_PADDING
        val minOrbitRadius = star.radius + star.deadZone + ORBIT_PADDING
        
        if (maxCanvasRadius <= minOrbitRadius) {
            return orbitalSystem
        }

        val availableSpace = maxCanvasRadius - minOrbitRadius
        val step = (availableSpace / orbitalBodies.size) * PLANET_SPACING_MULTIPLIER
        
        val updatedBodies = orbitalBodies.mapIndexed { index, orbitalBody ->
            val planetSize = orbitalBody.orbitalConfig.size
            val planetPadding = planetSize * 0.5f
            val totalPlanetSpace = planetSize + planetPadding
            
            val newDistance = minOrbitRadius + (index * step) + totalPlanetSpace
            
            val updatedConfig = orbitalBody.orbitalConfig.copy(distance = newDistance)
            orbitalBody.copy(orbitalConfig = updatedConfig)
        }

        return OrbitalSystem(star, updatedBodies)
    }

    fun recalculateOrbitalDistances(orbitalSystem: OrbitalSystem): OrbitalSystem {
        val defaultCanvasSize = Size(800f, 600f)
        return distributeOrbitsInCanvas(orbitalSystem, defaultCanvasSize)
    }

    fun updatePlanetSizeAndRecalculate(
        orbitalSystem: OrbitalSystem,
        planetIndex: Int,
        newSize: Float
    ): OrbitalSystem {
        val orbitalBodies = orbitalSystem.orbitalBodies.toMutableList()

        if (planetIndex < 0 || planetIndex >= orbitalBodies.size) {
            return orbitalSystem
        }

        val planet = orbitalBodies[planetIndex]
        val updatedConfig = planet.orbitalConfig.copy(size = newSize)
        val updatedPlanet = planet.copy(orbitalConfig = updatedConfig)
        orbitalBodies[planetIndex] = updatedPlanet

        val updatedSystem = OrbitalSystem(orbitalSystem.star, orbitalBodies)
        return recalculateOrbitalDistances(updatedSystem)
    }
}