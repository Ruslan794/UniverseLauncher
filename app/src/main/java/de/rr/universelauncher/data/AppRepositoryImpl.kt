package de.rr.universelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.rr.universelauncher.domain.model.AppInfo
import de.rr.universelauncher.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        packages.mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)

                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon
                )
            } catch (e: Exception) {
                Log.e("AppRepository", "Error loading app: ${resolveInfo.activityInfo.packageName}", e)
                null
            }
        }
    }

    override fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to launch app: $packageName", e)
        }
    }
}