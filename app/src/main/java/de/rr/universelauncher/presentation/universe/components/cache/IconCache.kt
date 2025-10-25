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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val maxCacheSize = 50
    private val standardSize = 64

    fun getIconBitmapSync(orbitalBody: OrbitalBody): ImageBitmap? {
        val cacheKey = "${orbitalBody.appInfo.packageName}_$standardSize"
        return cache[cacheKey]
    }

    suspend fun preloadIcons(orbitalSystem: OrbitalSystem) = withContext(Dispatchers.Default) {
        orbitalSystem.orbitalBodies.map { orbitalBody ->
            async {
                val cacheKey = "${orbitalBody.appInfo.packageName}_$standardSize"
                if (cache[cacheKey] == null && cache.size < maxCacheSize) {
                    convertDrawableToBitmap(orbitalBody.appInfo.icon, standardSize)?.let { bitmap ->
                        synchronized(cache) {
                            cache[cacheKey] = bitmap
                        }
                    }
                }
            }
        }.awaitAll()
    }

    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }

    private suspend fun convertDrawableToBitmap(
        drawable: Drawable,
        size: Int
    ): ImageBitmap? = withContext(Dispatchers.Default) {
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