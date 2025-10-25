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
import de.rr.universelauncher.domain.model.FolderData
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
            throw e
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

    fun getFolders(): Flow<List<FolderData>> = dataStore.data.map { preferences ->
        val folderStrings = preferences[AppSettings.FOLDER_DATA] ?: emptySet()
        folderStrings.mapNotNull { folderString ->
            val parts = folderString.split(":")
            if (parts.size == 3) {
                val id = parts[0]
                val appPackages = parts[1].split(",").filter { it.isNotEmpty() }.toSet()
                val name = parts[2]
                FolderData(id, name, appPackages)
            } else null
        }
    }

    suspend fun saveFolders(folders: List<FolderData>) {
        try {
            dataStore.edit { preferences ->
                val folderStrings = folders.map { folder ->
                    "${folder.id}:${folder.appPackageNames.joinToString(",")}:${folder.name}"
                }.toSet()
                preferences[AppSettings.FOLDER_DATA] = folderStrings
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to save folders", e)
            throw e
        }
    }

    suspend fun updateFolderName(folderId: String, newName: String) {
        try {
            dataStore.edit { preferences ->
                val currentFolders = preferences[AppSettings.FOLDER_DATA] ?: emptySet()
                val updatedFolders = currentFolders.map { folderString ->
                    val parts = folderString.split(":")
                    if (parts.size == 3 && parts[0] == folderId) {
                        "${parts[0]}:${parts[1]}:$newName"
                    } else folderString
                }.toSet()
                preferences[AppSettings.FOLDER_DATA] = updatedFolders
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to update folder name", e)
            throw e
        }
    }

    fun getFolderSelectedApps(folderId: String): Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[stringSetPreferencesKey("folder_${folderId}_selected_apps")] ?: emptySet()
    }

    suspend fun setFolderSelectedApps(folderId: String, apps: Set<String>) {
        try {
            dataStore.edit { preferences ->
                preferences[stringSetPreferencesKey("folder_${folderId}_selected_apps")] = apps
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set folder selected apps", e)
            throw e
        }
    }

    fun getFolderAppOrder(folderId: String): Flow<Map<String, Int>> = dataStore.data.map { preferences ->
        val orderString = preferences[stringSetPreferencesKey("folder_${folderId}_app_order")] ?: emptySet()
        orderString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else {
                "" to 0
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setFolderAppOrder(folderId: String, appOrder: Map<String, Int>) {
        try {
            dataStore.edit { preferences ->
                val orderSet = appOrder.map { "${it.key}:${it.value}" }.toSet()
                preferences[stringSetPreferencesKey("folder_${folderId}_app_order")] = orderSet
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set folder app order", e)
            throw e
        }
    }

    fun getFolderAppOrbitSpeeds(folderId: String): Flow<Map<String, Float>> = dataStore.data.map { preferences ->
        val speedsString = preferences[stringSetPreferencesKey("folder_${folderId}_orbit_speeds")] ?: emptySet()
        speedsString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toFloatOrNull() ?: 0f)
            } else {
                "" to 0f
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setFolderAppOrbitSpeed(folderId: String, packageName: String, speed: Float) {
        try {
            dataStore.edit { preferences ->
                val currentSpeeds = preferences[stringSetPreferencesKey("folder_${folderId}_orbit_speeds")] ?: emptySet()
                val updatedSpeeds = currentSpeeds.filterNot { it.startsWith("$packageName:") }.toMutableSet()
                updatedSpeeds.add("$packageName:$speed")
                preferences[stringSetPreferencesKey("folder_${folderId}_orbit_speeds")] = updatedSpeeds
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set folder orbit speed", e)
            throw e
        }
    }

    suspend fun getFolderAppOrbitSpeed(folderId: String, packageName: String): Float? {
        return dataStore.data.map { preferences ->
            val speedsString = preferences[stringSetPreferencesKey("folder_${folderId}_orbit_speeds")] ?: emptySet()
            speedsString.find { it.startsWith("$packageName:") }
                ?.substringAfter(":")?.toFloatOrNull()
        }.first()
    }

    fun getFolderAppPlanetSizes(folderId: String): Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val sizesString = preferences[stringSetPreferencesKey("folder_${folderId}_planet_sizes")] ?: emptySet()
        sizesString.associate { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                "" to "MEDIUM"
            }
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun setFolderAppPlanetSize(folderId: String, packageName: String, size: String) {
        try {
            dataStore.edit { preferences ->
                val currentSizes = preferences[stringSetPreferencesKey("folder_${folderId}_planet_sizes")] ?: emptySet()
                val updatedSizes = currentSizes.filterNot { it.startsWith("$packageName:") }.toMutableSet()
                updatedSizes.add("$packageName:$size")
                preferences[stringSetPreferencesKey("folder_${folderId}_planet_sizes")] = updatedSizes
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherPreferences", "Failed to set folder planet size", e)
            throw e
        }
    }

    suspend fun getFolderAppPlanetSize(folderId: String, packageName: String): String? {
        return dataStore.data.map { preferences ->
            val sizesString = preferences[stringSetPreferencesKey("folder_${folderId}_planet_sizes")] ?: emptySet()
            sizesString.find { it.startsWith("$packageName:") }
                ?.substringAfter(":")
        }.first()
    }
}
