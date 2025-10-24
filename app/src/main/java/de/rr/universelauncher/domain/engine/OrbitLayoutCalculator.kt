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

    private const val EDGE_PADDING = 20f

    fun calculateOrbitLayouts(
        star: Star,
        orbitalBodies: List<OrbitalBody>,
        canvasSize: Size
    ): List<OrbitLayout> {
        if (orbitalBodies.isEmpty()) return emptyList()

        val canvasRadius = min(canvasSize.width, canvasSize.height) / 2f
        val ellipseRatio = orbitalBodies.firstOrNull()?.orbitalConfig?.ellipseRatio ?: 1.3f

        val maxUsableRadius = canvasRadius - EDGE_PADDING
        val maxOrbitDistance = maxUsableRadius

        val firstPlanetRadius = orbitalBodies.first().orbitalConfig.size
        val minOrbitDistance = star.radius + star.deadZone + firstPlanetRadius + 30f

        val availableOrbitSpace = maxOrbitDistance - minOrbitDistance

        if (availableOrbitSpace <= 0) {
            return orbitalBodies.map { OrbitLayout(minOrbitDistance, it.orbitalConfig.size) }
        }

        if (orbitalBodies.size == 1) {
            return listOf(OrbitLayout(
                orbitDistance = minOrbitDistance + (availableOrbitSpace / 2f),
                planetSize = firstPlanetRadius
            ))
        }

        val layouts = mutableListOf<OrbitLayout>()
        val orbitStep = availableOrbitSpace / (orbitalBodies.size - 1)

        orbitalBodies.forEachIndexed { index, body ->
            val orbitDistance = minOrbitDistance + (orbitStep * index)

            layouts.add(OrbitLayout(
                orbitDistance = orbitDistance,
                planetSize = body.orbitalConfig.size
            ))
        }

        return layouts
    }
}