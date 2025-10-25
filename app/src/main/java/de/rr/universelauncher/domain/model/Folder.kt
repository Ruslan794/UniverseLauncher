package de.rr.universelauncher.domain.model

import androidx.compose.ui.geometry.Offset

data class Folder(
    val id: String,
    val name: String,
    val appPackageNames: Set<String>,
    val position: Offset
)

fun createDefaultFolders(screenWidth: Float, screenHeight: Float): List<Folder> {
    return listOf(
        Folder(
            id = "folder_1",
            name = "Productivity",
            appPackageNames = setOf(
                "com.android.chrome",
                "com.google.android.gm",
                "com.microsoft.office.officehubrow",
                "com.google.android.apps.docs",
                "com.google.android.apps.sheets"
            ),
            position = Offset(screenWidth * 0.75f, screenHeight * 0.2f)
        ),
        Folder(
            id = "folder_2", 
            name = "Entertainment",
            appPackageNames = setOf(
                "com.netflix.mediaclient",
                "com.spotify.music",
                "com.google.android.youtube",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus"
            ),
            position = Offset(screenWidth * 0.25f, screenHeight * 0.35f)
        ),
        Folder(
            id = "folder_3",
            name = "Utilities", 
            appPackageNames = setOf(
                "com.android.settings",
                "com.google.android.apps.photos",
                "com.android.camera2",
                "com.google.android.calculator",
                "com.android.calendar"
            ),
            position = Offset(screenWidth * 0.6f, screenHeight * 0.7f)
        )
    )
}
