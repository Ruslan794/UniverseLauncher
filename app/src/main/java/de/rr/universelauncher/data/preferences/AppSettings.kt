package de.rr.universelauncher.data.preferences

import androidx.datastore.preferences.core.stringSetPreferencesKey

object AppSettings {
    val SELECTED_APPS = stringSetPreferencesKey("selected_apps")
    val APP_LAUNCH_COUNT = stringSetPreferencesKey("app_launch_counts")
    val APP_ORDER = stringSetPreferencesKey("app_order")
    val APP_ORBIT_SPEEDS = stringSetPreferencesKey("app_orbit_speeds")
}
