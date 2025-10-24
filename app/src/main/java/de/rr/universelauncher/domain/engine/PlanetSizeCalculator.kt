package de.rr.universelauncher.domain.engine

import de.rr.universelauncher.domain.model.AppInfo
import kotlin.math.ln
import kotlin.math.sqrt

object PlanetSizeCalculator {

    private const val MIN_PLANET_SIZE = 20f
    private const val MAX_PLANET_SIZE = 60f

    fun calculatePlanetSizes(apps: List<AppInfo>): Map<String, Float> {
        if (apps.isEmpty()) return emptyMap()

        val launchCounts = apps.map { it.launchCount }
        val maxLaunchCount = launchCounts.maxOrNull() ?: 0
        val minLaunchCount = launchCounts.minOrNull() ?: 0

        if (maxLaunchCount == 0) {
            return apps.associate { it.packageName to MIN_PLANET_SIZE }
        }

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
            return (MIN_PLANET_SIZE + MAX_PLANET_SIZE) / 2f
        }

        val normalizedValue = when {
            launchCount == 0 -> 0f
            else -> {
                val logValue = ln((launchCount + 1).toFloat())
                val logMin = ln((minLaunchCount + 1).toFloat())
                val logMax = ln((maxLaunchCount + 1).toFloat())

                if (logMax == logMin) {
                    0.5f
                } else {
                    (logValue - logMin) / (logMax - logMin)
                }
            }
        }

        val size = MIN_PLANET_SIZE + (normalizedValue * (MAX_PLANET_SIZE - MIN_PLANET_SIZE))
        return size.coerceIn(MIN_PLANET_SIZE, MAX_PLANET_SIZE)
    }

    fun getMinPlanetSize(): Float = MIN_PLANET_SIZE
    fun getMaxPlanetSize(): Float = MAX_PLANET_SIZE
}