package de.rr.universelauncher.presentation.folderoverview

import de.rr.universelauncher.domain.model.Folder

data class FolderOverviewUiState(
    val folders: List<Folder>,
    val isLoading: Boolean,
    val error: String?,
    val editingFolderId: String?
)
