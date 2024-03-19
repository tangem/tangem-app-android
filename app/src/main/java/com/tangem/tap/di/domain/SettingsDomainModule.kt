package com.tangem.tap.di.domain

import com.tangem.domain.balancehiding.DeviceFlipDetector
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.balancehiding.UpdateBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.settings.*
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.repositories.SwapPromoRepository
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
    fun providesIsReadyToShowRatingUseCase(appRatingRepository: AppRatingRepository): IsReadyToShowRateAppUseCase {
        return IsReadyToShowRateAppUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesRemindToRateAppLaterUseCase(appRatingRepository: AppRatingRepository): RemindToRateAppLaterUseCase {
        return RemindToRateAppLaterUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesNeverToSuggestRateAppUseCase(appRatingRepository: AppRatingRepository): NeverToSuggestRateAppUseCase {
        return NeverToSuggestRateAppUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesSetWalletWithFundsFoundUseCase(
        appRatingRepository: AppRatingRepository,
    ): SetWalletWithFundsFoundUseCase {
        return SetWalletWithFundsFoundUseCase(appRatingRepository = appRatingRepository)
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
    fun providesGetBalanceHidingSettingsUseCase(
        balanceHidingRepository: BalanceHidingRepository,
    ): GetBalanceHidingSettingsUseCase {
        return GetBalanceHidingSettingsUseCase(
            balanceHidingRepository = balanceHidingRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun providesListenUseCase(
        flipDetector: DeviceFlipDetector,
        balanceHidingRepository: BalanceHidingRepository,
    ): ListenToFlipsUseCase {
        return ListenToFlipsUseCase(
            flipDetector = flipDetector,
            balanceHidingRepository = balanceHidingRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateHideBalancesSettingsUseCase(
        balanceHidingRepository: BalanceHidingRepository,
    ): UpdateBalanceHidingSettingsUseCase {
        return UpdateBalanceHidingSettingsUseCase(balanceHidingRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSetWalletsScrollPreviewIsShown(settingsRepository: SettingsRepository): NeverToShowWalletsScrollPreview {
        return NeverToShowWalletsScrollPreview(settingsRepository = settingsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsWalletsScrollPreviewEnabled(settingsRepository: SettingsRepository): IsWalletsScrollPreviewEnabled {
        return IsWalletsScrollPreviewEnabled(settingsRepository = settingsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideShouldShowSwapPromoWalletUseCase(
        swapPromoRepository: SwapPromoRepository,
    ): ShouldShowSwapPromoWalletUseCase {
        return ShouldShowSwapPromoWalletUseCase(swapPromoRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideShouldShowSwapPromoTokenUseCase(
        swapPromoRepository: SwapPromoRepository,
    ): ShouldShowSwapPromoTokenUseCase {
        return ShouldShowSwapPromoTokenUseCase(swapPromoRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideDeleteDeprecatedLogsUseCase(settingsRepository: SettingsRepository): DeleteDeprecatedLogsUseCase {
        return DeleteDeprecatedLogsUseCase(settingsRepository)
    }
}