package de.rr.universelauncher.presentation.universe.components.cache

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import de.rr.universelauncher.domain.model.OrbitalBody
import de.rr.universelauncher.domain.model.OrbitalSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberIconCache(
    orbitalSystem: OrbitalSystem
): IconCache {
    return remember(orbitalSystem.orbitalBodies.map { it.appInfo.packageName }) { 
        IconCache() 
    }
}

class IconCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun getIconBitmapSync(orbitalBody: OrbitalBody): ImageBitmap? {
        // Use a standard size for caching - will be scaled during rendering
        val standardSize = 64 // Standard icon size for better quality
        val cacheKey = "${orbitalBody.appInfo.packageName}_${standardSize}"
        return cache[cacheKey]
    }

    suspend fun getIconBitmap(orbitalBody: OrbitalBody): ImageBitmap? {
        // Use a standard size for caching - will be scaled during rendering
        val standardSize = 64 // Standard icon size for better quality
        val cacheKey = "${orbitalBody.appInfo.packageName}_${standardSize}"

        return cache[cacheKey] ?: run {
            val bitmap = convertDrawableToBitmap(orbitalBody.appInfo.icon, standardSize)
            if (bitmap != null) {
                cache[cacheKey] = bitmap
            }
            bitmap
        }
    }

    suspend fun preloadIcons(orbitalSystem: OrbitalSystem) {
        orbitalSystem.orbitalBodies.forEach { orbitalBody ->
            getIconBitmap(orbitalBody)
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private suspend fun convertDrawableToBitmap(
        drawable: Drawable,
        size: Int
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}