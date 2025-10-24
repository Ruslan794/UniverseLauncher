package de.rr.universelauncher.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val launchCount: Int = 0,
    val customOrbitSpeed: Float? = null,
    val customPlanetSize: PlanetSize? = null
)
