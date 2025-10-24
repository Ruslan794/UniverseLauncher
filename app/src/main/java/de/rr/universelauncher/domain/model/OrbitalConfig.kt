package de.rr.universelauncher.domain.model

import androidx.compose.ui.graphics.Color

data class OrbitalConfig(
    val distance: Float,        // Distance from sun in pixels
    val orbitDuration: Float,   // Seconds to complete one orbit
    val size: Float,            // Planet radius in pixels
    val startAngle: Float = 0f, // Starting position in degrees
    val color: Color           // Visual color
)
