package de.rr.universelauncher.core.physics.domain.model

import de.rr.universelauncher.core.launcher.domain.model.AppInfo

data class OrbitalBody(
    val appInfo: AppInfo,
    val orbitalConfig: OrbitalConfig
)
