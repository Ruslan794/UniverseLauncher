package de.rr.universelauncher.core.physics.domain.model

data class OrbitalSystem(
    val star: Star,
    val orbitalBodies: List<OrbitalBody>
)

