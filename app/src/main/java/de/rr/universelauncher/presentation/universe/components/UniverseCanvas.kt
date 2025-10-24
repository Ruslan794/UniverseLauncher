package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import de.rr.universelauncher.presentation.universe.components.cache.rememberIconCache
import de.rr.universelauncher.presentation.universe.components.cache.rememberOrbitPathCache
import kotlinx.coroutines.delay

@Composable
fun UniverseCanvas(
    orbitalSystem: OrbitalSystem,
    onPlanetTapped: (OrbitalBody) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconCache = rememberIconCache(orbitalSystem)
    
    LaunchedEffect(orbitalSystem) {
        iconCache.preloadIcons(orbitalSystem)
    }
    
    var currentAnimationTime by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            currentAnimationTime += 0.016f
        }
    }
    
    val orbitPathCache = rememberOrbitPathCache(orbitalSystem, center)
    
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(orbitalSystem) {
                detectTapGestures(
                    onTap = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                            val position = OrbitalPhysics.calculateOrbitalBodyPosition(
                                orbitalBody = orbitalBody,
                                timeSeconds = currentAnimationTime
                            )

                            val screenX = centerX + position.first
                            val screenY = centerY + position.second
                            val planetCenter = Offset(screenX, screenY)

                            val distance = (offset - planetCenter).getDistance()
                            if (distance <= orbitalBody.orbitalConfig.size * 2) {
                                onPlanetTapped(orbitalBody)
                            }
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val currentCenter = Offset(centerX, centerY)
        
        if (center != currentCenter) {
            center = currentCenter
        }
        
        UniverseRenderer.drawUniverse(
            drawScope = this,
            orbitalSystem = orbitalSystem,
            animationTime = currentAnimationTime,
            center = currentCenter,
            iconCache = iconCache,
            orbitPathCache = orbitPathCache
        )
    }
}
