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
}
