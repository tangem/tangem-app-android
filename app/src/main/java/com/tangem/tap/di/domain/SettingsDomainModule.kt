package com.tangem.tap.di.domain

import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.IsUserAlreadyRateAppUseCase
import com.tangem.domain.settings.ShouldShowSaveWalletScreenUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.settings.DefaultLegacySettingsRepository
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

    @Provides
    @ViewModelScoped
    fun providesShouldShowSaveWalletScreenUseCase(
        settingsRepository: SettingsRepository,
    ): ShouldShowSaveWalletScreenUseCase {
        return ShouldShowSaveWalletScreenUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesCanUseBiometryUseCase(tangemSdkManager: TangemSdkManager): CanUseBiometryUseCase {
        return CanUseBiometryUseCase(
            legacySettingsRepository = DefaultLegacySettingsRepository(tangemSdkManager = tangemSdkManager),
        )
    }
}