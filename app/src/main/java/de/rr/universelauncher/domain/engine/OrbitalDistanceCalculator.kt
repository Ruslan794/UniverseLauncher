package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.OrbitalSystem
import androidx.compose.ui.geometry.Size

object OrbitalDistanceCalculator {

    fun distributeOrbitsInCanvas(orbitalSystem: OrbitalSystem, canvasSize: Size): OrbitalSystem {
        if (orbitalSystem.orbitalBodies.isEmpty()) {
            return orbitalSystem
        }

        val layouts = OrbitLayoutCalculator.calculateOrbitLayouts(
            star = orbitalSystem.star,
            orbitalBodies = orbitalSystem.orbitalBodies,
            canvasSize = canvasSize
        )

        val updatedBodies = orbitalSystem.orbitalBodies.mapIndexed { index, orbitalBody ->
            val layout = layouts.getOrNull(index) ?: return@mapIndexed orbitalBody

            orbitalBody.copy(
                orbitalConfig = orbitalBody.orbitalConfig.copy(
                    distance = layout.orbitDistance
                )
            )
        }

        return orbitalSystem.copy(orbitalBodies = updatedBodies)
    }

    fun recalculateOrbitalDistances(orbitalSystem: OrbitalSystem): OrbitalSystem {
        val defaultCanvasSize = Size(1080f, 2400f)
        return distributeOrbitsInCanvas(orbitalSystem, defaultCanvasSize)
    }
}