package de.rr.universelauncher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_preferences")

@Singleton
class LauncherPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    fun getSelectedApps(): Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[AppSettings.SELECTED_APPS] ?: emptySet()
    }

    suspend fun setSelectedApps(apps: Set<String>) {
        try {
            dataStore.edit { preferences ->
                preferences[AppSettings.SELECTED_APPS] = apps
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set selected apps", e)
            throw e // Re-throw to let the caller handle the error
        }
    }

    fun getAppLaunchCounts(): Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val launchCountsString = preferences[AppSettings.APP_LAUNCH_COUNT] ?: emptySet()
        launchCountsString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else {
                "" to 0
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun incrementAppLaunchCount(packageName: String) {
        try {
            dataStore.edit { preferences ->
                val currentCounts = preferences[AppSettings.APP_LAUNCH_COUNT] ?: emptySet()
                val currentCount = currentCounts.find { it.startsWith("$packageName:") }
                    ?.substringAfter(":")?.toIntOrNull() ?: 0
                val newCount = currentCount + 1
                
                val updatedCounts = currentCounts.filterNot { it.startsWith("$packageName:") }.toMutableSet()
                updatedCounts.add("$packageName:$newCount")
                preferences[AppSettings.APP_LAUNCH_COUNT] = updatedCounts
            }
        } catch (e: Exception) {
            // Log error but don't throw to avoid breaking the app
            android.util.Log.e("LauncherPreferences", "Failed to increment launch count for $packageName", e)
        }
    }

    suspend fun getAppLaunchCount(packageName: String): Int {
        return dataStore.data.map { preferences ->
            val launchCountsString = preferences[AppSettings.APP_LAUNCH_COUNT] ?: emptySet()
            launchCountsString.find { it.startsWith("$packageName:") }
                ?.substringAfter(":")?.toIntOrNull() ?: 0
        }.first()
    }

    fun getAppOrder(): Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val orderString = preferences[AppSettings.APP_ORDER] ?: emptySet()
        orderString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else {
                "" to 0
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setAppOrder(appOrder: Map<String, Int>) {
        try {
            dataStore.edit { preferences ->
                val orderSet = appOrder.map { "${it.key}:${it.value}" }.toSet()
                preferences[AppSettings.APP_ORDER] = orderSet
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set app order", e)
            throw e
        }
    }

    fun getAppOrbitSpeeds(): Flow<Map<String, Float>> = dataStore.data.map { preferences ->
        val speedsString = preferences[AppSettings.APP_ORBIT_SPEEDS] ?: emptySet()
        speedsString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toFloatOrNull() ?: 0f)
            } else {
                "" to 0f
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setAppOrbitSpeed(packageName: String, speed: Float) {
        try {
            dataStore.edit { preferences ->
                val currentSpeeds = preferences[AppSettings.APP_ORBIT_SPEEDS] ?: emptySet()
                val updatedSpeeds = currentSpeeds.filterNot { it.startsWith("$packageName:") }.toMutableSet()
                updatedSpeeds.add("$packageName:$speed")
                preferences[AppSettings.APP_ORBIT_SPEEDS] = updatedSpeeds
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set orbit speed for $packageName", e)
            throw e
        }
    }

    suspend fun getAppOrbitSpeed(packageName: String): Float? {
        return dataStore.data.map { preferences ->
            val speedsString = preferences[AppSettings.APP_ORBIT_SPEEDS] ?: emptySet()
            speedsString.find { it.startsWith("$packageName:") }
                ?.substringAfter(":")?.toFloatOrNull()
        }.first()
    }

    fun getAppPlanetSizes(): Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val sizesString = preferences[AppSettings.APP_PLANET_SIZES] ?: emptySet()
        sizesString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                "" to "MEDIUM"
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setAppPlanetSize(packageName: String, size: String) {
        try {
            dataStore.edit { preferences ->
                val currentSizes = preferences[AppSettings.APP_PLANET_SIZES] ?: emptySet()
                val updatedSizes = currentSizes.filterNot { it.startsWith("$packageName:") }.toMutableSet()
                updatedSizes.add("$packageName:$size")
                preferences[AppSettings.APP_PLANET_SIZES] = updatedSizes
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set planet size for $packageName", e)
            throw e
        }
    }

    suspend fun getAppPlanetSize(packageName: String): String? {
        return dataStore.data.map { preferences ->
            val sizesString = preferences[AppSettings.APP_PLANET_SIZES] ?: emptySet()
            sizesString.find { it.startsWith("$packageName:") }
                ?.substringAfter(":")
        }.first()
    }
}
