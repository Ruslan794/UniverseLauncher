package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Size
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.Star
import de.rr.universelauncher.domain.model.PlanetSize
import de.rr.universelauncher.domain.engine.PlanetRenderingEngine
import kotlin.math.min

data class OrbitLayout(
    val orbitDistance: Float,
    val planetSizeCategory: PlanetSize
)

object OrbitLayoutCalculator {

    private const val EDGE_PADDING = 20f

    fun calculateOrbitLayouts(
        star: Star,
        orbitalBodies: List<OrbitalBody>,
        canvasSize: Size
    ): List<OrbitLayout> {
        if (orbitalBodies.isEmpty()) return emptyList()

        val canvasAnalysis = PlanetRenderingEngine.analyzeCanvas(canvasSize, star)
        val sizeCalculation = PlanetRenderingEngine.calculateSizes(orbitalBodies, canvasAnalysis)

        val layouts = mutableListOf<OrbitLayout>()

        orbitalBodies.forEachIndexed { index, body ->
            val orbitDistance = canvasAnalysis.minOffset + 
                (index * sizeCalculation.radialSlotSize) + 
                sizeCalculation.radialSlotSize / 2f

            layouts.add(OrbitLayout(
                orbitDistance = orbitDistance,
                planetSizeCategory = body.orbitalConfig.sizeCategory
            ))
        }

        return layouts
    }
}