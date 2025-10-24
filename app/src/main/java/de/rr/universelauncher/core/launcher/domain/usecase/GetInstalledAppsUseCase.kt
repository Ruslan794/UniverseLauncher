package de.rr.universelauncher.core.launcher.domain.usecase

import de.rr.universelauncher.core.launcher.domain.model.AppInfo
import de.rr.universelauncher.core.launcher.domain.repository.AppRepository
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): List<AppInfo> {
        return appRepository.getInstalledApps()
    }
}
