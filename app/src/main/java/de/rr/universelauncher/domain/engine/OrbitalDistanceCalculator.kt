package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalConfig
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.domain.model.Star

object OrbitalDistanceCalculator {

    private const val ORBIT_PADDING = 8f
    private const val PLANET_PADDING = 4f

    fun recalculateOrbitalDistances(orbitalSystem: OrbitalSystem): OrbitalSystem {
        val sun = orbitalSystem.star
        val orbitalBodies = orbitalSystem.orbitalBodies

        if (orbitalBodies.isEmpty()) {
            return orbitalSystem
        }

        val sortedBodies = orbitalBodies.sortedBy { it.orbitalConfig.distance }

        val updatedBodies = mutableListOf<OrbitalBody>()
        var currentDistance = sun.radius + ORBIT_PADDING

        for (orbitalBody in sortedBodies) {
            val planetSize = orbitalBody.orbitalConfig.size
            val ellipseRatio = orbitalBody.orbitalConfig.ellipseRatio

            val newDistance = currentDistance + planetSize + PLANET_PADDING

            val updatedConfig = orbitalBody.orbitalConfig.copy(
                distance = newDistance
            )

            val updatedBody = orbitalBody.copy(
                orbitalConfig = updatedConfig
            )

            updatedBodies.add(updatedBody)

            val maxOrbitRadius = newDistance * ellipseRatio
            currentDistance = maxOrbitRadius + planetSize + ORBIT_PADDING
        }

        return OrbitalSystem(sun, updatedBodies)
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