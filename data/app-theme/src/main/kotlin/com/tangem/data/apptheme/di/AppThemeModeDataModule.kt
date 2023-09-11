package com.tangem.data.apptheme.di

import com.tangem.data.apptheme.DefaultAppThemeModeRepository
import com.tangem.datasource.local.apptheme.AppThemeModeStore
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun provideAppThemeModeRepository(
        appThemeModeStore: AppThemeModeStore,
        dispatchers: CoroutineDispatcherProvider,
    ): AppThemeModeRepository {
        return DefaultAppThemeModeRepository(appThemeModeStore, dispatchers)
    }
}