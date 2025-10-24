package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.AppInfo
import kotlin.math.ln

object PlanetSizeCalculator {

    private const val MIN_PLANET_RADIUS = 28f
    private const val MAX_PLANET_RADIUS = 52f

    fun calculatePlanetSizes(apps: List<AppInfo>): Map<String, Float> {
        if (apps.isEmpty()) return emptyMap()

        val launchCounts = apps.map { it.launchCount }
        val maxLaunchCount = launchCounts.maxOrNull() ?: 0

        if (maxLaunchCount == 0) {
            return apps.associate { it.packageName to MIN_PLANET_RADIUS }
        }

        val minLaunchCount = launchCounts.minOrNull() ?: 0

        return apps.associate { app ->
            val normalizedSize = normalizeSize(
                launchCount = app.launchCount,
                minLaunchCount = minLaunchCount,
                maxLaunchCount = maxLaunchCount
            )
            app.packageName to normalizedSize
        }
    }

    private fun normalizeSize(
        launchCount: Int,
        minLaunchCount: Int,
        maxLaunchCount: Int
    ): Float {
        if (maxLaunchCount == minLaunchCount) {
            return (MIN_PLANET_RADIUS + MAX_PLANET_RADIUS) / 2f
        }

        val normalizedValue = if (launchCount == 0) {
            0f
        } else {
            val logValue = ln((launchCount + 1).toFloat())
            val logMin = ln((minLaunchCount + 1).toFloat())
            val logMax = ln((maxLaunchCount + 1).toFloat())

            ((logValue - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
        }

        return MIN_PLANET_RADIUS + (normalizedValue * (MAX_PLANET_RADIUS - MIN_PLANET_RADIUS))
    }

    fun getMinPlanetSize(): Float = MIN_PLANET_RADIUS
    fun getMaxPlanetSize(): Float = MAX_PLANET_RADIUS
}