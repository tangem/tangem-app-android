package com.tangem.tap

import com.tangem.TangemSdkLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.tap.common.log.TangemAppLoggerInitializer
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.home.featuretoggles.HomeFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository as WalletConnect2Repository

@EntryPoint
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
interface ApplicationEntryPoint {

    fun getEnvironmentConfigStorage(): EnvironmentConfigStorage

    fun getAppStateHolder(): AppStateHolder

    fun getAssetLoader(): AssetLoader

    fun getFeatureTogglesManager(): FeatureTogglesManager

    fun getNetworkConnectionManager(): NetworkConnectionManager

    fun getCardScanningFeatureToggles(): CardScanningFeatureToggles

    fun getWalletConnect2Repository(): WalletConnect2Repository

    fun getWalletConnectSessionsRepository(): WalletConnectSessionsRepository

    fun getScanCardProcessor(): ScanCardProcessor

    fun getAppCurrencyRepository(): AppCurrencyRepository

    fun getWalletManagersFacade(): WalletManagersFacade

    fun getNetworksRepository(): NetworksRepository

    fun getCurrenciesRepository(): CurrenciesRepository

    fun getAppThemeModeRepository(): AppThemeModeRepository

    fun getBalanceHidingRepository(): BalanceHidingRepository

    fun getAppPreferencesStore(): AppPreferencesStore

    fun getGetAppThemeModeUseCase(): GetAppThemeModeUseCase

    fun getWalletsRepository(): WalletsRepository

    fun getOneTimeEventFilter(): OneTimeEventFilter

    fun getGeneralUserWalletsListManager(): UserWalletsListManager

    fun getWasTwinsOnboardingShownUseCase(): WasTwinsOnboardingShownUseCase

    fun getSaveTwinsOnboardingShownUseCase(): SaveTwinsOnboardingShownUseCase

    fun getWalletNameGenerateUseCase(): GenerateWalletNameUseCase

    fun getCardRepository(): CardRepository

    fun getTangemSdkLogger(): TangemSdkLogger

    fun getSettingsRepository(): SettingsRepository

    fun getBlockchainSDKFactory(): BlockchainSDKFactory

    fun getSendFeedbackEmailUseCase(): SendFeedbackEmailUseCase

    fun getSaveBlockchainErrorUseCase(): SaveBlockchainErrorUseCase

    fun getGetCardInfoUseCase(): GetCardInfoUseCase

    fun getUrlOpener(): UrlOpener

    fun getShareManager(): ShareManager

    fun getAppRouter(): AppRouter

    fun getTangemAppLogger(): TangemAppLoggerInitializer

    fun getHomeFeatureToggles(): HomeFeatureToggles

    fun getGetUserCountryCodeUseCase(): GetUserCountryUseCase
}