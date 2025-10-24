package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.OrbitalSystem
import androidx.compose.ui.geometry.Size

object OrbitalDistanceCalculator {

    fun distributeOrbitsInCanvas(orbitalSystem: OrbitalSystem, canvasSize: Size): OrbitalSystem {
        val star = orbitalSystem.star
        val orbitalBodies = orbitalSystem.orbitalBodies

        if (orbitalBodies.isEmpty()) {
            return orbitalSystem
        }

        val layouts = OrbitLayoutCalculator.calculateOrbitLayouts(
            star = star,
            orbitalBodies = orbitalBodies,
            canvasSize = canvasSize
        )

        if (layouts.size != orbitalBodies.size) {
            return orbitalSystem
        }

        val updatedBodies = orbitalBodies.mapIndexed { index, orbitalBody ->
            val layout = layouts[index]

            val updatedConfig = orbitalBody.orbitalConfig.copy(
                distance = layout.orbitDistance,
                size = layout.planetSize
            )

            orbitalBody.copy(orbitalConfig = updatedConfig)
        }

        return OrbitalSystem(star, updatedBodies)
    }

    fun recalculateOrbitalDistances(orbitalSystem: OrbitalSystem): OrbitalSystem {
        val defaultCanvasSize = Size(1080f, 2400f)
        return distributeOrbitsInCanvas(orbitalSystem, defaultCanvasSize)
    }
}