package com.tangem.data.apptheme.di

import com.tangem.data.apptheme.DefaultAppThemeModeRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppThemeModeDataModule {

    @Provides
    @Singleton
    fun provideAppThemeModeRepository(appPreferencesStore: AppPreferencesStore): AppThemeModeRepository {
        return DefaultAppThemeModeRepository(appPreferencesStore = appPreferencesStore)
    }
}