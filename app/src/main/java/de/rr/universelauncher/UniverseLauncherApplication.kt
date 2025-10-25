package de.rr.universelauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.rr.universelauncher.domain.manager.AppDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class UniverseLauncherApplication : Application() {

    @Inject
    lateinit var appDataManager: AppDataManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            try {
                appDataManager.preloadData()
            } catch (e: Exception) {
                android.util.Log.e("UniverseLauncher", "Failed to preload data", e)
            }
        }
    }
}
