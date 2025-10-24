package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Size
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.Star
import kotlin.math.min

data class OrbitLayout(
    val orbitDistance: Float,
    val planetSize: Float,
    val maxRadiusX: Float,
    val maxRadiusY: Float
)

object OrbitLayoutCalculator {

    private const val EDGE_PADDING = 80f
    private const val ORBIT_PADDING = 30f
    private const val PLANET_SAFETY_MARGIN = 15f

    fun calculateOrbitLayouts(
        star: Star,
        orbitalBodies: List<OrbitalBody>,
        canvasSize: Size
    ): List<OrbitLayout> {
        if (orbitalBodies.isEmpty()) return emptyList()

        val canvasRadius = min(canvasSize.width, canvasSize.height) / 2f
        val availableRadius = canvasRadius - EDGE_PADDING

        val startRadius = star.radius + star.deadZone + ORBIT_PADDING

        if (availableRadius <= startRadius) {
            return orbitalBodies.map { body ->
                OrbitLayout(
                    orbitDistance = startRadius,
                    planetSize = body.orbitalConfig.size,
                    maxRadiusX = startRadius * body.orbitalConfig.ellipseRatio,
                    maxRadiusY = startRadius
                )
            }
        }

        val layouts = mutableListOf<OrbitLayout>()
        var currentRadius = startRadius

        orbitalBodies.forEach { body ->
            val ellipseRatio = body.orbitalConfig.ellipseRatio
            val planetSize = body.orbitalConfig.size

            val requiredRadiusForPlanet = planetSize + PLANET_SAFETY_MARGIN

            if (currentRadius + requiredRadiusForPlanet > availableRadius) {
                currentRadius = availableRadius - requiredRadiusForPlanet
                if (currentRadius < startRadius) {
                    currentRadius = startRadius
                }
            }

            val maxRadiusX = currentRadius * ellipseRatio
            val maxRadiusY = currentRadius

            val layout = OrbitLayout(
                orbitDistance = currentRadius,
                planetSize = planetSize,
                maxRadiusX = maxRadiusX,
                maxRadiusY = maxRadiusY
            )
            layouts.add(layout)

            val nextMinRadius = currentRadius + requiredRadiusForPlanet + ORBIT_PADDING
            currentRadius = nextMinRadius
        }

        return optimizeSpacing(layouts, availableRadius, startRadius)
    }

    private fun optimizeSpacing(
        layouts: List<OrbitLayout>,
        availableRadius: Float,
        startRadius: Float
    ): List<OrbitLayout> {
        if (layouts.isEmpty()) return layouts

        val lastLayout = layouts.last()
        val lastRequiredRadius = lastLayout.orbitDistance + lastLayout.planetSize + PLANET_SAFETY_MARGIN

        if (lastRequiredRadius > availableRadius) {
            return compressLayouts(layouts, availableRadius, startRadius)
        }

        val totalUsedSpace = lastRequiredRadius - startRadius
        val totalAvailableSpace = availableRadius - startRadius

        if (totalUsedSpace < totalAvailableSpace * 0.6f) {
            return expandLayouts(layouts, availableRadius, startRadius)
        }

        return layouts
    }

    private fun expandLayouts(
        layouts: List<OrbitLayout>,
        availableRadius: Float,
        startRadius: Float
    ): List<OrbitLayout> {
        val totalAvailableSpace = availableRadius - startRadius

        return layouts.mapIndexed { index, layout ->
            val progress = if (layouts.size == 1) 0.4f else index.toFloat() / (layouts.size - 1)
            val newDistance = startRadius + (totalAvailableSpace * progress * 0.85f)

            layout.copy(
                orbitDistance = newDistance,
                maxRadiusX = newDistance * (layout.maxRadiusX / layout.orbitDistance),
                maxRadiusY = newDistance
            )
        }
    }

    private fun compressLayouts(
        layouts: List<OrbitLayout>,
        availableRadius: Float,
        startRadius: Float
    ): List<OrbitLayout> {
        val totalAvailableSpace = availableRadius - startRadius
        var currentRadius = startRadius

        return layouts.map { layout ->
            val planetSpace = layout.planetSize + PLANET_SAFETY_MARGIN

            if (currentRadius + planetSpace > availableRadius) {
                currentRadius = availableRadius - planetSpace
            }

            val compressedLayout = layout.copy(
                orbitDistance = currentRadius,
                maxRadiusX = currentRadius * (layout.maxRadiusX / layout.orbitDistance),
                maxRadiusY = currentRadius
            )

            currentRadius += planetSpace + ORBIT_PADDING * 0.5f

            compressedLayout
        }
    }
}