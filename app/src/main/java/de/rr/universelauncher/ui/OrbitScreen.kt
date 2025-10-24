package de.rr.universelauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.rr.universelauncher.physics.OrbitalPhysics
import de.rr.universelauncher.physics.Planet
import de.rr.universelauncher.physics.Star
import de.rr.universelauncher.ui.theme.SpaceBackground
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Screen displaying planets orbiting around a star with realistic physics
 */
@Composable
fun OrbitScreen(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val system = remember { OrbitalPhysics.createSampleSolarSystem() }
    
    // Animation state
    var animationTime by remember { mutableStateOf(0.0) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    
    // Scale factor for visualization (AU to pixels)
    val scaleFactor = 50f // 1 AU = 50 pixels
    
    // Animation loop
    LaunchedEffect(Unit) {
        while (true) {
            animationTime += 0.1 // Increment time by 0.1 days per frame
            delay(16) // ~60 FPS
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val center = Offset(centerX, centerY)
            
            // Draw orbital paths (ellipses)
            drawOrbitalPaths(system, center, scaleFactor)
            
            // Draw star
            drawStar(system.star, center)
            
            // Draw planets
            system.planets.forEach { planet ->
                val position = OrbitalPhysics.calculatePlanetPosition(
                    planet = planet,
                    timeDays = animationTime,
                    starMass = system.star.mass
                )
                
                val screenX = centerX + (position.first * scaleFactor).toFloat()
                val screenY = centerY + (position.second * scaleFactor).toFloat()
                
                drawPlanet(
                    planet = planet,
                    position = Offset(screenX, screenY)
                )
            }
        }
    }
}

/**
 * Draw orbital paths as ellipses
 */
private fun DrawScope.drawOrbitalPaths(
    system: de.rr.universelauncher.physics.OrbitalSystem,
    center: Offset,
    scaleFactor: Float
) {
    system.planets.forEach { planet ->
        val a = (planet.semiMajorAxis * scaleFactor).toFloat()
        val b = (a * sqrt(1.0 - planet.eccentricity * planet.eccentricity)).toFloat()
        
        // Draw orbital path as a thin ellipse
        drawOval(
            color = Color.White.copy(alpha = 0.1f),
            topLeft = Offset(center.x - a, center.y - b),
            size = androidx.compose.ui.geometry.Size(a * 2, b * 2),
            style = Stroke(width = 1f)
        )
    }
}

/**
 * Draw the central star
 */
private fun DrawScope.drawStar(
    star: Star,
    center: Offset
) {
    // Draw star glow effect
    drawCircle(
        color = star.color.copy(alpha = 0.3f),
        radius = star.radius * 2,
        center = center
    )
    
    // Draw main star
    drawCircle(
        color = star.color,
        radius = star.radius,
        center = center
    )
}

/**
 * Draw a planet at its current position
 */
private fun DrawScope.drawPlanet(
    planet: Planet,
    position: Offset
) {
    // Draw planet shadow/glow
    drawCircle(
        color = planet.color.copy(alpha = 0.2f),
        radius = planet.radius * 1.5f,
        center = position
    )
    
    // Draw main planet
    drawCircle(
        color = planet.color,
        radius = planet.radius,
        center = position
    )
}
