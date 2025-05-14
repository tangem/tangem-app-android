package com.tangem.tap.di.domain

import com.tangem.domain.balancehiding.DeviceFlipDetector
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.balancehiding.UpdateBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.settings.*
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.FetchUserCountryUseCase
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.settings.DefaultLegacySettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDomainModule {

    @Provides
    @Singleton
    fun providesIsReadyToShowRatingUseCase(appRatingRepository: AppRatingRepository): IsReadyToShowRateAppUseCase {
        return IsReadyToShowRateAppUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @Singleton
    fun providesRemindToRateAppLaterUseCase(appRatingRepository: AppRatingRepository): RemindToRateAppLaterUseCase {
        return RemindToRateAppLaterUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @Singleton
    fun providesNeverToSuggestRateAppUseCase(appRatingRepository: AppRatingRepository): NeverToSuggestRateAppUseCase {
        return NeverToSuggestRateAppUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @Singleton
    fun providesSetWalletWithFundsFoundUseCase(
        appRatingRepository: AppRatingRepository,
    ): SetWalletWithFundsFoundUseCase {
        return SetWalletWithFundsFoundUseCase(appRatingRepository = appRatingRepository)
    }

    @Provides
    @Singleton
    fun providesShouldShowSaveWalletScreenUseCase(
        settingsRepository: SettingsRepository,
    ): ShouldShowSaveWalletScreenUseCase {
        return ShouldShowSaveWalletScreenUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowMarketsTooltipUseCase(
        settingsRepository: SettingsRepository,
    ): ShouldShowMarketsTooltipUseCase {
        return ShouldShowMarketsTooltipUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun providesCanUseBiometryUseCase(tangemSdkManager: TangemSdkManager): CanUseBiometryUseCase {
        return CanUseBiometryUseCase(
            legacySettingsRepository = DefaultLegacySettingsRepository(tangemSdkManager = tangemSdkManager),
        )
    }

    @Provides
    @Singleton
    fun providesGetBalanceHidingSettingsUseCase(
        balanceHidingRepository: BalanceHidingRepository,
    ): GetBalanceHidingSettingsUseCase {
        return GetBalanceHidingSettingsUseCase(
            balanceHidingRepository = balanceHidingRepository,
        )
    }

    @Provides
    @Singleton
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
    @Singleton
    fun provideUpdateHideBalancesSettingsUseCase(
        balanceHidingRepository: BalanceHidingRepository,
    ): UpdateBalanceHidingSettingsUseCase {
        return UpdateBalanceHidingSettingsUseCase(balanceHidingRepository)
    }

    @Provides
    @Singleton
    fun provideSetWalletsScrollPreviewIsShown(settingsRepository: SettingsRepository): NeverToShowWalletsScrollPreview {
        return NeverToShowWalletsScrollPreview(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideIsWalletsScrollPreviewEnabled(settingsRepository: SettingsRepository): IsWalletsScrollPreviewEnabled {
        return IsWalletsScrollPreviewEnabled(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteDeprecatedLogsUseCase(settingsRepository: SettingsRepository): DeleteDeprecatedLogsUseCase {
        return DeleteDeprecatedLogsUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideIsSendTapHelpPreviewEnabledUseCase(
        settingsRepository: SettingsRepository,
    ): IsSendTapHelpEnabledUseCase {
        return IsSendTapHelpEnabledUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideNeverShowTapHelpUseCase(settingsRepository: SettingsRepository): NeverShowTapHelpUseCase {
        return NeverShowTapHelpUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSetSaveWalletScreenShownUseCase(
        settingsRepository: SettingsRepository,
    ): SetSaveWalletScreenShownUseCase {
        return SetSaveWalletScreenShownUseCase(settingsRepository = settingsRepository)
    }

    @Provides
    @Singleton
    fun provideIncrementAppLaunchCounterUseCase(
        settingsRepository: SettingsRepository,
    ): IncrementAppLaunchCounterUseCase {
        return IncrementAppLaunchCounterUseCase(settingsRepository = settingsRepository)
    }

    // region PushPermissionRepository
    @Provides
    @Singleton
    fun provideShouldInitiallyAskPermissionUseCase(
        permissionRepository: PermissionRepository,
    ): ShouldInitiallyAskPermissionUseCase {
        return ShouldInitiallyAskPermissionUseCase(repository = permissionRepository)
    }

    @Provides
    @Singleton
    fun provideNeverToInitiallyAskPermissionUseCase(
        permissionRepository: PermissionRepository,
    ): NeverToInitiallyAskPermissionUseCase {
        return NeverToInitiallyAskPermissionUseCase(repository = permissionRepository)
    }

    @Provides
    @Singleton
    fun provideShouldAskPermissionUseCase(permissionRepository: PermissionRepository): ShouldAskPermissionUseCase {
        return ShouldAskPermissionUseCase(repository = permissionRepository)
    }

    @Provides
    @Singleton
    fun provideNeverRequestPermissionUseCase(
        permissionRepository: PermissionRepository,
    ): NeverRequestPermissionUseCase {
        return NeverRequestPermissionUseCase(repository = permissionRepository)
    }
    // endregion

    @Provides
    @Singleton
    fun provideFetchUserCountryCodeUseCase(settingsRepository: SettingsRepository): FetchUserCountryUseCase {
        return FetchUserCountryUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideGetUserCountryCodeUseCase(settingsRepository: SettingsRepository): GetUserCountryUseCase {
        return GetUserCountryUseCase(settingsRepository)
    }

    // region Google services availability
    @Provides
    @Singleton
    fun provideSetGoogleServicesAvailabilityUseCase(
        settingsRepository: SettingsRepository,
    ): SetGoogleServicesAvailabilityUseCase {
        return SetGoogleServicesAvailabilityUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideIsGoogleServicesAvailableUseCase(
        settingsRepository: SettingsRepository,
    ): IsGoogleServicesAvailableUseCase {
        return IsGoogleServicesAvailableUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSetGooglePayAvailabilityUseCase(
        settingsRepository: SettingsRepository,
    ): SetGooglePayAvailabilityUseCase {
        return SetGooglePayAvailabilityUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideIsGooglePayAvailableUseCase(settingsRepository: SettingsRepository): IsGooglePayAvailableUseCase {
        return IsGooglePayAvailableUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideMaybeSetWalletFirstTimeUsageUseCase(
        settingsRepository: SettingsRepository,
    ): SetWalletFirstTimeUsageUseCase {
        return SetWalletFirstTimeUsageUseCase(settingsRepository)
    }
    // endregion
}