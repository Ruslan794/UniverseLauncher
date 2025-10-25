package de.rr.universelauncher.domain.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.rr.universelauncher.domain.model.Folder
import kotlin.math.*

object FolderRenderer {
    
    private const val FOLDER_STAR_RADIUS = 35f
    private const val FOLDER_PLANET_RADIUS = 8f
    private const val MIN_ORBIT_RADIUS = 60f
    private const val ORBIT_SPACING = 35f
    private const val MAX_ORBITS = 3
    
    private val PLANET_COLORS = listOf(
        Color(0xFF4A90E2),
        Color(0xFFE24A4A),
        Color(0xFF4AE24A),
        Color(0xFFE2E24A),
        Color(0xFFE24AE2),
        Color(0xFF4AE2E2)
    )
    
    fun drawFolder(
        drawScope: DrawScope,
        folder: Folder,
        animationTime: Float,
        planetCount: Int
    ) {
        drawFolderStar(drawScope, folder.position, animationTime)
        drawFolderPlanets(drawScope, folder.position, animationTime, planetCount)
    }
    
    private fun drawFolderStar(
        drawScope: DrawScope,
        position: Offset,
        animationTime: Float
    ) {
        val starColor = Color(0xFFFFD700)
        val pulse = (sin(animationTime * 0.8f) * 0.1f + 0.9f)
        
        val rings = 2
        for (i in rings downTo 1) {
            val alpha = 0.2f * (i.toFloat() / rings)
            val radius = FOLDER_STAR_RADIUS * (1f + (rings - i) * 0.15f) * pulse
            
            drawScope.drawCircle(
                color = starColor.copy(alpha = alpha),
                radius = radius,
                center = position
            )
        }
        
        drawScope.drawCircle(
            color = starColor,
            radius = FOLDER_STAR_RADIUS * 0.8f,
            center = position
        )
        
        drawScope.drawCircle(
            color = Color(0xFFFFF8DC).copy(alpha = 0.6f),
            radius = FOLDER_STAR_RADIUS * 0.5f,
            center = position
        )
    }
    
    private fun drawFolderPlanets(
        drawScope: DrawScope,
        center: Offset,
        animationTime: Float,
        planetCount: Int
    ) {
        if (planetCount <= 0) return
        
        val orbitConfigs = calculateOrbitDistribution(planetCount)
        
        orbitConfigs.forEachIndexed { orbitIndex, config ->
            val orbitRadius = MIN_ORBIT_RADIUS + (orbitIndex * ORBIT_SPACING)
            
            drawOrbitPath(drawScope, center, orbitRadius)
            
            config.planets.forEachIndexed { planetIndex, planetInfo ->
                val angle = planetInfo.angle + animationTime * planetInfo.speed
                val x = center.x + cos(angle).toFloat() * orbitRadius
                val y = center.y + sin(angle).toFloat() * orbitRadius
                
                val planetPosition = Offset(x, y)
                drawFolderPlanet(drawScope, planetPosition, FOLDER_PLANET_RADIUS, planetInfo.color)
            }
        }
    }
    
    private fun calculateOrbitDistribution(planetCount: Int): List<OrbitConfig> {
        val orbits = mutableListOf<OrbitConfig>()
        
        when {
            planetCount <= 2 -> {
                val planets = (0 until planetCount).map { index ->
                    PlanetInfo(
                        angle = (index * 2 * PI / planetCount).toFloat(),
                        speed = 0.8f + (index * 0.2f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(planets))
            }
            planetCount <= 4 -> {
                val innerPlanets = (0 until 2).map { index ->
                    PlanetInfo(
                        angle = (index * PI).toFloat(),
                        speed = 1.2f + (index * 0.3f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(innerPlanets))
                
                val outerPlanets = (2 until planetCount).map { index ->
                    PlanetInfo(
                        angle = ((index - 2) * 2 * PI / (planetCount - 2)).toFloat(),
                        speed = 0.9f + ((index - 2) * 0.2f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(outerPlanets))
            }
            else -> {
                val innerPlanets = (0 until 2).map { index ->
                    PlanetInfo(
                        angle = (index * PI).toFloat(),
                        speed = 1.5f + (index * 0.4f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(innerPlanets))
                
                val middlePlanets = (2 until 4).map { index ->
                    PlanetInfo(
                        angle = ((index - 2) * PI).toFloat(),
                        speed = 1.1f + ((index - 2) * 0.3f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(middlePlanets))
                
                val outerPlanets = (4 until planetCount).map { index ->
                    PlanetInfo(
                        angle = ((index - 4) * 2 * PI / (planetCount - 4)).toFloat(),
                        speed = 0.8f + ((index - 4) * 0.2f),
                        color = PLANET_COLORS[index % PLANET_COLORS.size]
                    )
                }
                orbits.add(OrbitConfig(outerPlanets))
            }
        }
        
        return orbits
    }
    
    private fun drawOrbitPath(
        drawScope: DrawScope,
        center: Offset,
        radius: Float
    ) {
        val path = androidx.compose.ui.graphics.Path()
        val steps = 64
        for (i in 0..steps) {
            val angle = (i * 2 * PI / steps).toFloat()
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            val point = Offset(x, y)
            
            if (i == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        
        drawScope.drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.1f),
            style = Stroke(width = 1f)
        )
    }
    
    private fun drawFolderPlanet(
        drawScope: DrawScope,
        position: Offset,
        radius: Float,
        color: Color
    ) {
        val shadowColor = color.copy(alpha = 0.6f)
        
        drawScope.drawCircle(
            color = shadowColor,
            radius = radius,
            center = position + Offset(radius * 0.1f, radius * 0.1f)
        )
        
        drawScope.drawCircle(
            color = color,
            radius = radius,
            center = position
        )
        
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius * 0.3f,
            center = position - Offset(radius * 0.3f, radius * 0.3f)
        )
    }
    
    private data class OrbitConfig(
        val planets: List<PlanetInfo>
    )
    
    private data class PlanetInfo(
        val angle: Float,
        val speed: Float,
        val color: Color
    )
}