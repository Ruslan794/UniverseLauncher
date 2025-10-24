package de.rr.universelauncher.domain.model

data class OrbitalSystem(
    val star: Star,
    val orbitalBodies: List<OrbitalBody>
)
