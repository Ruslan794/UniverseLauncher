package de.rr.universelauncher.feature.orbit.presentation

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.rr.universelauncher.core.physics.domain.engine.OrbitalPhysics
import de.rr.universelauncher.core.physics.domain.model.Planet
import de.rr.universelauncher.core.physics.domain.model.Star
import de.rr.universelauncher.core.ui.theme.SpaceBackground
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun UniverseScreen(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val system = remember { OrbitalPhysics.createSampleSolarSystem() }
    
    var animationTime by remember { mutableStateOf(0.0) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    
    val scaleFactor = 50f
    
    LaunchedEffect(Unit) {
        while (true) {
            animationTime += 0.1
            delay(16)
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
            
            drawOrbitalPaths(system, center, scaleFactor)
            
            drawStar(system.star, center)
            
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

private fun DrawScope.drawOrbitalPaths(
    system: de.rr.universelauncher.core.physics.domain.model.OrbitalSystem,
    center: Offset,
    scaleFactor: Float
) {
    system.planets.forEach { planet ->
        val a = (planet.semiMajorAxis * scaleFactor).toFloat()
        val b = (a * sqrt(1.0 - planet.eccentricity * planet.eccentricity)).toFloat()
        
        drawOval(
            color = Color.White.copy(alpha = 0.1f),
            topLeft = Offset(center.x - a, center.y - b),
            size = androidx.compose.ui.geometry.Size(a * 2, b * 2),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawStar(
    star: Star,
    center: Offset
) {
    drawCircle(
        color = star.color.copy(alpha = 0.3f),
        radius = star.radius * 2,
        center = center
    )
    
    drawCircle(
        color = star.color,
        radius = star.radius,
        center = center
    )
}

private fun DrawScope.drawPlanet(
    planet: Planet,
    position: Offset
) {
    drawCircle(
        color = planet.color.copy(alpha = 0.2f),
        radius = planet.radius * 1.5f,
        center = position
    )
    
    drawCircle(
        color = planet.color,
        radius = planet.radius,
        center = position
    )
}