package de.rr.universelauncher.core.launcher.domain.usecase

import de.rr.universelauncher.core.launcher.domain.repository.AppRepository
import javax.inject.Inject

class LaunchAppUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke(packageName: String) {
        appRepository.launchApp(packageName)
    }
}
