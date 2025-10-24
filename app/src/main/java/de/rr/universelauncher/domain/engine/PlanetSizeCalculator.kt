package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.AppInfo
import kotlin.math.ln

object PlanetSizeCalculator {

    private const val BASE_MIN_PLANET_RADIUS = 28f
    private const val BASE_MAX_PLANET_RADIUS = 52f

    fun calculatePlanetSizes(apps: List<AppInfo>): Map<String, Float> {
        if (apps.isEmpty()) return emptyMap()

        val planetCount = apps.size
        val (minRadius, maxRadius) = calculateSizeRange(planetCount)

        val launchCounts = apps.map { it.launchCount }
        val maxLaunchCount = launchCounts.maxOrNull() ?: 0

        if (maxLaunchCount == 0) {
            return apps.associate { it.packageName to minRadius }
        }

        val minLaunchCount = launchCounts.minOrNull() ?: 0

        return apps.associate { app ->
            val normalizedSize = normalizeSize(
                launchCount = app.launchCount,
                minLaunchCount = minLaunchCount,
                maxLaunchCount = maxLaunchCount,
                minRadius = minRadius,
                maxRadius = maxRadius
            )
            app.packageName to normalizedSize
        }
    }

    private fun calculateSizeRange(planetCount: Int): Pair<Float, Float> {
        if (planetCount <= 1) {
            return Pair(BASE_MIN_PLANET_RADIUS, BASE_MAX_PLANET_RADIUS)
        }

        val scaleFactor = when {
            planetCount <= 3 -> 1.0f
            planetCount <= 5 -> 0.85f
            planetCount <= 7 -> 0.7f
            planetCount <= 9 -> 0.6f
            else -> 0.5f
        }

        val minRadius = BASE_MIN_PLANET_RADIUS * scaleFactor
        val maxRadius = BASE_MAX_PLANET_RADIUS * scaleFactor

        return Pair(minRadius, maxRadius)
    }

    private fun normalizeSize(
        launchCount: Int,
        minLaunchCount: Int,
        maxLaunchCount: Int,
        minRadius: Float,
        maxRadius: Float
    ): Float {
        if (maxLaunchCount == minLaunchCount) {
            return (minRadius + maxRadius) / 2f
        }

        val normalizedValue = if (launchCount == 0) {
            0f
        } else {
            val logValue = ln((launchCount + 1).toFloat())
            val logMin = ln((minLaunchCount + 1).toFloat())
            val logMax = ln((maxLaunchCount + 1).toFloat())

            ((logValue - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
        }

        return minRadius + (normalizedValue * (maxRadius - minRadius))
    }

    fun getMinPlanetSize(): Float = BASE_MIN_PLANET_RADIUS
    fun getMaxPlanetSize(): Float = BASE_MAX_PLANET_RADIUS
}