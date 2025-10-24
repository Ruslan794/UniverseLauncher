package de.rr.universelauncher.domain.model

import androidx.compose.ui.graphics.Color

data class Star(
    val radius: Float,
    val color: Color
)


val defaultStar = Star(
    radius = 20f,
    color = Color(0xFFFFD700)
)
