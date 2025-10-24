package de.rr.universelauncher.domain.model

import androidx.compose.ui.graphics.Color

data class Star(
    val radius: Float,
    val color: Color,
    val deadZone: Float
)

val defaultStar = Star(
    radius = 40f,
    color = Color(0xFFFFD700),
    deadZone = 40f
)