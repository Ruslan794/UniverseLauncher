package de.rr.universelauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.rr.universelauncher.core.launcher.domain.repository.AppRepository
import de.rr.universelauncher.core.launcher.domain.usecase.GetInstalledAppsUseCase
import de.rr.universelauncher.core.launcher.domain.usecase.LaunchAppUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetInstalledAppsUseCase(
        appRepository: AppRepository
    ): GetInstalledAppsUseCase {
        return GetInstalledAppsUseCase(appRepository)
    }

    @Provides
    @Singleton
    fun provideLaunchAppUseCase(
        appRepository: AppRepository
    ): LaunchAppUseCase {
        return LaunchAppUseCase(appRepository)
    }
}
