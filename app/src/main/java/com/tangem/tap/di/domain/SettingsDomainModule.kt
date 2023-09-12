package com.tangem.tap.di.domain

import android.content.Context
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.settings.*
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.tap.DeviceFlipDetectorImpl
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.settings.DefaultLegacySettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @ViewModelScoped
    fun providesIsBalanceHiddenUseCase(settingsRepository: SettingsRepository): IsBalanceHiddenUseCase {
        return IsBalanceHiddenUseCase(
            settingsRepository = settingsRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun providesListenUseCase(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
    ): ListenToFlipsUseCase {
        return ListenToFlipsUseCase(
            flipDetector = DeviceFlipDetectorImpl(context),
            settingsRepository = settingsRepository,
        )
    }
}
