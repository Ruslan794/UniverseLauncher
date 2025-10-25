package de.rr.universelauncher.domain.model

data class FolderData(
    val id: String,
    val name: String,
    val appPackageNames: Set<String>
)
