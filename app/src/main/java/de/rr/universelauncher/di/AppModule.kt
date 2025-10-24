package de.rr.universelauncher.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.rr.universelauncher.data.AppRepositoryImpl
import de.rr.universelauncher.data.LauncherSettingsRepositoryImpl
import de.rr.universelauncher.domain.repository.AppRepository
import de.rr.universelauncher.domain.repository.LauncherSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindLauncherSettingsRepository(
        launcherSettingsRepositoryImpl: LauncherSettingsRepositoryImpl
    ): LauncherSettingsRepository
}
