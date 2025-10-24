package de.rr.universelauncher.core.physics.domain.model

import androidx.compose.ui.graphics.Color

data class Planet(
    val name: String,
    val mass: Double,
    val radius: Float,
    val color: Color,
    val semiMajorAxis: Double,
    val eccentricity: Double,
    val inclination: Double,
    val argumentOfPeriapsis: Double,
    val meanAnomaly: Double,
    val orbitalPeriod: Double
)

