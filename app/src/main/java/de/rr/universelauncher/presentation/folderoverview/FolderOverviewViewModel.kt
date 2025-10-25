package de.rr.universelauncher.presentation.folderoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rr.universelauncher.domain.model.Folder
import de.rr.universelauncher.domain.model.createDefaultFolders
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderOverviewViewModel @Inject constructor(
    private val appRepository: de.rr.universelauncher.domain.repository.AppRepository,
    private val launcherSettingsRepository: de.rr.universelauncher.domain.repository.LauncherSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FolderOverviewUiState(
            folders = emptyList(),
            isLoading = true,
            error = null,
            editingFolderId = null
        )
    )
    val uiState: StateFlow<FolderOverviewUiState> = _uiState.asStateFlow()

    private var currentScreenSize: Size = Size(1080f, 2400f)

    init {
        viewModelScope.launch {
            loadFolders()
        }
    }

    private suspend fun loadFolders() {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val savedFolders = launcherSettingsRepository.getFolders().first()

            val folders = if (savedFolders.isEmpty()) {
                val allApps = appRepository.getInstalledAppsWithLaunchCounts()

                if (allApps.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No apps found"
                        )
                    }
                    return
                }

                val folder1Apps = allApps.take(5).map { it.packageName }.toSet()
                val folder2Apps = allApps.drop(5).take(3).map { it.packageName }.toSet()
                val folder3Apps = allApps.drop(8).take(4).map { it.packageName }.toSet()

                val margin = 32f * 3.5f
                val availableWidth = currentScreenSize.width - (margin * 2)
                val availableHeight = currentScreenSize.height - (margin * 2)

                val defaultFolders = listOf(
                    Folder(
                        id = "folder_1",
                        name = "Ordner 1",
                        appPackageNames = folder1Apps,
                        position = androidx.compose.ui.geometry.Offset(
                            margin + availableWidth * 0.75f,
                            margin + availableHeight * 0.2f
                        )
                    ),
                    Folder(
                        id = "folder_2",
                        name = "Ordner 2",
                        appPackageNames = folder2Apps,
                        position = androidx.compose.ui.geometry.Offset(
                            margin + availableWidth * 0.25f,
                            margin + availableHeight * 0.35f
                        )
                    ),
                    Folder(
                        id = "folder_3",
                        name = "Ordner 3",
                        appPackageNames = folder3Apps,
                        position = androidx.compose.ui.geometry.Offset(
                            margin + availableWidth * 0.6f,
                            margin + availableHeight * 0.7f
                        )
                    )
                )

                val folderData = defaultFolders.map { folder ->
                    de.rr.universelauncher.domain.model.FolderData(folder.id, folder.name, folder.appPackageNames)
                }
                launcherSettingsRepository.saveFolders(folderData)

                defaultFolders
            } else {
                savedFolders.map { folderData ->
                    val selectedApps = launcherSettingsRepository.getFolderSelectedApps(folderData.id).first()
                    val actualAppPackages = if (selectedApps.isNotEmpty()) selectedApps else folderData.appPackageNames

                    Folder(
                        id = folderData.id,
                        name = folderData.name,
                        appPackageNames = actualAppPackages,
                        position = getFolderPosition(folderData.id)
                    )
                }
            }

            _uiState.update {
                it.copy(
                    folders = folders,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FolderOverviewViewModel", "Error loading folders", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load folders"
                )
            }
        }
    }

    fun reloadFolders() {
        viewModelScope.launch {
            loadFolders()
        }
    }

    private fun getFolderPosition(folderId: String): androidx.compose.ui.geometry.Offset {
        val margin = 32f * 3.5f
        val availableWidth = currentScreenSize.width - (margin * 2)
        val availableHeight = currentScreenSize.height - (margin * 2)
        
        return when (folderId) {
            "folder_1" -> androidx.compose.ui.geometry.Offset(
                margin + availableWidth * 0.75f, 
                margin + availableHeight * 0.2f
            )
            "folder_2" -> androidx.compose.ui.geometry.Offset(
                margin + availableWidth * 0.25f, 
                margin + availableHeight * 0.35f
            )
            "folder_3" -> androidx.compose.ui.geometry.Offset(
                margin + availableWidth * 0.6f, 
                margin + availableHeight * 0.7f
            )
            else -> androidx.compose.ui.geometry.Offset.Zero
        }
    }

    fun onFolderTapped(folderId: String): String? {
        return folderId
    }

    fun onFolderNameTapped(folderId: String) {
        _uiState.update { it.copy(editingFolderId = folderId) }
    }

    fun updateFolderName(folderId: String, newName: String) {
        viewModelScope.launch {
            try {
                launcherSettingsRepository.updateFolderName(folderId, newName)
                _uiState.update {
                    it.copy(
                        editingFolderId = null,
                        folders = it.folders.map { folder ->
                            if (folder.id == folderId) folder.copy(name = newName) else folder
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update folder name")
                }
            }
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingFolderId = null) }
    }

    fun updateScreenSize(screenSize: Size) {
        if (screenSize.width <= 0 || screenSize.height <= 0) return

        currentScreenSize = screenSize

        viewModelScope.launch {
            try {
                val currentFolders = _uiState.value.folders
                val folders = currentFolders.map { folder ->
                    folder.copy(position = getFolderPosition(folder.id))
                }
                _uiState.update { it.copy(folders = folders) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update screen size")
                }
            }
        }
    }
}