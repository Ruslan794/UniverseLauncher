package de.rr.universelauncher.presentation.universe.components.cache

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import de.rr.universelauncher.domain.engine.OrbitalPhysics
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem

@Composable
fun rememberOrbitPathCache(
    orbitalSystem: OrbitalSystem?,
    center: Offset
): OrbitPathCache {
    return remember(orbitalSystem, center) {
        orbitalSystem?.let { system ->
            OrbitPathCache.create(system, center)
        } ?: OrbitPathCache.empty()
    }
}

class OrbitPathCache private constructor(
    private val paths: Map<String, Path>
) {
    companion object {
        fun create(orbitalSystem: OrbitalSystem, center: Offset): OrbitPathCache {
            val paths = mutableMapOf<String, Path>()
            
            orbitalSystem.orbitalBodies.forEach { orbitalBody ->
                val pathPoints = OrbitalPhysics.calculateOrbitPathPoints(orbitalBody)
                
                if (pathPoints.isNotEmpty()) {
                    val path = Path()
                    val firstPoint = pathPoints[0]
                    path.moveTo(center.x + firstPoint.first, center.y + firstPoint.second)
                    
                    for (i in 1 until pathPoints.size) {
                        val point = pathPoints[i]
                        path.lineTo(center.x + point.first, center.y + point.second)
                    }
                    
                    path.close()
                    paths[orbitalBody.appInfo.packageName] = path
                }
            }
            
            return OrbitPathCache(paths)
        }
        
        fun empty(): OrbitPathCache = OrbitPathCache(emptyMap())
    }
    
    fun getPath(orbitalBody: OrbitalBody): Path? {
        return paths[orbitalBody.appInfo.packageName]
    }
    
    fun getAllPaths(): Collection<Path> = paths.values
}
