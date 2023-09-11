package com.tangem.data.apptheme.di

import com.tangem.data.apptheme.MockAppThemeModeRepository
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
    fun provideAppThemeModeRepository(): AppThemeModeRepository {
        return MockAppThemeModeRepository()
    }
}