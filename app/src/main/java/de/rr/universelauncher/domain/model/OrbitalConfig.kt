package de.rr.universelauncher.domain.model

import androidx.compose.ui.graphics.Color

data class OrbitalConfig(
    val distance: Float,
    val orbitDuration: Float,
    val sizeCategory: PlanetSize,
    val startAngle: Float = 0f,
    val color: Color,
    val ellipseRatio: Float = 1.5f
)