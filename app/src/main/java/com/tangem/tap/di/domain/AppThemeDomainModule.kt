package com.tangem.tap.di.domain

import com.tangem.domain.apptheme.ChangeAppThemeModeUseCase
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AppThemeDomainModule {

    @Provides
    fun provideGetAppThemeModeUpdatesUseCase(appThemeModeRepository: AppThemeModeRepository): GetAppThemeModeUseCase {
        return GetAppThemeModeUseCase(appThemeModeRepository)
    }

    @Provides
    fun provideChangeAppThemeModeUseCase(appThemeModeRepository: AppThemeModeRepository): ChangeAppThemeModeUseCase {
        return ChangeAppThemeModeUseCase(appThemeModeRepository)
    }
}