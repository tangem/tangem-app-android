package com.tangem.tap.di.domain

import com.tangem.domain.settings.IsUserAlreadyRateAppUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object SettingsDomainModule {

    @Provides
    @ViewModelScoped
    fun providesGetWalletsUseCase(settingsRepository: SettingsRepository): IsUserAlreadyRateAppUseCase {
        return IsUserAlreadyRateAppUseCase(settingsRepository = settingsRepository)
    }
}