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
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val savedFolders = launcherSettingsRepository.getFolders().first()
                
                val folders = if (savedFolders.isEmpty()) {
                    val allApps = appRepository.getInstalledAppsWithLaunchCounts()
                    val selectedApps = launcherSettingsRepository.getSelectedApps().first()
                    
                    val finalSelectedApps = selectedApps.ifEmpty {
                        val topApps = allApps.sortedByDescending { it.launchCount }.take(10)
                        val topAppPackages = topApps.map { it.packageName }.toSet()
                        launcherSettingsRepository.setSelectedApps(topAppPackages)
                        topAppPackages
                    }
                    
                    val remainingApps = allApps.filter { it.packageName !in finalSelectedApps }
                    val appsPerFolder = (remainingApps.size / 2).coerceAtLeast(1)
                    
                    val defaultFolders = createDefaultFolders(currentScreenSize.width, currentScreenSize.height)
                    val foldersWithApps = defaultFolders.mapIndexed { index, folder ->
                        val folderApps = when (index) {
                            0 -> finalSelectedApps.take(6).toSet()
                            else -> {
                                val startIndex = (index - 1) * 6
                                remainingApps.drop(startIndex).take(6).map { it.packageName }.toSet()
                            }
                        }
                        folder.copy(appPackageNames = folderApps)
                    }
                    
                    val folderData = foldersWithApps.map { folder ->
                        de.rr.universelauncher.domain.model.FolderData(folder.id, folder.name, folder.appPackageNames)
                    }
                    launcherSettingsRepository.saveFolders(folderData)
                    
                    foldersWithApps
                } else {
                    savedFolders.map { folderData ->
                        Folder(
                            id = folderData.id,
                            name = folderData.name,
                            appPackageNames = folderData.appPackageNames,
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load folders"
                    )
                }
            }
        }
    }
    
    private fun getFolderPosition(folderId: String): androidx.compose.ui.geometry.Offset {
        return when (folderId) {
            "folder_1" -> androidx.compose.ui.geometry.Offset(currentScreenSize.width * 0.75f, currentScreenSize.height * 0.2f)
            "folder_2" -> androidx.compose.ui.geometry.Offset(currentScreenSize.width * 0.25f, currentScreenSize.height * 0.35f)
            "folder_3" -> androidx.compose.ui.geometry.Offset(currentScreenSize.width * 0.6f, currentScreenSize.height * 0.7f)
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
                val savedFolders = launcherSettingsRepository.getFolders().first()
                val folders = savedFolders.map { folderData ->
                    Folder(
                        id = folderData.id,
                        name = folderData.name,
                        appPackageNames = folderData.appPackageNames,
                        position = getFolderPosition(folderData.id)
                    )
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
